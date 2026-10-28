package com.yogistudio.molkkyboard.core.sync

import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.scoring.Abandon
import com.yogistudio.molkkyboard.core.scoring.AddPlayer
import com.yogistudio.molkkyboard.core.scoring.Correct
import com.yogistudio.molkkyboard.core.scoring.Delete
import com.yogistudio.molkkyboard.core.scoring.EventId
import com.yogistudio.molkkyboard.core.scoring.Fault
import com.yogistudio.molkkyboard.core.scoring.FaultKind
import com.yogistudio.molkkyboard.core.scoring.PlayEvent
import com.yogistudio.molkkyboard.core.scoring.RemovePlayer
import com.yogistudio.molkkyboard.core.scoring.Reorder
import com.yogistudio.molkkyboard.core.scoring.ReplayResult
import com.yogistudio.molkkyboard.core.scoring.ScoringError
import com.yogistudio.molkkyboard.core.scoring.SetEvent
import com.yogistudio.molkkyboard.core.scoring.Throw
import com.yogistudio.molkkyboard.core.scoring.Undo
import com.yogistudio.molkkyboard.core.scoring.WrongTeam
import com.yogistudio.molkkyboard.core.scoring.checkInput
import com.yogistudio.molkkyboard.core.scoring.describe
import com.yogistudio.molkkyboard.core.scoring.getOrThrow
import com.yogistudio.molkkyboard.core.scoring.replay
import com.yogistudio.molkkyboard.core.scoring.start

/** Identifiant d'un téléphone dans une partie (aléatoire, propre à la partie : jamais un identifiant matériel). */
@JvmInline
value class DeviceId(
    val value: String,
)

/**
 * Droits d'un téléphone. [SCORER] (rôle par défaut de tout invité, joueur ou arbitre) saisit, annule et corrige les
 * lancers de tout le monde ; [PLAYER] ne touche qu'aux lancers de son joueur ; seul l'[ORGANIZER] gère les joueurs et
 * abandonne la partie.
 */
enum class Role { ORGANIZER, SCORER, PLAYER }

/** Le téléphone qui propose un événement, et le joueur qui lui est associé (pour un [Role.PLAYER]). */
data class Author(
    val device: DeviceId,
    val role: Role,
    val player: PlayerId? = null,
) {
    companion object {
        /** Partie sur un seul téléphone : l'organisateur saisit pour tout le monde. */
        val Local = Author(DeviceId("local"), Role.ORGANIZER)
    }
}

/** Motifs de refus transmis au téléphone qui a proposé (`ARCHITECTURE.md` § 6 bis). */
enum class RejectCode {
    /** Ce n'est plus (ou pas) le tour de ce joueur. */
    NOT_YOUR_TURN,

    /** Le rôle du téléphone ne permet pas cette action. */
    FORBIDDEN,

    /** Refusé par le moteur de score (lancer incohérent, manche finie, correction impossible…). */
    INVALID,

    /** Correction fondée sur un état dépassé : le journal a changé depuis. */
    STALE,
}

sealed interface Validation {
    data class Accepted(
        val result: ReplayResult,
    ) : Validation

    data class Rejected(
        val code: RejectCode,
        /** Erreur du moteur, quand c'est lui qui refuse. */
        val error: ScoringError? = null,
    ) : Validation
}

/** Le journal brut d'une manche, tel que l'autorité le connaît. */
data class SetLog(
    val config: GameConfig,
    val lineup: List<TeamLineup>,
    val log: List<SetEvent>,
)

/**
 * Validation d'un événement proposé par [author], sur le journal **courant** de l'autorité (téléphone hôte, ou
 * serveur plus tard). Fonction pure, la même partout : droits, tour, cohérence avec le moteur, fraîcheur.
 *
 * [baseSeq] est la taille du journal que l'auteur connaissait. Un lancer reste acceptable si c'est toujours le tour
 * de ce joueur ; une annulation, une correction ou une suppression proposée sur un journal dépassé est refusée en
 * [RejectCode.STALE] dès que sa cible a été touchée depuis (et toujours pour une annulation, qui vise « le dernier »).
 */
