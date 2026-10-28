package com.yogistudio.molkkyboard.core.nearby

import android.net.Uri

/**
 * Lien d'invitation du QR code (`molkkyboard://join#…`, paramètres après le `#`) : adresse et port de l'hôte,
 * empreinte SHA-256 du certificat de partie, jeton à usage unique, et le réseau en mode point d'accès. Le QR code est
 * considéré comme observable : il ne contient aucun secret durable.
 */
data class JoinLink(
    val host: String,
    val port: Int,
    val fingerprint: String,
    val token: String,
    val ssid: String? = null,
    val passphrase: String? = null,
) {
    fun format(): String {
        val params =
            listOfNotNull(
                "v=$VERSION",
                "h=$host",
                "p=$port",
                "fp=$fingerprint",
                "t=$token",
                ssid?.let { "ssid=${Uri.encode(it)}" },
                passphrase?.let { "psk=${Uri.encode(it)}" },
            )
        return PREFIX + params.joinToString("&")
    }

    companion object {
        const val PREFIX = "molkkyboard://join#"
        private const val VERSION = "1"
        private const val MAX_PORT = 65_535
        private const val MAX_FIELD = 64
        private val HEX64 = Regex("[0-9a-f]{64}")
        private val HEX32 = Regex("[0-9a-f]{32}")

        /**
         * Validation stricte de tout ce qui arrive d'un QR code : version, adresse IPv4 **privée** (jamais une adresse
         * publique : rien ne part vers Internet), port, longueurs exactes de l'empreinte et du jeton, champs bornés.
         */
        fun parse(text: String): JoinLink? {
            val fragment = text.trim().takeIf { it.startsWith(PREFIX) }?.removePrefix(PREFIX) ?: return null
            val params =
                fragment
                    .split("&")
                    .mapNotNull { part ->
                        part.split("=", limit = 2).takeIf { it.size == 2 }?.let { it[0] to Uri.decode(it[1]) }
                    }.toMap()
            val host = params["h"]?.takeIf(::isPrivateIpv4)
            val port = params["p"]?.toIntOrNull()?.takeIf { it in 1..MAX_PORT }
            val fingerprint = params["fp"]?.takeIf { HEX64.matches(it) }
            val token = params["t"]?.takeIf { HEX32.matches(it) }
            val valid =
                params["v"] == VERSION && listOfNotNull(params["ssid"], params["psk"]).all { it.length <= MAX_FIELD }
            if (!valid || host == null || port == null) return null
            return if (fingerprint != null && token != null) {
                JoinLink(host, port, fingerprint, token, params["ssid"], params["psk"])
            } else {
                null
            }
        }

        /** 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16. */
        fun isPrivateIpv4(address: String): Boolean {
            val parts = address.split(".").mapNotNull { it.toIntOrNull()?.takeIf { n -> n in 0..BYTE_MAX } }
            if (parts.size != IPV4_PARTS) return false
            val (a, b) = parts
            return a == PRIVATE_A || (a == PRIVATE_B && b in PRIVATE_B_RANGE) ||
                (a == PRIVATE_C && b == PRIVATE_C_SECOND)
        }

        private const val BYTE_MAX = 255
        private const val IPV4_PARTS = 4
        private const val PRIVATE_A = 10
        private const val PRIVATE_B = 172
        private val PRIVATE_B_RANGE = 16..31
        private const val PRIVATE_C = 192
        private const val PRIVATE_C_SECOND = 168
    }
}
