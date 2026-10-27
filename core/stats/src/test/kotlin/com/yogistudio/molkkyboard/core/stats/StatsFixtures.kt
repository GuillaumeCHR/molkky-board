package com.yogistudio.molkkyboard.core.stats

import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.scoring.Correct
import com.yogistudio.molkkyboard.core.scoring.EventId
import com.yogistudio.molkkyboard.core.scoring.PlayEvent
import com.yogistudio.molkkyboard.core.scoring.SetEvent
import com.yogistudio.molkkyboard.core.scoring.Throw
import com.yogistudio.molkkyboard.core.scoring.ThrowResult
import com.yogistudio.molkkyboard.core.scoring.getOrThrow
import com.yogistudio.molkkyboard.core.scoring.replay
import java.util.concurrent.atomic.AtomicInteger

private val ids = AtomicInteger()

fun nextId() = EventId("s${ids.incrementAndGet()}")

/** Un lancer en mode « Quilles » (aucune quille = raté). */
fun pins(vararg pins: Int): PlayEvent = Throw(nextId(), ThrowResult.Pins(pins.toList()))

/** Une équipe : identité = son nom, joueurs = les noms donnés (un seul = joueur solo). */
data class T(
    val name: String,
    val players: List<String> = listOf(name),
)

/** Partie d'une manche. Le journal est rejoué réellement par le moteur : les fixtures ne trichent pas. */
fun game(
    id: String,
    at: Long,
    teams: List<T>,
    log: List<SetEvent>,
    config: GameConfig = GameConfig.Classic,
    abandoned: Boolean = false,
): StatsGame {
    val lineup = teams.map { TeamLineup(TeamId(it.name), it.players.map(::PlayerId)) }
    return StatsGame(
        id = id,
        playedAt = at,
        config = config,
        teams = teams.map { StatsTeam(TeamId(it.name), memberKey = it.name, name = it.name) },
        sets = listOf(StatsSet(0, lineup, replay(config, lineup, log).getOrThrow())),
        abandoned = abandoned,
    )
}

/** A gagne en 5 lancers contre B (qui fait 1 à chaque fois) : la partie parfaite. */
fun perfectLog(): List<SetEvent> =
    listOf(pins(12), pins(1), pins(12), pins(1), pins(12), pins(1), pins(12), pins(1), pins(2))

/** La première équipe marque [first] (quille seule à chaque lancer), la seconde fait 1 entre chaque lancer. */
fun firstWins(vararg first: Int): List<SetEvent> =
    first.toList().flatMapIndexed { i, p -> if (i == first.lastIndex) listOf(pins(p)) else listOf(pins(p), pins(1)) }

/** Victoire en 9 lancers : 8 × 6 puis 2. */
fun slowLog(): List<SetEvent> = firstWins(6, 6, 6, 6, 6, 6, 6, 6, 2)

/** Correction après la fin : la manche sort des records. */
fun correctedAfterFinish(log: List<SetEvent>): List<SetEvent> {
    val first = log.first() as Throw
    return log + Correct(nextId(), first.id, Throw(nextId(), first.result))
}

fun summaries(vararg games: StatsGame): List<SetSummary> = games.flatMap(::summarize)
