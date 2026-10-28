package com.yogistudio.molkkyboard.core.sync

import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.scoring.MatchConfig
import com.yogistudio.molkkyboard.core.scoring.MolkkoutThrow
import com.yogistudio.molkkyboard.core.scoring.SetEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Version du protocole de la partie à côté : un téléphone d'une autre version est refusé avec un motif clair. */
const val PROTOCOL_VERSION = 1

/** Taille maximale d'un message, en octets (`ARCHITECTURE.md` § 6 bis). */
const val MAX_MESSAGE_BYTES = 64 * 1024

/** Nombre maximal de téléphones invités dans une partie. */
const val MAX_GUESTS = 12

/**
 * Messages échangés entre l'hôte (l'autorité) et les invités. JSON, discriminant `type`. Les invités proposent,
 * l'hôte valide, numérote et diffuse ; un invité n'écrit jamais dans son journal que ce que l'hôte a diffusé.
 */
@Serializable
sealed interface Message {
    /** Invité → hôte : première demande, avec le jeton à usage unique lu dans le QR code. */
    @Serializable
    @SerialName("join_request")
    data class JoinRequest(
        val version: Int,
        val token: String,
        val nickname: String,
        val avatar: String? = null,
    ) : Message

    /** Hôte → invité : demande reçue, l'organisateur doit l'accepter. */
    @Serializable
    @SerialName("join_pending")
    data object JoinPending : Message

    /** Hôte → invité : accepté. Le jeton de session sert à se reconnecter ; [game] est l'état complet de la partie. */
    @Serializable
    @SerialName("welcome")
    data class Welcome(
        val sessionToken: String,
        val device: String,
        val role: Role,
        val player: PlayerId?,
        val game: SharedGame,
        val lastSeq: Long,
    ) : Message

    @Serializable
    @SerialName("join_rejected")
    data class JoinRejected(
        val reason: JoinRejection,
    ) : Message

    /** Invité → hôte : reconnexion après une coupure, avec le dernier numéro reçu. */
    @Serializable
    @SerialName("resume")
    data class Resume(
        val sessionToken: String,
        val lastSeq: Long,
    ) : Message

    /** Hôte → invité : état complet, quand le retard est trop grand pour renvoyer les événements un à un. */
    @Serializable
    @SerialName("snapshot")
    data class Snapshot(
        val game: SharedGame,
        val lastSeq: Long,
    ) : Message

    /** Invité → hôte : une commande proposée ; [id] rend la proposition idempotente (renvoi après coupure). */
    @Serializable
    @SerialName("proposal")
    data class Proposal(
        val id: String,
        val command: GameCommand,
    ) : Message

    /** Hôte → tous : une commande acceptée, avec son numéro d'ordre. */
    @Serializable
    @SerialName("event")
    data class Event(
        val seq: Long,
        val id: String,
        val author: String,
        val command: GameCommand,
        val at: Long,
    ) : Message

    /** Hôte → auteur : proposition refusée. */
    @Serializable
    @SerialName("event_rejected")
    data class EventRejected(
        val id: String,
        val code: RejectCode,
    ) : Message

    /** Hôte → invité : l'organisateur a exclu ce téléphone. */
    @Serializable
    @SerialName("device_removed")
    data object DeviceRemoved : Message

    /** Invité → hôte : ce téléphone quitte la partie (il la garde en lecture seule). */
    @Serializable
    @SerialName("leave")
    data object Leave : Message

    /** Hôte → tous : fin du partage (partie terminée ou arrêt par l'organisateur). */
    @Serializable
    @SerialName("game_ended")
    data object GameEnded : Message

    @Serializable
    @SerialName("ping")
    data object Ping : Message

    @Serializable
    @SerialName("pong")
    data object Pong : Message
}

@Serializable
enum class JoinRejection {
    /** L'organisateur a refusé. */
    REFUSED,

    /** Jeton inconnu, expiré ou déjà utilisé : il faut scanner le QR code affiché maintenant. */
    TOKEN_INVALID,

    /** Déjà le nombre maximal de téléphones. */
    FULL,

    /** Version de l'app incompatible. */
    VERSION,

    /** Jeton de session inconnu ou révoqué (reconnexion impossible). */
    SESSION_INVALID,
}

/** Une partie telle qu'un invité la reçoit : tout ce qu'il faut pour la créer chez lui et la rejouer à l'identique. */
@Serializable
data class SharedGame(
    val id: String,
    val createdAt: Long,
    val config: GameConfig,
    val matchConfig: MatchConfig,
    val teams: List<SharedTeam>,
    val players: List<SharedPlayer>,
    val sets: List<SharedSet>,
    val molkkout: List<MolkkoutThrow> = emptyList(),
    val place: String? = null,
)

@Serializable
data class SharedTeam(
    val id: TeamId,
    val name: String,
    val colorIndex: Int,
    val emblem: String? = null,
    val memberKey: String,
)

/** Un joueur : pseudo et emblème seulement (jamais de photo ni d'identifiant de profil local). */
@Serializable
data class SharedPlayer(
    val id: PlayerId,
    val name: String,
    val avatar: String? = null,
)

@Serializable
data class SharedSet(
    val lineup: List<TeamLineup>,
    val log: List<SetEvent>,
)

/** JSON du protocole : champs inconnus ignorés (compatibilité ascendante au sein d'une même version). */
val ProtocolJson: Json =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

/** Encode un message ; `null` s'il dépasse [MAX_MESSAGE_BYTES] (il ne doit pas partir). */
fun Message.encode(): String? =
    ProtocolJson.encodeToString(Message.serializer(), this).takeIf { it.toByteArray().size <= MAX_MESSAGE_BYTES }

/** Décode un message reçu ; `null` s'il est trop gros ou illisible (il est ignoré, la connexion peut être fermée). */
fun decodeMessage(text: String): Message? {
    if (text.length > MAX_MESSAGE_BYTES) return null
    return runCatching { ProtocolJson.decodeFromString(Message.serializer(), text) }.getOrNull()
}
