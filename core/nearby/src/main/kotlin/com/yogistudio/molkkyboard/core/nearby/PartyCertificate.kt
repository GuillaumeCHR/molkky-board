package com.yogistudio.molkkyboard.core.nearby

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.security.auth.x500.X500Principal

/**
 * Certificat auto-signé d'une partie, prêt à servir TLS, et son empreinte SHA-256 (dans le QR code, épinglée par les
 * invités). Clé EC P-256 dans l'Android Keystore, non exportable, supprimée à la fin du partage (validé au prototype
 * P0 : TLS 1.3 avec cette clé).
 */
class PartyCertificate(
    val ssl: SSLContext,
    val certificate: X509Certificate,
) {
    val fingerprint: String get() = sha256(certificate.encoded)

    companion object {
        private const val ALIAS = "molkky-partie"
        private const val DAY_MS = 86_400_000L
        private const val SERIAL_BITS = 64

        fun keystore(): PartyCertificate {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            store.deleteEntry(ALIAS)
            val now = System.currentTimeMillis()
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
                initialize(
                    KeyGenParameterSpec
                        .Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_NONE)
                        .setCertificateSubject(X500Principal("CN=Molkky Board partie"))
                        .setCertificateSerialNumber(BigInteger(SERIAL_BITS, SecureRandom()))
                        .setCertificateNotBefore(Date(now - DAY_MS))
                        .setCertificateNotAfter(Date(now + DAY_MS))
                        .build(),
                )
                generateKeyPair()
            }
            return from(store, null, ALIAS)
        }

        /** Supprime la clé de la partie (fin du partage). */
        fun delete() {
            runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(ALIAS) }
        }

        /** Certificat tiré d'un magasin de clés quelconque (tests sur la JVM : PKCS12 en mémoire). */
        fun from(store: KeyStore, password: CharArray?, alias: String): PartyCertificate {
            val kmf =
                KeyManagerFactory
                    .getInstance(
                        KeyManagerFactory.getDefaultAlgorithm(),
                    ).apply { init(store, password) }
            val ssl = SSLContext.getInstance("TLS").apply { init(kmf.keyManagers, null, null) }
            return PartyCertificate(ssl, store.getCertificate(alias) as X509Certificate)
        }

        fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
