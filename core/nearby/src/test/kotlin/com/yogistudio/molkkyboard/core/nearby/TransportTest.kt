package com.yogistudio.molkkyboard.core.nearby

import com.yogistudio.molkkyboard.core.sync.ConnectionId
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigInteger
import java.net.InetSocketAddress
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

/** Transport réel sur la boucle locale : TLS, épinglage de l'empreinte, aller-retour de messages. */
@RunWith(RobolectricTestRunner::class)
class TransportTest {
    private val certificate = softwareCertificate()
    private val received = LinkedBlockingQueue<Pair<ConnectionId, String>>()
    private lateinit var server: NearbyServer

    private fun softwareCertificate(): PartyCertificate {
        val pair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val name = X500Name("CN=test")
        val now = System.currentTimeMillis()
        val holder =
            JcaX509v3CertificateBuilder(name, BigInteger.ONE, Date(now - 60_000), Date(now + 60_000), name, pair.public)
                .build(JcaContentSignerBuilder("SHA256withECDSA").build(pair.private))
        val cert = JcaX509CertificateConverter().getCertificate(holder)
        val store =
            KeyStore.getInstance("PKCS12").apply {
                load(null)
                setKeyEntry("k", pair.private, CharArray(0), arrayOf(cert))
            }
        return PartyCertificate.from(store, CharArray(0), "k")
    }

    private fun startServer(known: Boolean = true, admissionMs: Long = 15_000L): Int {
        server =
            NearbyServer(
                InetSocketAddress("127.0.0.1", 0),
                certificate,
                onText = { id, text -> received += id to text },
                onClosed = {},
                isKnown = { _, answer -> answer(known) },
                admissionMs = admissionMs,
            )
        return server.startAndWait()
    }

    /** Poignée de main TLS brute avec un protocole et une suite imposés ; renvoie le protocole négocié. */
    private fun handshake(port: Int, protocol: String, suite: String): String {
        val trustAll =
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String?) = Unit

                override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String?) = Unit

                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
        val context = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trustAll), null) }
        (context.socketFactory.createSocket("127.0.0.1", port) as SSLSocket).use { socket ->
            socket.soTimeout = 10_000
            socket.enabledProtocols = arrayOf(protocol)
            socket.enabledCipherSuites = arrayOf(suite)
            socket.startHandshake()
            return socket.session.protocol
        }
    }

    @After
    fun tearDown() = server.stop(1_000)

    private class Probe : ClientListener {
        val opened = CountDownLatch(1)
        val closed = CountDownLatch(1)
        val texts = LinkedBlockingQueue<String>()
        var wrongHost = false

        override fun onOpen() = opened.countDown()

        override fun onText(text: String) {
            texts += text
        }

        override fun onClosed(wrongHost: Boolean) {
            this.wrongHost = wrongHost
            closed.countDown()
        }
    }

    @Test
    fun `aller-retour chiffre avec l'empreinte epinglee`() {
        val port = startServer()
        val probe = Probe()
        val client = NearbyClient("127.0.0.1", port, certificate.fingerprint, null, probe)
        client.connect()
        assertTrue(probe.opened.await(10, TimeUnit.SECONDS))
        client.send("bonjour")
        val (id, text) = received.poll(10, TimeUnit.SECONDS)!!
        assertEquals("bonjour", text)
        server.send(id, "salut")
        assertEquals("salut", probe.texts.poll(10, TimeUnit.SECONDS))
        client.close()
    }

    @Test
    fun `une autre empreinte - refus a la poignee de main, sans nouvelle tentative`() {
        val port = startServer()
        val probe = Probe()
        NearbyClient("127.0.0.1", port, "0".repeat(64), null, probe).connect()
        assertTrue(probe.closed.await(10, TimeUnit.SECONDS))
        assertTrue(probe.wrongHost)
        assertTrue(received.isEmpty())
    }

    @Test
    fun `connexion qui ne presente aucun jeton - fermee apres le delai d'admission`() {
        val port = startServer(known = false, admissionMs = 300)
        val probe = Probe()
        NearbyClient("127.0.0.1", port, certificate.fingerprint, null, probe).connect()
        assertTrue(probe.opened.await(10, TimeUnit.SECONDS))
        assertTrue("connexion inconnue fermée", probe.closed.await(10, TimeUnit.SECONDS))
        assertTrue(!probe.wrongHost)
    }

    @Test
    fun `connexion reconnue - gardee apres le delai d'admission`() {
        val port = startServer(known = true, admissionMs = 300)
        val probe = Probe()
        val client = NearbyClient("127.0.0.1", port, certificate.fingerprint, null, probe)
        client.connect()
        assertTrue(probe.opened.await(10, TimeUnit.SECONDS))
        assertTrue("toujours ouverte", !probe.closed.await(1, TimeUnit.SECONDS))
        client.close()
    }

    @Test
    fun `trame plus grosse que 64 Ko - connexion fermee sans que le message soit remis`() {
        val port = startServer()
        val probe = Probe()
        val client = NearbyClient("127.0.0.1", port, certificate.fingerprint, null, probe)
        client.connect()
        assertTrue(probe.opened.await(10, TimeUnit.SECONDS))
        client.send("x".repeat(70_000))
        assertTrue(probe.closed.await(10, TimeUnit.SECONDS))
        assertTrue(received.isEmpty())
    }

    @Test
    fun `TLS 1_3 negocie par defaut`() {
        val port = startServer()
        assertEquals("TLSv1.3", handshake(port, "TLSv1.3", "TLS_AES_128_GCM_SHA256"))
    }

    @Test
    fun `TLS 1_2 accepte seulement avec une suite AEAD et ECDHE`() {
        val port = startServer()
        assertEquals("TLSv1.2", handshake(port, "TLSv1.2", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256"))
        val weak =
            SSLContext
                .getDefault()
                .supportedSSLParameters.cipherSuites
                .filter { "ECDHE_ECDSA" in it && "CBC" in it }
        assertTrue("le JDK propose au moins une suite CBC", weak.isNotEmpty())
        weak.forEach { suite ->
            assertThrows(suite, SSLHandshakeException::class.java) { handshake(port, "TLSv1.2", suite) }
        }
    }
}
