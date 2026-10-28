package com.yogistudio.molkkyboard.core.nearby

import com.yogistudio.molkkyboard.core.sync.ConnectionId
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigInteger
import java.net.InetSocketAddress
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

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

    private fun startServer(): Int {
        server =
            NearbyServer(InetSocketAddress("127.0.0.1", 0), certificate, { id, text -> received += id to text }, {})
        return server.startAndWait()
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
}
