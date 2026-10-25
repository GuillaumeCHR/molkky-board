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
 * @property effectiveLog événements de jeu effectivement appliqués, dans l'ordre (après Undo et Correct).
 * @property rejected événements ignorés de façon déterministe : tous les appareils convergent vers le même état.
 * @property corrected le journal contient au moins une correction acceptée (un Undo n'en est pas une).
 */
data class ReplayResult(
    val state: SetState,
    val effectiveLog: List<PlayEvent>,
    val rejected: List<Rejection>,
    val corrected: Boolean,
) {
    /** Seules les manches terminées normalement et jamais corrigées comptent pour les records. */
    val eligibleForRecords: Boolean get() = state.status == SetStatus.FINISHED && !corrected
}

/** Unique façon d'obtenir l'état d'une manche : on rejoue le journal brut depuis l'état initial. */
fun replay(config: GameConfig, teams: List<TeamLineup>, log: List<SetEvent>): Outcome<ReplayResult> =
    start(config, teams).flatMap { initial ->
        var state = initial
        val effective = mutableListOf<PlayEvent>()
        val rejected = mutableListOf<Rejection>()
        var corrected = false
        for (event in log) {
            val error: ScoringError? =
                when (event) {
                    is PlayEvent -> {
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
                    }

                    is Undo -> {
                        if (effective.lastOrNull()?.id == event.target) {
                            effective.removeAt(effective.lastIndex)
                            state = fold(initial, effective).getOrThrow()
                            null
                        } else {
                            ScoringError.NOTHING_TO_UNDO
                        }
                    }

                    is Correct -> {
                        val index = effective.indexOfFirst { it.id == event.target }
                        val replacement = event.replacement.withId(event.target)
                        val candidate = effective.toMutableList().also { if (index >= 0) it[index] = replacement }
                        val refolded = fold(initial, candidate)
                        when {
                            index < 0 -> {
                                ScoringError.UNKNOWN_EVENT
                            }

                            refolded is Outcome.Ok -> {
                                effective[index] = replacement
                                state = refolded.value
                                corrected = true
                                null
                            }

                            else -> {
                                ScoringError.CORRECTION_INVALIDATES_LOG
                            }
                        }
                    }
                }
            if (error != null) rejected += Rejection(event, error)
        }
        Outcome.Ok(ReplayResult(state, effective.toList(), rejected.toList(), corrected))
    }

/** Plie strictement une liste d'événements de jeu : la première erreur interrompt le pliage. */
fun fold(initial: SetState, events: List<PlayEvent>): Outcome<SetState> =
    events.fold<PlayEvent, Outcome<SetState>>(Outcome.Ok(initial)) { acc, event -> acc.flatMap { reduce(it, event) } }

/** Le remplaçant d'une correction prend l'identifiant de l'événement corrigé (pour qu'on puisse l'annuler ensuite). */
private fun PlayEvent.withId(id: EventId): PlayEvent =
    when (this) {
        is Throw -> copy(id = id)
        is Fault -> copy(id = id)
        is WrongTeam -> copy(id = id)
        is Abandon -> copy(id = id)
    }
