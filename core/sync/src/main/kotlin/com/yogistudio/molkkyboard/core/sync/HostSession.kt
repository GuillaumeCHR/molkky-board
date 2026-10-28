package com.yogistudio.molkkyboard.core.sync

import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.withoutUnsafeChars
import java.security.MessageDigest
import java.security.SecureRandom

/** Ce que l'hôte sait faire de sa partie : appliquer une commande validée, et la décrire en entier. */
interface Authority {
    /** Valide et applique [command] au nom de [author] (même porte que les saisies de l'organisateur). */
    suspend fun apply(command: GameCommand, author: Author): CommandOutcome

    /** État complet de la partie en cours, pour un invité qui arrive ou qui a trop de retard. */
    suspend fun snapshot(): SharedGame
}

/** Une connexion réseau ouverte (identifiant attribué par le transport). */
@JvmInline
value class ConnectionId(
    val value: Long,
)

/** Message à envoyer : à une connexion, ou à tous les invités acceptés et connectés. */
sealed interface Outgoing {
    val message: Message

    data class To(
        val connection: ConnectionId,
        override val message: Message,
    ) : Outgoing

    data class Broadcast(
        override val message: Message,
    ) : Outgoing

    /** Fermer cette connexion après l'envoi éventuel des messages précédents. */
    data class Close(
        val connection: ConnectionId,
        override val message: Message = Message.GameEnded,
    ) : Outgoing
}

/** Demande d'un téléphone, en attente de la décision de l'organisateur. */
data class JoinCandidate(
    val connection: ConnectionId,
    val nickname: String,
    val avatar: String?,
)

/** Un téléphone accepté dans la partie. */
data class GuestDevice(
    val device: DeviceId,
    val nickname: String,
    val player: PlayerId?,
    val connection: ConnectionId?,
    internal val sessionToken: String,
)

/**
 * L'hôte d'une partie à côté, sans réseau : il reçoit des messages et renvoie ceux à envoyer. Le transport l'appelle
 * depuis un seul fil à la fois. Règles (`ARCHITECTURE.md` § 6 bis, `THREAT_MODEL.md`) : jeton d'invitation à usage
 * unique valable [INVITE_TTL_MS] ; validation humaine de chaque arrivée ; jeton de session de 256 bits par téléphone ;
 * chaque proposition est validée par l'[Authority], numérotée et diffusée à tous ; une proposition renvoyée reçoit la
 * même réponse sans être appliquée deux fois ; un téléphone exclu ne revient pas.
 */
