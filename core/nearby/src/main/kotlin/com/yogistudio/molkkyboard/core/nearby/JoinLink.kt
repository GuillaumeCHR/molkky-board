package com.yogistudio.molkkyboard.core.nearby

import android.net.Uri

/**
 * Lien d'invitation du QR code (`molkkyboard://join#…`, paramètres après le `#`) : adresse et port de l'hôte,
 * empreinte SHA-256 du certificat de partie, jeton à usage unique, et le réseau en mode point d'accès.
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
                "v=1",
                "h=$host",
                "p=$port",
                "fp=$fingerprint",
                "t=$token",
                ssid?.let { "ssid=${Uri.encode(it)}" },
                passphrase?.let { "psk=${Uri.encode(it)}" },
            )
        return "molkkyboard://join#" + params.joinToString("&")
    }

    companion object {
        /** Validation stricte : version, adresse IPv4 privée, port, longueurs exactes de l'empreinte et du jeton. */
        fun parse(text: String): JoinLink? {
            val fragment = text.trim().substringAfter("molkkyboard://join#", "").ifEmpty { return null }
            val params =
                fragment
                    .split("&")
                    .mapNotNull { part ->
                        part.split("=", limit = 2).takeIf { it.size == 2 }?.let { it[0] to Uri.decode(it[1]) }
                    }.toMap()
            val host = params["h"]?.takeIf { it.isPrivateIpv4() } ?: return null
            val port = params["p"]?.toIntOrNull()?.takeIf { it in 1..65_535 } ?: return null
            val fingerprint = params["fp"]?.takeIf { HEX64.matches(it) } ?: return null
            val token = params["t"]?.takeIf { HEX32.matches(it) } ?: return null
            if (params["v"] != "1") return null
            return JoinLink(host, port, fingerprint, token, params["ssid"], params["psk"])
        }

        private val HEX64 = Regex("[0-9a-f]{64}")
        private val HEX32 = Regex("[0-9a-f]{32}")

        private fun String.isPrivateIpv4(): Boolean {
            val parts = split(".").mapNotNull { it.toIntOrNull()?.takeIf { n -> n in 0..255 } }
            if (parts.size != 4) return false
            return parts[0] == 10 ||
                (parts[0] == 172 && parts[1] in 16..31) ||
                (parts[0] == 192 && parts[1] == 168)
        }
    }
}
