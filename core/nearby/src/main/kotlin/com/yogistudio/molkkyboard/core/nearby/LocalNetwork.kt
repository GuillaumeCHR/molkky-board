package com.yogistudio.molkkyboard.core.nearby

import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlin.coroutines.resume

/** Comment les téléphones se retrouvent : le point d'accès de l'hôte (dehors), ou le Wi-Fi commun. */
enum class NetworkMode { HOTSPOT, SAME_WIFI }

/** Réseau d'un point d'accès local démarré par l'hôte (sans Internet). */
class Hotspot(
    val ssid: String,
    val passphrase: String,
    private val reservation: WifiManager.LocalOnlyHotspotReservation,
) {
    fun close() = reservation.close()
}

/**
 * Point d'accès local, sans Internet. Android 13+ : permission « appareils à proximité » (jamais la localisation) ;
 * en dessous, Android exige la localisation précise, que l'app ne demande jamais : `null` (mode « même Wi-Fi »).
 */
@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
suspend fun startHotspot(wifi: WifiManager): Hotspot? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    return suspendCancellableCoroutine { continuation ->
        try {
            wifi.startLocalOnlyHotspot(
                object : WifiManager.LocalOnlyHotspotCallback() {
                    override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                        val config = reservation.softApConfiguration
                        val spot = Hotspot(config.ssid.orEmpty().trim('"'), config.passphrase.orEmpty(), reservation)
                        if (continuation.isActive) continuation.resume(spot) else spot.close()
                    }

                    override fun onFailed(reason: Int) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                },
                Handler(Looper.getMainLooper()),
            )
        } catch (_: SecurityException) {
            continuation.resume(null)
        } catch (_: IllegalStateException) {
            continuation.resume(null)
        }
    }
}

/** Adresses IPv4 privées de ce téléphone, par interface (le point d'accès est en général « ap0 » ou « swlan0 »). */
fun localAddresses(): List<Pair<String, String>> =
    NetworkInterface
        .getNetworkInterfaces()
        ?.toList()
        .orEmpty()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { nif ->
            nif.inetAddresses
                .toList()
                .filterIsInstance<Inet4Address>()
                .filter { it.isSiteLocalAddress }
                .map { nif.name to it.hostAddress.orEmpty() }
        }

/** L'adresse où servir la partie : le point d'accès s'il y en a un, sinon le Wi-Fi. */
fun partyAddress(hotspot: Boolean): String? {
    val addresses = localAddresses()
    val wifi = addresses.firstOrNull { it.first.startsWith("wlan") }
    val other = addresses.firstOrNull { it != wifi }
    return (if (hotspot) other ?: wifi else wifi ?: other)?.second
}

/** Requête du réseau de la partie, à libérer quand l'invité quitte la partie. */
class PartyNetwork(
    private val connectivity: ConnectivityManager,
    private val callback: ConnectivityManager.NetworkCallback?,
) {
    fun release() {
        callback?.let { runCatching { connectivity.unregisterNetworkCallback(it) } }
    }
}

/**
 * Rejoint le réseau de la partie. Point d'accès : `WifiNetworkSpecifier` (Android 10+, fenêtre système de
 * confirmation, réseau sans Internet, les données mobiles restent actives) ; Wi-Fi commun : le réseau courant.
 * [onNetwork] reçoit `null` pour le réseau courant ; [onUnavailable] si la connexion est refusée ou impossible.
 */
fun requestPartyNetwork(
    connectivity: ConnectivityManager,
    ssid: String?,
    passphrase: String?,
    onNetwork: (Network?) -> Unit,
    onUnavailable: () -> Unit,
): PartyNetwork {
    if (ssid == null) {
        onNetwork(null)
        return PartyNetwork(connectivity, null)
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        onUnavailable()
        return PartyNetwork(connectivity, null)
    }
    val request =
        NetworkRequest
            .Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(
                WifiNetworkSpecifier
                    .Builder()
                    .setSsid(ssid)
                    .setWpa2Passphrase(passphrase.orEmpty())
                    .build(),
            ).build()
    val callback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = onNetwork(network)

            override fun onUnavailable() = onUnavailable()
        }
    connectivity.requestNetwork(request, callback)
    return PartyNetwork(connectivity, callback)
}