class HostSession(
    private val authority: Authority,
    private val clock: () -> Long = System::currentTimeMillis,
    private val random: SecureRandom = SecureRandom(),
) {
    private var invite: String? = null
    private var inviteExpiresAt = 0L
    private val pendingJoins = linkedMapOf<ConnectionId, JoinCandidate>()
    private val devices = linkedMapOf<DeviceId, GuestDevice>()
    private val revoked = mutableSetOf<String>()
    private val events = mutableListOf<Message.Event>()
    private val processed = mutableMapOf<String, Result>()

    /** Organisateur : ce téléphone, qui saisit directement par [submit]. */
    val organizer = Author(DeviceId("hote"), Role.ORGANIZER)

    val candidates: List<JoinCandidate> get() = pendingJoins.values.toList()
    val guests: List<GuestDevice> get() = devices.values.toList()
    val lastSeq: Long get() = events.size.toLong()

    private sealed interface Result {
        data class Accepted(
            val seq: Long,
        ) : Result

        data class Refused(
            val code: RejectCode,
        ) : Result
    }

    /** Nouveau jeton d'invitation (le précédent n'est plus valable) : il va dans le QR code. */
    fun newInvite(): String {
        val token = randomHex(INVITE_BYTES)
        invite = token
        inviteExpiresAt = clock() + INVITE_TTL_MS
        return token
    }

    /** Saisie de l'organisateur : même validation que pour les invités, puis diffusion. */
    suspend fun submit(command: GameCommand): Pair<CommandOutcome, List<Outgoing>> {
        val id = "hote-" + randomHex(ID_BYTES)
        return when (val outcome = authority.apply(command, organizer)) {
            CommandOutcome.Applied -> outcome to listOf(Outgoing.Broadcast(record(id, organizer.device, command)))
            is CommandOutcome.Rejected -> outcome to emptyList()
        }
    }

    /** Un message reçu sur [connection]. */
    suspend fun receive(connection: ConnectionId, message: Message): List<Outgoing> =
        when (message) {
            is Message.JoinRequest -> join(connection, message)

            is Message.Resume -> resume(connection, message)

            is Message.Proposal -> propose(connection, message)

            Message.Ping -> listOf(Outgoing.To(connection, Message.Pong))

            Message.Leave -> leave(connection)

            // Un invité n'envoie rien d'autre : la connexion est fermée.
            else -> listOf(Outgoing.Close(connection))
        }

    /** La connexion s'est fermée : une demande en attente tombe, un invité accepté pourra se reconnecter. */
    fun disconnected(connection: ConnectionId) {
        pendingJoins.remove(connection)
        devices.replaceAll { _, device ->
            if (device.connection ==
                connection
            ) {
                device.copy(connection = null)
            } else {
                device
            }
        }
    }

    /** L'organisateur accepte une demande, en associant le téléphone à [player] (ou à aucun joueur). */
    suspend fun accept(connection: ConnectionId, player: PlayerId?): List<Outgoing> {
        val candidate = pendingJoins.remove(connection) ?: return emptyList()
        val device = DeviceId("tel-" + randomHex(ID_BYTES))
        val token = randomHex(SESSION_BYTES)
        devices[device] = GuestDevice(device, candidate.nickname, player, connection, token)
        val welcome = Message.Welcome(token, device.value, Role.SCORER, player, authority.snapshot(), lastSeq)
        return listOf(Outgoing.To(connection, welcome))
    }

    fun refuse(connection: ConnectionId): List<Outgoing> {
        pendingJoins.remove(connection) ?: return emptyList()
        return listOf(Outgoing.To(connection, Message.JoinRejected(JoinRejection.REFUSED)), Outgoing.Close(connection))
    }

    /** Un invité s'en va de lui-même : comme une exclusion, sans message ; il revient en rescannant un QR code. */
    private fun leave(connection: ConnectionId): List<Outgoing> {
        val left =
            devices.values.firstOrNull { it.connection == connection } ?: return listOf(Outgoing.Close(connection))
        devices.remove(left.device)
        revoked += left.sessionToken
        return listOf(Outgoing.Close(connection))
    }

    /** L'organisateur exclut un téléphone : son jeton de session est révoqué. */
    fun exclude(device: DeviceId): List<Outgoing> {
        val removed = devices.remove(device) ?: return emptyList()
        revoked += removed.sessionToken
        val connection = removed.connection ?: return emptyList()
        return listOf(Outgoing.To(connection, Message.DeviceRemoved), Outgoing.Close(connection, Message.DeviceRemoved))
    }

    /** Fin du partage : tous les jetons tombent. */
    fun end(): List<Outgoing> {
        invite = null
        // Destinataires relevés avant de tout oublier : chacun reçoit la fin, puis sa connexion est fermée.
        val connections = devices.values.mapNotNull { it.connection } + pendingJoins.keys
        devices.values.forEach { revoked += it.sessionToken }
        devices.clear()
        pendingJoins.clear()
        return connections.flatMap { listOf(Outgoing.To(it, Message.GameEnded), Outgoing.Close(it)) }
    }

    /** Connexions des invités acceptés et connectés : les destinataires d'une diffusion. */
    val connectedGuests: List<ConnectionId> get() = devices.values.mapNotNull { it.connection }

    private fun join(connection: ConnectionId, request: Message.JoinRequest): List<Outgoing> {
        val expected = invite
        val rejection =
            when {
                request.version != PROTOCOL_VERSION -> {
                    JoinRejection.VERSION
                }

                expected == null || clock() > inviteExpiresAt || !constantTimeEquals(expected, request.token) -> {
                    JoinRejection.TOKEN_INVALID
                }

                devices.size + pendingJoins.size >= MAX_GUESTS -> {
                    JoinRejection.FULL
                }

                else -> {
                    null
                }
            }
        if (rejection != null) {
            return listOf(Outgoing.To(connection, Message.JoinRejected(rejection)), Outgoing.Close(connection))
        }
        // Jeton consommé : le QR code suivant en portera un nouveau.
        invite = null
        val nickname = request.nickname.cleanNickname()
        pendingJoins[connection] = JoinCandidate(connection, nickname, request.avatar?.take(MAX_AVATAR))
        return listOf(Outgoing.To(connection, Message.JoinPending))
    }

    private suspend fun resume(connection: ConnectionId, request: Message.Resume): List<Outgoing> {
        val entry = devices.values.firstOrNull { constantTimeEquals(it.sessionToken, request.sessionToken) }
        if (entry == null || request.sessionToken in revoked) {
            return listOf(
                Outgoing.To(connection, Message.JoinRejected(JoinRejection.SESSION_INVALID)),
                Outgoing.Close(connection),
            )
        }
        devices[entry.device] = entry.copy(connection = connection)
        val missing = request.lastSeq.coerceAtLeast(0)
        // Retard raisonnable : on renvoie ce qui manque ; sinon (ou numéro incohérent), l'état complet.
        return if (missing <= lastSeq && lastSeq - missing <= MAX_REPLAY) {
            events.drop(missing.toInt()).map { Outgoing.To(connection, it) }
        } else {
            listOf(Outgoing.To(connection, Message.Snapshot(authority.snapshot(), lastSeq)))
        }
    }

    private suspend fun propose(connection: ConnectionId, proposal: Message.Proposal): List<Outgoing> {
        val device =
            devices.values.firstOrNull { it.connection == connection } ?: return listOf(Outgoing.Close(connection))
        val id = proposal.id.take(MAX_ID)
        processed[id]?.let { previous ->
            return when (previous) {
                is Result.Accepted -> listOf(Outgoing.To(connection, events[(previous.seq - 1).toInt()]))
                is Result.Refused -> listOf(Outgoing.To(connection, Message.EventRejected(id, previous.code)))
            }
        }
        val author = Author(device.device, Role.SCORER, device.player)
        return when (val outcome = authority.apply(proposal.command, author)) {
            CommandOutcome.Applied -> {
                listOf(Outgoing.Broadcast(record(id, device.device, proposal.command)))
            }

            is CommandOutcome.Rejected -> {
                processed[id] = Result.Refused(outcome.code)
                listOf(Outgoing.To(connection, Message.EventRejected(id, outcome.code)))
            }
        }
    }

    private fun record(id: String, author: DeviceId, command: GameCommand): Message.Event {
        val event = Message.Event(lastSeq + 1, id, author.value, command, clock())
        events += event
        processed[id] = Result.Accepted(event.seq)
        return event
    }

    private fun randomHex(bytes: Int): String =
        ByteArray(bytes).also(random::nextBytes).joinToString("") {
            "%02x".format(it)
        }

    companion object {
        const val INVITE_TTL_MS = 2 * 60 * 1000L
        private const val INVITE_BYTES = 16
        private const val SESSION_BYTES = 32
        private const val ID_BYTES = 8
        private const val MAX_REPLAY = 500
        private const val MAX_ID = 64
        private const val MAX_AVATAR = 32
        private const val MAX_NICKNAME = 20

        private fun constantTimeEquals(a: String, b: String): Boolean =
            MessageDigest.isEqual(
                a.toByteArray(),
                b.toByteArray(),
            )

        /** Pseudo reçu d'un autre téléphone : mêmes règles qu'une saisie locale (borné, sans caractères invisibles). */
        private fun String.cleanNickname(): String =
            withoutUnsafeChars().trim().take(MAX_NICKNAME).ifEmpty { "?" }
    }
}
