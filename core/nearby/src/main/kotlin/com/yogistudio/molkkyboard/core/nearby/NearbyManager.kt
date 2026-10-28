package com.yogistudio.molkkyboard.core.nearby

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import com.yogistudio.molkkyboard.core.data.GameAuthority
import com.yogistudio.molkkyboard.core.data.GameOrigin
import com.yogistudio.molkkyboard.core.data.GameRepository
import com.yogistudio.molkkyboard.core.data.GuestGameStore
import com.yogistudio.molkkyboard.core.data.IdGenerator
import com.yogistudio.molkkyboard.core.data.NearbyRouter
import com.yogistudio.molkkyboard.core.data.PlayerRepository
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.scoring.Fault
import com.yogistudio.molkkyboard.core.scoring.SetStatus
import com.yogistudio.molkkyboard.core.scoring.Throw
import com.yogistudio.molkkyboard.core.sync.CommandOutcome
import com.yogistudio.molkkyboard.core.sync.ConnectionId
import com.yogistudio.molkkyboard.core.sync.DeviceId
import com.yogistudio.molkkyboard.core.sync.GameCommand
import com.yogistudio.molkkyboard.core.sync.GuestDevice
import com.yogistudio.molkkyboard.core.sync.GuestSession
import com.yogistudio.molkkyboard.core.sync.GuestStatus
import com.yogistudio.molkkyboard.core.sync.HostSession
import com.yogistudio.molkkyboard.core.sync.JoinCandidate
import com.yogistudio.molkkyboard.core.sync.JoinRejection
import com.yogistudio.molkkyboard.core.sync.Message
import com.yogistudio.molkkyboard.core.sync.Outgoing
import com.yogistudio.molkkyboard.core.sync.RejectCode
import com.yogistudio.molkkyboard.core.sync.decodeMessage
import com.yogistudio.molkkyboard.core.sync.encode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import javax.inject.Inject
import javax.inject.Singleton

enum class NearbyRole { NONE, HOST, GUEST }

enum class NearbyError {
    /** Ni Wi-Fi ni point d'accès : aucune adresse locale. */
    NO_NETWORK,

    /** Le point d'accès n'a pas démarré (Android 12 ou moins, permission refusée, Wi-Fi occupé). */
    HOTSPOT_FAILED,

    /** Le serveur ou le certificat de partie n'a pas démarré. */
    SERVER_FAILED,

    /** Lien illisible ou incomplet. */
    INVALID_LINK,

    /** Le réseau de la partie est refusé ou hors de portée. */
    NETWORK_UNAVAILABLE,

    /** L'empreinte ne correspond pas : ce n'est pas le téléphone de l'organisateur. */
    WRONG_HOST,
}

/** Ce qui arrive aux autres téléphones, annoncé à l'écran (« Tristan s'est déconnecté »…). */
sealed interface NearbyNotice {
    /** Hôte : l'invité a quitté la partie de lui-même. */
    data class GuestLeft(
        val name: String,
    ) : NearbyNotice

    /** Hôte : connexion perdue avec l'invité (il revient tout seul s'il le peut). */
    data class GuestOffline(
        val name: String,
    ) : NearbyNotice

    /** Hôte : l'invité est de retour. */
    data class GuestBack(
        val name: String,
    ) : NearbyNotice

    /** Invité : l'organisateur a déconnecté ce téléphone. */
    data object RemovedByHost : NearbyNotice

    /** Invité : l'organisateur a arrêté le partage. */
    data object SharingStopped : NearbyNotice

    /** Partie terminée ou abandonnée : le partage s'arrête tout seul ([host] : sur le téléphone de l'organisateur). */
    data class GameOver(
        val host: Boolean,
    ) : NearbyNotice
}

