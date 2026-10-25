package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import java.util.concurrent.atomic.AtomicInteger

val A = TeamId("A")
val B = TeamId("B")
val C = TeamId("C")

val alice = PlayerId("Alice")
val arthur = PlayerId("Arthur")
val bruno = PlayerId("Bruno")
val bea = PlayerId("Béa")
val benoit = PlayerId("Benoît")
val chloe = PlayerId("Chloé")

/** A (Alice), B (Bruno), C (Chloé) : équipes d'un joueur, convention de `SCORING_SPEC.md` § 2. */
fun soloTeams(count: Int = 2): List<TeamLineup> =
    listOf(TeamLineup(A, listOf(alice)), TeamLineup(B, listOf(bruno)), TeamLineup(C, listOf(chloe))).take(count)

// Pas de vararg possible sur une value class : quelques surcharges suffisent.
fun lineup(team: TeamId) = TeamLineup(team, emptyList())

fun lineup(team: TeamId, p1: PlayerId) = TeamLineup(team, listOf(p1))

fun lineup(team: TeamId, p1: PlayerId, p2: PlayerId) = TeamLineup(team, listOf(p1, p2))

fun lineup(team: TeamId, p1: PlayerId, p2: PlayerId, p3: PlayerId) = TeamLineup(team, listOf(p1, p2, p3))

fun newSet(config: GameConfig = GameConfig.Classic, teams: List<TeamLineup> = soloTeams()): SetState =
    start(config, teams).getOrThrow()

private val ids = AtomicInteger()

fun nextId() = EventId("e${ids.incrementAndGet()}")

/** Fixe directement l'état d'une équipe (le moteur étant pur, on peut partir de n'importe quel état). */
fun SetState.given(
    team: TeamId,
    score: Int? = null,
    misses: Int? = null,
    eliminated: Boolean = false,
    finishedRank: Int? = null,
): SetState {
    val current = team(team)!!
    val updated =
        current.copy(
            score = score ?: current.score,
            misses = misses ?: current.misses,
            eliminated = eliminated,
            finishedRank = finishedRank,
        )
    return copy(
        teams = teams.map { if (it.id == team) updated else it },
        eliminationOrder = if (eliminated) eliminationOrder + team else eliminationOrder,
    )
}

fun SetState.turnOf(team: TeamId) = copy(turn = team)

fun SetState.score(team: TeamId) = team(team)!!.score

fun SetState.misses(team: TeamId) = team(team)!!.misses

fun pins(vararg pins: Int) = ThrowResult.Pins(pins.toList())

fun throwOf(vararg pins: Int, by: PlayerId? = null) = Throw(nextId(), pins(*pins), by)

fun wrongTeam(team: TeamId, player: PlayerId, vararg pins: Int) = WrongTeam(nextId(), team, player, pins(*pins))

/** Lancer du joueur courant ; `play()` sans quille = raté. */
fun SetState.play(vararg pins: Int): SetState = reduce(this, throwOf(*pins)).getOrThrow()

/** Contrôle de saisie d'un lancer annoncé par [player] (par défaut le prochain lanceur de [team]). */
fun SetState.tryThrow(
    team: TeamId,
    vararg pins: Int,
    player: PlayerId = team(team)?.nextPlayer ?: PlayerId("?"),
): ScoringError? =
    checkInput(this, throwOf(*pins, by = player))

fun SetState.fault(kind: FaultKind, by: PlayerId? = null): SetState =
    reduce(
        this,
        Fault(nextId(), kind, by),
    ).getOrThrow()

/** Construit un journal brut lancer par lancer, en lisant l'état courant obtenu par rejeu. */
class LogBuilder(
    val config: GameConfig = GameConfig.Classic,
    val teams: List<TeamLineup> = soloTeams(),
) {
    val events = mutableListOf<SetEvent>()

    fun result(): ReplayResult = replay(config, teams, events).getOrThrow()

    val state: SetState get() = result().state

    fun play(vararg pins: Int): Throw = throwOf(*pins).also { events += it }

    fun add(event: SetEvent): SetEvent = event.also { events += it }

    fun undoLast() = add(Undo(nextId(), result().effectiveLog.last().id))
}
