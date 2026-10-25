package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.TeamLineup

/** Événement du journal brut ignoré au rejeu, et pourquoi. */
data class Rejection(
    val event: SetEvent,
    val error: ScoringError,
)

/**
 * Résultat du rejeu d'un journal brut.
 *
 * @property effectiveLog événements de jeu effectivement appliqués, dans l'ordre (après Undo, Correct et Delete).
 * @property rejected événements ignorés de façon déterministe : tous les appareils convergent vers le même état.
 * @property hasCorrections au moins une correction ou suppression acceptée (simple mention dans l'historique).
 * @property modifiedAfterFinish la manche a été modifiée alors qu'elle était terminée (hors annulation immédiate
 * du lancer gagnant) : elle sort des records.
 */
data class ReplayResult(
    val state: SetState,
    val effectiveLog: List<PlayEvent>,
    val rejected: List<Rejection>,
    val hasCorrections: Boolean,
    val modifiedAfterFinish: Boolean,
) {
    /** Seules les manches terminées normalement, et jamais modifiées après leur fin, comptent pour les records. */
    val eligibleForRecords: Boolean get() = state.status == SetStatus.FINISHED && !modifiedAfterFinish
}

/** Unique façon d'obtenir l'état d'une manche : on rejoue le journal brut depuis l'état initial. */
fun replay(
    config: GameConfig,
    teams: List<TeamLineup>,
    log: List<SetEvent>,
    rules: ScoringRules = StandardRules,
): Outcome<ReplayResult> = start(config, teams, rules).flatMap { initial -> Outcome.Ok(Replayer(initial).run(log)) }

/** Plie strictement une liste d'événements de jeu : la première erreur interrompt le pliage. */
fun fold(initial: SetState, events: List<PlayEvent>): Outcome<SetState> =
    events.fold<PlayEvent, Outcome<SetState>>(Outcome.Ok(initial)) { acc, event -> acc.flatMap { reduce(it, event) } }

private class Replayer(
    private val initial: SetState,
) {
    private var state = initial
    private val effective = mutableListOf<PlayEvent>()
    private val rejected = mutableListOf<Rejection>()
    private var hasCorrections = false
    private var modifiedAfterFinish = false

    fun run(log: List<SetEvent>): ReplayResult {
        log.forEachIndexed { index, event ->
            val finishedBefore = state.status == SetStatus.FINISHED
            val error =
                when (event) {
                    is PlayEvent -> play(event)
                    is Undo -> undo(event)
                    is Correct -> rewrite(event.target) { list, i -> list[i] = event.replacement.withId(event.target) }
                    is Delete -> rewrite(event.target) { list, i -> list.removeAt(i) }
                }
            if (error != null) {
                rejected += Rejection(event, error)
            } else if (finishedBefore) {
                // Seuls Undo, Correct et Delete peuvent être acceptés sur une manche terminée.
                // Undo immédiat du lancer gagnant : simple rectification de saisie, la manche reste éligible.
                val tolerated = event is Undo && log[index - 1].id == event.target
                if (!tolerated) modifiedAfterFinish = true
            }
        }
        return ReplayResult(state, effective.toList(), rejected.toList(), hasCorrections, modifiedAfterFinish)
    }

    private fun play(event: PlayEvent): ScoringError? =
        when (val outcome = reduce(state, event)) {
            is Outcome.Ok -> {
                state = outcome.value
                effective += event
                null
            }

            is Outcome.Err -> {
                outcome.error
            }
        }

    private fun undo(event: Undo): ScoringError? {
        if (effective.lastOrNull()?.id != event.target) return ScoringError.NOTHING_TO_UNDO
        effective.removeAt(effective.lastIndex)
        state = fold(initial, effective).getOrThrow()
        return null
    }

    /** Correction ou suppression : on modifie une copie du journal effectif et on la rejoue entièrement. */
    private fun rewrite(target: EventId, change: (MutableList<PlayEvent>, Int) -> Unit): ScoringError? {
        val index = effective.indexOfFirst { it.id == target }
        if (index < 0) return ScoringError.UNKNOWN_EVENT
        val candidate = effective.toMutableList().also { change(it, index) }
        return when (val refolded = fold(initial, candidate)) {
            is Outcome.Ok -> {
                effective.clear()
                effective += candidate
                state = refolded.value
                hasCorrections = true
                null
            }

            is Outcome.Err -> {
                ScoringError.CORRECTION_INVALIDATES_LOG
            }
        }
    }
}

/** Le remplaçant d'une correction prend l'identifiant de l'événement corrigé (pour qu'on puisse l'annuler ensuite). */
private fun PlayEvent.withId(id: EventId): PlayEvent =
    when (this) {
        is Throw -> copy(id = id)
        is Fault -> copy(id = id)
        is WrongTeam -> copy(id = id)
        is Abandon -> copy(id = id)
        is AddPlayer -> copy(id = id)
        is RemovePlayer -> copy(id = id)
        is Reorder -> copy(id = id)
    }