/** Ce que les écrans « Inviter » et « Rejoindre », et la pastille de l'écran de jeu, affichent. */
data class NearbyState(
    val role: NearbyRole = NearbyRole.NONE,
    val gameId: String? = null,
    val starting: Boolean = false,
    val link: String? = null,
    val networkMode: NetworkMode? = null,
    val ssid: String? = null,
    val candidates: List<JoinCandidate> = emptyList(),
    val guests: List<GuestDevice> = emptyList(),
    val guestStatus: GuestStatus? = null,
    val rejection: JoinRejection? = null,
    val connected: Boolean = false,
    val error: NearbyError? = null,
    /** Invité : joueur dont ce téléphone saisit les lancers. */
    val player: String? = null,
) {
    /** Téléphones connectés à la partie, ce téléphone compris. */
    val phones: Int get() = 1 + guestsOnline

    /** Invités connectés (sans ce téléphone). */
    val guestsOnline: Int get() = guests.count { it.connection != null }
}

/**
 * La partie à côté sur ce téléphone, hôte ou invité (`ARCHITECTURE.md` § 6 bis). Les sessions du protocole sont
 * confinées à un seul fil ; le réseau ne fait que porter leurs messages. Fournit le routage des saisies
 * ([NearbyRouter]) : sur l'hôte, elles passent par la même validation que celles des invités ; sur un invité, elles
 * sont proposées à l'hôte et attendent sa réponse. Hôte et invité dans un seul
 * gestionnaire : ils partagent le fil des sessions, le routage et l'état affiché.
 */
