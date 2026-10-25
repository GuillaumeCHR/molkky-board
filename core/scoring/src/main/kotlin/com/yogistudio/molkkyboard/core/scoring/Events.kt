package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** Identifiant unique d'un événement (UUID généré par l'appareil qui le saisit). */
@Serializable
@JvmInline
value class EventId(
    val value: String,
)

/**
 * Événement du journal d'une manche. Le journal est en ajout seul : [Undo], [Correct] et [Delete] sont
 * eux-mêmes des événements, interprétés par [replay].
 */
@Serializable
sealed interface SetEvent {
    val id: EventId
}

/** Événement de jeu, appliqué par [reduce]. */
@Serializable
sealed interface PlayEvent : SetEvent

/** Résultat d'un lancer : quilles touchées (mode « Quilles ») ou points saisis (mode « Rapide »). */
@Serializable
sealed interface ThrowResult {
    /** Liste brute des quilles tombées (vide = raté) : alimente les statistiques par quille. */
    @Serializable
    @SerialName("pins")
    data class Pins(
        val pins: List<Int>,
    ) : ThrowResult

    /** Saisie rapide : la valeur d'une quille seule, ou un nombre de quilles tombées (0 = raté). */
    @Serializable
    @SerialName("points")
    data class Points(
        val value: Int,
        val kind: PointsKind,
    ) : ThrowResult
}

@Serializable
enum class PointsKind { SINGLE_PIN, PIN_COUNT }

/**
 * Lancer régulier. Il appartient au lanceur dont c'est le tour **à sa position dans le journal** :
 * [by] (le joueur qui saisit) n'est contrôlé qu'à la saisie par [checkInput], jamais au rejeu.
 */
@Serializable
@SerialName("throw")
data class Throw(
    override val id: EventId,
    val result: ThrowResult,
    val by: PlayerId? = null,
) : PlayEvent

/** Faute de l'équipe dont c'est le tour. Pour [FaultKind.WRONG_PLAYER], [by] est le joueur fautif (obligatoire). */
@Serializable
@SerialName("fault")
data class Fault(
    override val id: EventId,
    val kind: FaultKind,
    val by: PlayerId? = null,
) : PlayEvent

@Serializable
enum class FaultKind(
    val triggersRule37: Boolean,
) {
    /** Mordu : pied au-delà du mölkkaari ou des lignes latérales. */
    TRESPASS(triggersRule37 = true),

    /** Mölkky lâché dans la zone de lancer. */
    DROPPED(triggersRule37 = false),

    /** Temps de lancer dépassé (second dépassement). */
    TIMEOUT(triggersRule37 = false),

    /** Joueur absent ou incapable de lancer. */
    ABSENT(triggersRule37 = false),

    /** Un autre joueur de l'équipe a lancé : l'ordre continue comme si le bon joueur avait lancé. */
    WRONG_PLAYER(triggersRule37 = true),
}

/**
 * Une équipe a lancé hors de son tour : les points ne comptent pas et elle perdra son prochain lancer
 * régulier ; le tour ne change pas.
 */
@Serializable
@SerialName("wrong_team")
data class WrongTeam(
    override val id: EventId,
    val team: TeamId,
    val player: PlayerId,
    val result: ThrowResult,
) : PlayEvent

/** Abandon de la manche : plus aucun lancer n'est accepté. */
@Serializable
@SerialName("abandon")
data class Abandon(
    override val id: EventId,
) : PlayEvent

/**
 * Un joueur rejoint la partie en cours. [team] existante : il s'ajoute à la fin de sa rotation ;
 * sinon une nouvelle équipe est créée en fin d'ordre de passage, avec le score de départ.
 */
@Serializable
@SerialName("add_player")
data class AddPlayer(
    override val id: EventId,
    val player: PlayerId,
    val team: TeamId,
) : PlayEvent

/** Un joueur quitte la partie ; une équipe qui n'a plus de joueur est « partie ». */
@Serializable
@SerialName("remove_player")
data class RemovePlayer(
    override val id: EventId,
    val player: PlayerId,
) : PlayEvent

/** Nouvel ordre de passage des équipes et, pour les équipes citées, de leurs joueurs. Entre deux tours seulement. */
@Serializable
@SerialName("reorder")
data class Reorder(
    override val id: EventId,
    val teams: List<TeamId>,
    val players: Map<TeamId, List<PlayerId>> = emptyMap(),
) : PlayEvent

/** Annule l'événement [target], qui doit être le dernier du journal effectif. */
@Serializable
@SerialName("undo")
data class Undo(
    override val id: EventId,
    val target: EventId,
) : SetEvent

/** Remplace l'événement [target] du journal effectif (correction). */
@Serializable
@SerialName("correct")
data class Correct(
    override val id: EventId,
    val target: EventId,
    val replacement: PlayEvent,
) : SetEvent

/** Supprime l'événement [target] du journal effectif, où qu'il soit : tout est recalculé. */
@Serializable
@SerialName("delete")
data class Delete(
    override val id: EventId,
    val target: EventId,
) : SetEvent
