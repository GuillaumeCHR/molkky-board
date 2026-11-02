package com.yogistudio.molkkyboard.core.nearby

import android.net.Network
import com.yogistudio.molkkyboard.core.sync.ConnectionId
import com.yogistudio.molkkyboard.core.sync.MAX_GUESTS
import com.yogistudio.molkkyboard.core.sync.MAX_MESSAGE_BYTES
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.SSLParametersWebSocketServerFactory
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import javax.net.ssl.X509TrustManager
import org.java_websocket.WebSocket as ServerSocket

/**
 * Serveur WebSocket sur TLS de l'hôte, sur l'adresse de la partie et un port aléatoire. Rien en clair ; TLS 1.3, ou
 * 1.2 pour les invités sous Android 8 et 9, avec des suites AEAD et ECDHE seulement ; trames et messages texte de
 * [MAX_MESSAGE_BYTES] au plus (une trame plus grosse est refusée avant d'être lue) ; débit borné par connexion ;
 * nombre de connexions borné. Une connexion que la session de l'hôte n'a pas reconnue ([isKnown] : demande d'arrivée
 * avec un jeton valable, ou téléphone déjà accepté) au bout de [admissionMs] est fermée : un intrus ne peut pas
 * occuper les places en restant connecté sans rien dire. Les messages reçus sont remis à [onText] (le gestionnaire
 * les sérialise vers la session de l'hôte).
 */
class NearbyServer(
    address: InetSocketAddress,
    certificate: PartyCertificate,
    private val onText: (ConnectionId, String) -> Unit,
    private val onClosed: (ConnectionId) -> Unit,
    private val isKnown: (ConnectionId, (Boolean) -> Unit) -> Unit = { _, answer -> answer(true) },
    private val admissionMs: Long = ADMISSION_MS,
) : WebSocketServer(address, listOf(Draft_6455(emptyList(), MAX_MESSAGE_BYTES))) {
    private val ids = AtomicLong()
    private val connections = ConcurrentHashMap<ConnectionId, ServerSocket>()
    private val budget = ConcurrentHashMap<ConnectionId, Pair<Long, Int>>()
    private val started = CountDownLatch(1)
    private val deadlines: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    init {
        setWebSocketFactory(SSLParametersWebSocketServerFactory(certificate.ssl, strictTls(certificate.ssl)))
        isReuseAddr = true
        connectionLostTimeout = LOST_TIMEOUT_S
    }

    /** Démarre le serveur et renvoie le port attribué (0 si le démarrage échoue). */
    fun startAndWait(): Int {
        start()
        started.await(START_TIMEOUT_S, TimeUnit.SECONDS)
        return port
    }

    override fun onStart() = started.countDown()

    override fun onOpen(conn: ServerSocket, handshake: ClientHandshake) {
        if (connections.size >= MAX_CONNECTIONS) {
            conn.close(CLOSE_FULL, "complet")
            return
        }
        val id = ConnectionId(ids.incrementAndGet())
        conn.setAttachment(id)
        connections[id] = conn
        deadlines.schedule({ isKnown(id) { known -> if (!known) close(id) } }, admissionMs, TimeUnit.MILLISECONDS)
    }

    override fun stop(timeout: Int) {
        deadlines.shutdownNow()
        super.stop(timeout)
    }

    override fun onMessage(conn: ServerSocket, message: String) {
        val id = conn.getAttachment<ConnectionId>() ?: return
        if (message.length > MAX_MESSAGE_BYTES || !withinBudget(id)) {
            conn.close(CLOSE_POLICY, "refusé")
            return
        }
        onText(id, message)
    }

    /** Seuls les messages texte du protocole sont acceptés. */
    override fun onMessage(conn: ServerSocket, message: ByteBuffer) = conn.close(CLOSE_POLICY, "refusé")

    override fun onClose(conn: ServerSocket, code: Int, reason: String?, remote: Boolean) {
        val id = conn.getAttachment<ConnectionId>() ?: return
        connections.remove(id)
        budget.remove(id)
        onClosed(id)
    }

    override fun onError(conn: ServerSocket?, ex: Exception) = Unit

    fun send(connection: ConnectionId, text: String) {
        connections[connection]?.takeIf { it.isOpen }?.send(text)
    }

    fun close(connection: ConnectionId) {
        connections[connection]?.close(CLOSE_NORMAL, "")
    }

    /** Au plus [MAX_PER_SECOND] messages par seconde et par connexion (inondation de messages). */
    private fun withinBudget(id: ConnectionId): Boolean {
        val second = System.currentTimeMillis() / MS_PER_S
        val (window, count) = budget[id] ?: (second to 0)
        val next = if (window == second) count + 1 else 1
        budget[id] = second to next
        return next <= MAX_PER_SECOND
    }

    private companion object {
        /** Délai pour présenter un jeton d'invitation ou de session (un vrai invité l'envoie dès la connexion). */
        const val ADMISSION_MS = 15_000L
        const val MAX_CONNECTIONS = MAX_GUESTS * 2
        const val MAX_PER_SECOND = 20
        const val LOST_TIMEOUT_S = 30
        const val START_TIMEOUT_S = 5L
        const val MS_PER_S = 1_000L
        const val CLOSE_NORMAL = 1000
        const val CLOSE_POLICY = 1008
        const val CLOSE_FULL = 1013
    }
}