@Suppress("TooManyFunctions")
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class NearbyManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val games: GameRepository,
        private val players: PlayerRepository,
        private val ids: IdGenerator,
    ) : NearbyRouter {
        private val session = Dispatchers.IO.limitedParallelism(1)
        private val scope = CoroutineScope(SupervisorJob() + session)
        private val mutable = MutableStateFlow(NearbyState())
        val state: StateFlow<NearbyState> = mutable.asStateFlow()

        private var host: HostSession? = null
        private var authority: GameAuthority? = null
        private var server: NearbyServer? = null
        private var hotspot: Hotspot? = null
        private var invite: JoinLink? = null

        private var guest: GuestSession? = null
        private var store: GuestGameStore? = null
        private var client: NearbyClient? = null
        private var partyNetwork: PartyNetwork? = null
        private var link: JoinLink? = null
        private var network: Network? = null
        private var reconnect: Job? = null
        private var renewal: Job? = null
        private var ending: Job? = null
        private val noticeFlow = MutableSharedFlow<NearbyNotice>(extraBufferCapacity = NOTICE_BUFFER)

        /** Événements à annoncer à l'écran (départs, exclusions, fin du partage). */
        val notices: SharedFlow<NearbyNotice> = noticeFlow.asSharedFlow()
        private val waiters = HashMap<String, CompletableDeferred<CommandOutcome>>()

        // ---- Hôte ----

        /** Partage la partie [gameId] : réseau, certificat de partie, serveur, QR code, service de premier plan. */
        fun startHosting(gameId: String, mode: NetworkMode) {
            scope.launch {
                stopAll()
                mutable.value =
                    NearbyState(role = NearbyRole.HOST, gameId = gameId, starting = true, networkMode = mode)
                val spot =
                    if (mode ==
                        NetworkMode.HOTSPOT
                    ) {
                        startHotspot(context.getSystemService(WifiManager::class.java))
                    } else {
                        null
                    }
                if (mode == NetworkMode.HOTSPOT && spot == null) return@launch fail(NearbyError.HOTSPOT_FAILED)
                hotspot = spot
                // Le point d'accès met un instant à obtenir son adresse.
                val address = waitForAddress(spot != null) ?: return@launch fail(NearbyError.NO_NETWORK)
                val started =
                    runCatching {
                        val certificate = PartyCertificate.keystore()
                        val gameAuthority = GameAuthority(games, gameId) { avatars() }
                        val hostSession = HostSession(gameAuthority)
                        val nearby =
                            NearbyServer(
                                InetSocketAddress(address, 0),
                                certificate,
                                onText = { id, text -> scope.launch { onHostText(id, text) } },
                                onClosed = { id ->
                                    scope.launch {
                                        guestOn(id)?.let { noticeFlow.tryEmit(NearbyNotice.GuestOffline(it)) }
                                        host?.disconnected(id)
                                        publishHost()
                                    }
                                },
                            )
                        val port = nearby.startAndWait()
                        check(port > 0)
                        authority = gameAuthority
                        host = hostSession
                        server = nearby
                        invite =
                            JoinLink(
                                address,
                                port,
                                certificate.fingerprint,
                                hostSession.newInvite(),
                                spot?.ssid,
                                spot?.passphrase,
                            )
                    }
                if (started.isFailure) return@launch fail(NearbyError.SERVER_FAILED)
                games.setOrigin(gameId, GameOrigin.HOSTED)
                ContextCompat.startForegroundService(context, HostService.start(context))
                mutable.update { it.copy(starting = false, ssid = spot?.ssid) }
                publishHost()
                renewal = renewInvitesForever()
                ending = stopWhenGameCloses(gameId)
            }
        }

        fun accept(connection: ConnectionId, player: PlayerId?) =
            scope.launch {
                host?.let { dispatch(it.accept(connection, player)) }
                publishHost()
            }

        fun refuse(connection: ConnectionId) =
            scope.launch {
                host?.let { dispatch(it.refuse(connection)) }
                publishHost()
            }

        fun exclude(device: DeviceId) =
            scope.launch {
                host?.let { dispatch(it.exclude(device)) }
                publishHost()
            }

        /** Le jeton d'invitation ne vaut que 2 minutes : le QR code affiché est renouvelé avant expiration. */
        private fun renewInvitesForever(): Job =
            scope.launch {
                while (true) {
                    delay(INVITE_RENEW_MS)
                    renewInvite()
                }
            }

        /** Nouveau QR code (le précédent expire au bout de 2 minutes, ou après une arrivée). */
        fun renewInvite() =
            scope.launch {
                val session = host ?: return@launch
                invite = invite?.copy(token = session.newInvite())
                publishHost()
            }

        private suspend fun onHostText(connection: ConnectionId, text: String) {
            val session = host ?: return
            val message = decodeMessage(text)
            if (message == null) {
                server?.close(connection)
                return
            }
            val before = guestOn(connection)
            dispatch(session.receive(connection, message))
            when (message) {
                Message.Leave -> before?.let { noticeFlow.tryEmit(NearbyNotice.GuestLeft(it)) }
                is Message.Resume -> guestOn(connection)?.let { noticeFlow.tryEmit(NearbyNotice.GuestBack(it)) }
                else -> Unit
            }
            // Le jeton vient de servir : le QR code suivant en porte un nouveau.
            if (message is Message.JoinRequest) invite = invite?.copy(token = session.newInvite())
            publishHost()
        }

        private fun broadcast(text: String) {
            val nearby = server ?: return
            host?.connectedGuests?.forEach { nearby.send(it, text) }
        }

        private fun dispatch(outgoing: List<Outgoing>) {
            val nearby = server ?: return
            outgoing.forEach { out ->
                when (out) {
                    is Outgoing.To -> {
                        out.message.encode()?.let { nearby.send(out.connection, it) }
                    }

                    is Outgoing.Broadcast -> {
                        out.message.encode()?.let(::broadcast)
                    }

                    is Outgoing.Close -> {
                        nearby.close(out.connection)
                    }
                }
            }
        }

        private fun publishHost() {
            val session = host
            mutable.update {
                it.copy(
                    gameId = authority?.gameId ?: it.gameId,
                    link = invite?.format(),
                    candidates = session?.candidates.orEmpty(),
                    guests = session?.guests.orEmpty(),
                )
            }
            HostService.refresh(context, mutable.value.guestsOnline)
        }

        private suspend fun waitForAddress(hotspot: Boolean): String? {
            repeat(ADDRESS_TRIES) {
                partyAddress(hotspot)?.let { return it }
                delay(ADDRESS_DELAY_MS)
            }
            return null
        }

        private suspend fun avatars(): Map<String, String> =
            players.profiles
                .first()
                .mapNotNull { p -> p.avatar?.let { p.id to it } }
                .toMap()

        // ---- Invité ----

        /** Rejoint la partie du lien scanné, sous le pseudo [nickname]. */
        fun join(text: String, nickname: String, avatar: String?) {
            scope.launch {
                stopAll()
                val parsed = JoinLink.parse(text) ?: return@launch fail(NearbyError.INVALID_LINK, NearbyRole.GUEST)
                link = parsed
                val guestStore = GuestGameStore(games)
                store = guestStore
                val session = GuestSession(guestStore) { ids.newId() }
                guest = session
                mutable.value =
                    NearbyState(role = NearbyRole.GUEST, starting = true, guestStatus = GuestStatus.CONNECTING)
                partyNetwork =
                    requestPartyNetwork(
                        context.getSystemService(ConnectivityManager::class.java),
                        parsed.ssid,
                        parsed.passphrase,
                        onNetwork = { net ->
                            scope.launch {
                                network = net
                                connectGuest(nickname, avatar)
                            }
                        },
                        onUnavailable = { scope.launch { fail(NearbyError.NETWORK_UNAVAILABLE, NearbyRole.GUEST) } },
                    )
            }
        }

        private fun connectGuest(nickname: String, avatar: String?) {
            val target = link ?: return
            client?.close()
            val nearby =
                NearbyClient(
                    target.host,
                    target.port,
                    target.fingerprint,
                    network,
                    object : ClientListener {
                        override fun onOpen() {
                            scope.launch {
                                val session = guest ?: return@launch
                                val first =
                                    session.resumeRequest() ?: session.joinRequest(target.token, nickname, avatar)
                                send(first)
                                session.unanswered().forEach { send(it) }
                                mutable.update { it.copy(connected = true, starting = false) }
                            }
                        }

                        override fun onText(text: String) {
                            scope.launch { onGuestText(text) }
                        }

                        override fun onClosed(wrongHost: Boolean) {
                            scope.launch { onGuestClosed(wrongHost, nickname, avatar) }
                        }
                    },
                )
            client = nearby
            nearby.connect()
        }

        private suspend fun onGuestText(text: String) {
            val session = guest ?: return
            val message = decodeMessage(text) ?: return
            val wasPlaying = session.status == GuestStatus.PLAYING
            session.receive(message).forEach { send(it) }
            if (wasPlaying && session.status == GuestStatus.REMOVED) noticeFlow.tryEmit(NearbyNotice.RemovedByHost)
            if (wasPlaying && session.status == GuestStatus.ENDED) noticeFlow.tryEmit(endNotice())
            // Réponses aux propositions de ce téléphone.
            session.outcomes.keys.toList().forEach { id ->
                session.outcomes.remove(id)?.let { waiters.remove(id)?.complete(it) }
            }
            mutable.update {
                it.copy(
                    gameId = store?.gameId,
                    guestStatus = session.status,
                    player = session.player?.value,
                    rejection = session.rejection,
                    starting = session.status == GuestStatus.CONNECTING,
                )
            }
            if (session.status in FINAL) closeGuest(keepStatus = true)
        }

        private suspend fun onGuestClosed(wrongHost: Boolean, nickname: String, avatar: String?) {
            mutable.update { it.copy(connected = false) }
            val session = guest ?: return
            when {
                wrongHost -> {
                    fail(NearbyError.WRONG_HOST, NearbyRole.GUEST)
                }

                session.status in FINAL -> {
                    Unit
                }

                // Coupure : nouvelle tentative avec le jeton de session et le dernier numéro reçu.
                else -> {
                    reconnect?.cancel()
                    reconnect =
                        scope.launch {
                            delay(RECONNECT_DELAY_MS)
                            if (guest === session) connectGuest(nickname, avatar)
                        }
                }
            }
        }

        private fun send(message: Message) {
            message.encode()?.let { client?.send(it) }
        }

        // ---- Routage des saisies ----

        override fun routes(gameId: String): Boolean {
            val current = mutable.value
            return current.role != NearbyRole.NONE && current.gameId == gameId && (host != null || guest != null)
        }

        override suspend fun submit(gameId: String, command: GameCommand): CommandOutcome =
            withContext(session) {
                val hostSession = host
                val guestSession = guest
                when {
                    hostSession != null -> {
                        val (outcome, outgoing) = hostSession.submit(command)
                        dispatch(outgoing)
                        publishHost()
                        outcome
                    }

                    // Hôte parti, téléphone exclu ou refusé : la partie reste en lecture seule.
                    guestSession != null && guestSession.status != GuestStatus.PLAYING -> {
                        CommandOutcome.Rejected(RejectCode.FORBIDDEN)
                    }

                    guestSession != null -> {
                        val proposal = guestSession.propose(command)
                        val waiter = CompletableDeferred<CommandOutcome>()
                        waiters[proposal.id] = waiter
                        send(proposal)
                        withTimeoutOrNull(ANSWER_TIMEOUT_MS) { waiter.await() }
                            ?: CommandOutcome.Rejected(RejectCode.STALE).also { waiters.remove(proposal.id) }
                    }

                    else -> {
                        games.apply(gameId, command)
                    }
                }
            }

        // ---- Fin ----

        /** Arrêt du partage (hôte), ou départ de la partie (invité : l'hôte en est prévenu). */
        fun stop() =
            scope.launch {
                if (guest?.status == GuestStatus.PLAYING) send(Message.Leave)
                stopAll()
            }

        /** Invité : fin du partage parce que la partie est finie, ou parce que l'organisateur l'a arrêté. */
        private suspend fun endNotice(): NearbyNotice {
            val game = store?.gameId?.let { games.observe(it).first() }
            val over =
                game != null &&
                    (
                        game.closedAt != null || game.abandoned ||
                            game.currentSet.replay.state.status == SetStatus.FINISHED
                    )
            return if (over) NearbyNotice.GameOver(host = false) else NearbyNotice.SharingStopped
        }

        /** Partie terminée ou abandonnée : fin du partage, les invités sont prévenus (`GAME_ENDED`). */
        private fun stopWhenGameCloses(gameId: String): Job =
            scope.launch {
                games.observe(gameId).first { it == null || it.closedAt != null }
                noticeFlow.tryEmit(NearbyNotice.GameOver(host = true))
                stopAll()
            }

        /** Pseudo de l'invité accepté sur cette connexion. */
        private fun guestOn(connection: ConnectionId): String? =
            host?.guests?.firstOrNull { it.connection == connection }?.nickname

        private fun stopAll() {
            renewal?.cancel()
            renewal = null
            ending?.cancel()
            ending = null
            host?.let { dispatch(it.end()) }
            server?.let { runCatching { it.stop(STOP_TIMEOUT_MS) } }
            if (server != null) PartyCertificate.delete()
            hotspot?.close()
            if (host != null) HostService.stop(context)
            host = null
            authority = null
            server = null
            hotspot = null
            invite = null
            closeGuest(keepStatus = false)
            mutable.value = NearbyState()
        }

        private fun closeGuest(keepStatus: Boolean) {
            reconnect?.cancel()
            client?.close()
            partyNetwork?.release()
            waiters.values.forEach { it.complete(CommandOutcome.Rejected(RejectCode.STALE)) }
            waiters.clear()
            client = null
            partyNetwork = null
            network = null
            if (!keepStatus) {
                guest = null
                store = null
                link = null
            }
        }

        private fun fail(error: NearbyError, role: NearbyRole = NearbyRole.HOST) {
            stopAll()
            mutable.value = NearbyState(role = role, error = error)
        }

        private companion object {
            val FINAL = setOf(GuestStatus.REJECTED, GuestStatus.REMOVED, GuestStatus.ENDED)
            const val ADDRESS_TRIES = 20
            const val NOTICE_BUFFER = 8
            const val ADDRESS_DELAY_MS = 250L
            const val RECONNECT_DELAY_MS = 2_000L
            const val ANSWER_TIMEOUT_MS = 10_000L
            const val STOP_TIMEOUT_MS = 1_000
            const val INVITE_RENEW_MS = 90_000L
        }
    }
