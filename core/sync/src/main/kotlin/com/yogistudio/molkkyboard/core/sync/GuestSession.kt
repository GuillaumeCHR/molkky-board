package com.yogistudio.molkkyboard.core.sync

import com.yogistudio.molkkyboard.core.model.PlayerId

/** Ce que l'invité fait de ce que l'hôte lui envoie : créer la partie reçue, et appliquer les commandes diffusées. */
interface GuestStore {
    /** Crée (ou remplace) la partie reçue, telle quelle. */
    suspend fun importGame(game: SharedGame)

    /** Applique une commande déjà validée par l'hôte ; [author] : le téléphone qui l'a proposée. */
    suspend fun applyRemote(command: GameCommand, author: String)
}

/** État de la connexion vu par l'invité. */
enum class GuestStatus { CONNECTING, WAITING_APPROVAL, PLAYING, REJECTED, REMOVED, ENDED }

/**
 * Un invité d'une partie à côté, sans réseau. Il n'écrit dans sa partie que ce que l'hôte diffuse, dans l'ordre des
 * numéros ; un trou déclenche une demande de rattrapage. Ses propres propositions attendent la réponse de l'hôte
 * ([outcomes]) : acceptées quand l'événement correspondant arrive, refusées sinon.
 */
class GuestSession(
    private val store: GuestStore,
    private val newId: () -> String,
) {
    var status: GuestStatus = GuestStatus.CONNECTING
        private set
    var rejection: JoinRejection? = null
        private set
    var lastSeq: Long = 0
        private set
    var sessionToken: String? = null
        private set
    var device: String? = null
        private set
    var player: PlayerId? = null
        private set

    /** Réponses aux propositions de ce téléphone, par identifiant (consommées par l'appelant). */
    val outcomes = mutableMapOf<String, CommandOutcome>()
    private val pending = linkedMapOf<String, GameCommand>()

    fun joinRequest(token: String, nickname: String, avatar: String?): Message =
        Message.JoinRequest(PROTOCOL_VERSION, token, nickname, avatar)

    /** Message de reconnexion, ou `null` si ce téléphone n'a jamais été accepté. */
    fun resumeRequest(): Message? = sessionToken?.let { Message.Resume(it, lastSeq) }

    /** Une commande à proposer à l'hôte : le message à envoyer (à renvoyer tel quel après une coupure). */
    fun propose(command: GameCommand): Message.Proposal {
        val id = newId()
        pending[id] = command
        return Message.Proposal(id, command)
    }

    /** Propositions sans réponse, à renvoyer après une reconnexion (l'hôte ignore les doublons). */
    fun unanswered(): List<Message.Proposal> = pending.map { (id, command) -> Message.Proposal(id, command) }

    /** Un message de l'hôte ; renvoie ceux à lui répondre. */
    suspend fun receive(message: Message): List<Message> =
        when (message) {
            Message.JoinPending -> {
                status = GuestStatus.WAITING_APPROVAL
                emptyList()
            }

            is Message.Welcome -> {
                sessionToken = message.sessionToken
                device = message.device
                player = message.player
                restore(message.game, message.lastSeq)
                emptyList()
            }

            is Message.Snapshot -> {
                restore(message.game, message.lastSeq)
                emptyList()
            }

            is Message.Event -> {
                event(message)
            }

            is Message.EventRejected -> {
                pending.remove(message.id)?.let { outcomes[message.id] = CommandOutcome.Rejected(message.code) }
                emptyList()
            }

            is Message.JoinRejected -> {
                status = GuestStatus.REJECTED
                rejection = message.reason
                emptyList()
            }

            Message.DeviceRemoved -> {
                status = GuestStatus.REMOVED
                emptyList()
            }

            Message.GameEnded -> {
                status = GuestStatus.ENDED
                emptyList()
            }

            Message.Ping -> {
                listOf(Message.Pong)
            }

            else -> {
                emptyList()
            }
        }

    private suspend fun restore(game: SharedGame, seq: Long) {
        store.importGame(game)
        lastSeq = seq
        status = GuestStatus.PLAYING
    }

    private suspend fun event(event: Message.Event): List<Message> =
        when {
            // Déjà appliqué (renvoi après une reconnexion) : rien à faire.
            event.seq <= lastSeq -> {
                emptyList()
            }

            // Il en manque : on redemande à partir du dernier reçu.
            event.seq > lastSeq + 1 -> {
                listOfNotNull(resumeRequest())
            }

            else -> {
                store.applyRemote(event.command, event.author)
                lastSeq = event.seq
                if (pending.remove(event.id) != null) outcomes[event.id] = CommandOutcome.Applied
                emptyList()
            }
        }
}
