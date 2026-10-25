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
 * Événement du journal d'une manche. Le journal est en ajout seul : [Undo] et [Correct] sont
 * eux-mêmes des événements, interprétés par [replay].
 */
@Serializable
sealed interface SetEvent {
    val id: EventId
}

/** Événement de jeu, appliqué par [reduce]. */
@Serializable
sealed interface PlayEvent : SetEvent

/** Lancer régulier ; `pins` vide = raté. Liste brute pour pouvoir détecter les doublons. */
@Serializable
@SerialName("throw")
data class Throw(
    override val id: EventId,
    val team: TeamId,
    val player: PlayerId,
    val pins: List<Int>,
) : PlayEvent

/** Faute de l'équipe dont c'est le tour. Pour [FaultKind.WRONG_PLAYER], `player` est le joueur fautif. */
@Serializable
@SerialName("fault")
data class Fault(
    override val id: EventId,
    val team: TeamId,
    val player: PlayerId,
    val kind: FaultKind,
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
    val pins: List<Int>,
) : PlayEvent

/** Abandon de la manche : plus aucun lancer n'est accepté. */
@Serializable
@SerialName("abandon")
data class Abandon(
    override val id: EventId,
) : PlayEvent

/** Annule l'événement [target], qui doit être le dernier du journal effectif. */
@Serializable
@SerialName("undo")
data class Undo(
    override val id: EventId,
    val target: EventId,
) : SetEvent

/** Remplace l'événement [target] du journal effectif (correction par l'organisateur). */
@Serializable
@SerialName("correct")
data class Correct(
    override val id: EventId,
    val target: EventId,
    val replacement: PlayEvent,
) : SetEvent