/** N'accepte que le certificat dont l'empreinte SHA-256 est celle du QR code : aucune autre vérification ni repli. */
class PinnedTrustManager(
    private val fingerprint: String,
) : X509TrustManager {
    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String?) {
        val actual = chain.firstOrNull()?.encoded?.let(PartyCertificate::sha256)
        if (actual != fingerprint) throw CertificateException("Ce n'est pas le téléphone de l'organisateur")
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String?): Unit =
        throw CertificateException("pas de certificat client")

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}

/** Ce que l'invité reçoit de sa connexion. */
interface ClientListener {
    fun onOpen()

    fun onText(text: String)

    /** Connexion perdue ou refusée ; [wrongHost] : l'empreinte ne correspond pas (aucune nouvelle tentative). */
    fun onClosed(wrongHost: Boolean)
}

/**
 * Client WebSocket sur TLS de l'invité, épinglé sur l'empreinte du QR code et attaché au réseau de la partie s'il y en
 * a un (les données mobiles ne servent jamais).
 */
class NearbyClient(
    private val host: String,
    private val port: Int,
    fingerprint: String,
    network: Network?,
    private val listener: ClientListener,
) {
    private val trust = PinnedTrustManager(fingerprint)
    private val client =
        OkHttpClient
            .Builder()
            .sslSocketFactory(pinnedContext(trust).socketFactory, trust)
            // TLS 1.3 ou 1.2, suites AEAD et ECDHE seulement : jamais de TLS 1.0 / 1.1 ni de suite faible.
            .connectionSpecs(listOf(ConnectionSpec.RESTRICTED_TLS))
            // L'adresse est une IP locale sans nom : c'est l'empreinte épinglée qui authentifie l'hôte.
            .hostnameVerifier { _, _ -> true }
            .apply { network?.let { socketFactory(it.socketFactory) } }
            .connectTimeout(CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
            .pingInterval(PING_S, TimeUnit.SECONDS)
            .build()
    private var socket: WebSocket? = null

    fun connect() {
        socket = client.newWebSocket(Request.Builder().url("wss://$host:$port/").build(), Callbacks())
    }

    fun send(text: String): Boolean = socket?.send(text) ?: false

    fun close() {
        socket?.close(CLOSE_NORMAL, null)
        socket = null
        client.dispatcher.executorService.shutdown()
    }

    private inner class Callbacks : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) = listener.onOpen()

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (text.length <= MAX_MESSAGE_BYTES) listener.onText(text)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) =
            listener.onClosed(wrongHost = generateSequence(t) { it.cause }.any { it is CertificateException })

        // L'hôte ferme (exclusion, fin de partie, connexion non reconnue, message refusé) : on accuse la fermeture
        // tout de suite, sinon elle ne serait constatée qu'au délai réseau.
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = listener.onClosed(wrongHost = false)
    }

    private companion object {
        const val CONNECT_TIMEOUT_S = 10L
        const val PING_S = 15L
        const val CLOSE_NORMAL = 1000
    }
}

private fun pinnedContext(trust: X509TrustManager): SSLContext =
    SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), null) }

/** Protocoles et suites acceptés par l'hôte, parmi ceux que le téléphone sait faire. */
internal val STRICT_PROTOCOLS = listOf("TLSv1.3", "TLSv1.2")
internal val STRICT_SUITES =
    listOf(
        "TLS_AES_128_GCM_SHA256",
        "TLS_AES_256_GCM_SHA384",
        "TLS_CHACHA20_POLY1305_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
    )

private fun strictTls(ssl: SSLContext): SSLParameters {
    val supported = ssl.supportedSSLParameters
    return SSLParameters(
        STRICT_SUITES.filter { it in supported.cipherSuites }.toTypedArray(),
        STRICT_PROTOCOLS.filter { it in supported.protocols }.toTypedArray(),
    )
}