fun validate(
    set: SetLog,
    event: SetEvent,
    author: Author,
    baseSeq: Int = set.log.size,
): Validation {
    val before = replay(set.config, set.lineup, set.log).getOrThrow()
    val refused =
        when {
            set.log.any { it.id == event.id } -> {
                Validation.Rejected(RejectCode.INVALID, ScoringError.INVALID_EVENT)
            }

            isStale(set.log, event, baseSeq) -> {
                Validation.Rejected(RejectCode.STALE)
            }

            author.role == Role.SCORER && !scorerAllowed(before, event) -> {
                Validation.Rejected(RejectCode.FORBIDDEN)
            }

            author.role == Role.PLAYER && !allowed(set, before, event, author.player) -> {
                Validation.Rejected(RejectCode.FORBIDDEN)
            }

            else -> {
                (event as? PlayEvent)?.let { checkInput(before.state, it) }?.let(::rejection)
            }
        }
    if (refused != null) return refused
    val after = replay(set.config, set.lineup, set.log + event).getOrThrow()
    // Undo, Correct et Delete sont validés par le rejeu : refusés, ils y apparaissent comme rejetés.
    val engine = after.rejected.lastOrNull()?.takeIf { it.event == event }
    return if (engine != null) rejection(engine.error) else Validation.Accepted(after)
}

private fun rejection(error: ScoringError): Validation.Rejected =
    Validation.Rejected(
        if (error ==
            ScoringError.NOT_YOUR_TURN
        ) {
            RejectCode.NOT_YOUR_TURN
        } else {
            RejectCode.INVALID
        },
        error,
    )

private fun isStale(log: List<SetEvent>, event: SetEvent, baseSeq: Int): Boolean {
    if (baseSeq >= log.size) return false
    val since = log.drop(baseSeq.coerceAtLeast(0))
    return when (event) {
        is Undo -> true
        is Correct -> since.any { it.id == event.target || it.targets(event.target) }
        is Delete -> since.any { it.id == event.target || it.targets(event.target) }
        else -> false
    }
}

private fun SetEvent.targets(id: EventId): Boolean =
    when (this) {
        is Undo -> target == id
        is Correct -> target == id
        is Delete -> target == id
        else -> false
    }

/** Droits d'un téléphone joueur : ses propres lancers, et l'annulation ou la correction de son dernier lancer. */
private fun allowed(
    set: SetLog,
    before: ReplayResult,
    event: SetEvent,
    player: PlayerId?,
): Boolean {
    if (player == null) return false
    return when (event) {
        is Throw -> event.by == player
        is Fault -> event.by == player && event.kind != FaultKind.WRONG_PLAYER
        is WrongTeam -> event.player == player
        is Abandon, is AddPlayer, is RemovePlayer, is Reorder -> false
        is Undo -> event.target == lastThrowOf(set, before, player)
        is Correct -> event.target == lastThrowOf(set, before, player) && event.replacement.ownedBy(player)
        is Delete -> event.target == lastThrowOf(set, before, player)
    }
}

/** Compteur : lancers et fautes de tous, leurs annulations et corrections ; jamais la gestion des joueurs. */
private fun scorerAllowed(before: ReplayResult, event: SetEvent): Boolean {
    fun isThrow(e: SetEvent?): Boolean = e is Throw || e is Fault || e is WrongTeam
    val target =
        when (event) {
            is Undo -> event.target
            is Correct -> event.target
            is Delete -> event.target
            else -> null
        }
    val targeted = target?.let { id -> before.effectiveLog.firstOrNull { it.id == id } }
    return when (event) {
        is Throw, is Fault, is WrongTeam -> true
        is Abandon, is AddPlayer, is RemovePlayer, is Reorder -> false
        is Correct -> isThrow(targeted) && isThrow(event.replacement)
        is Undo, is Delete -> isThrow(targeted)
    }
}

/** Remplaçant d'une correction par un joueur : un lancer ou une faute, toujours à son nom. */
private fun PlayEvent.ownedBy(player: PlayerId): Boolean =
    when (this) {
        is Throw -> by == null || by == player
        is Fault -> by == null || by == player
        else -> false
    }

/** Dernier événement du journal effectif attribué à [player] (attribution par position). */
private fun lastThrowOf(set: SetLog, before: ReplayResult, player: PlayerId): EventId? {
    val initial = start(set.config, set.lineup).getOrThrow()
    return describe(initial, before.effectiveLog).lastOrNull { it.player == player }?.event?.id
}
