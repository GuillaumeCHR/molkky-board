package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.EndCondition
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.MissPenalty
import com.yogistudio.molkkyboard.core.model.Overshoot
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.model.isExemptFromMissLimit
import com.yogistudio.molkkyboard.core.model.startScoreFor
import com.yogistudio.molkkyboard.core.model.targetFor

private const val RULE_37_THRESHOLD = 37
private const val RULE_37_RESET = 25

/** Crée l'état initial d'une manche ; l'ordre de [teams] est l'ordre de passage. */
fun start(config: GameConfig, teams: List<TeamLineup>): Outcome<SetState> {
    val allPlayers = teams.flatMap { it.players }
    val error =
        when {
            !config.isValidFor(teams.map { it.id }) -> ScoringError.INVALID_CONFIG
            teams.size < 2 -> ScoringError.NOT_ENOUGH_TEAMS
            teams.any { it.players.isEmpty() } -> ScoringError.EMPTY_TEAM
            teams.map { it.id }.toSet().size != teams.size -> ScoringError.DUPLICATE_TEAM
            allPlayers.toSet().size != allPlayers.size -> ScoringError.PLAYER_IN_MULTIPLE_TEAMS
            else -> null
        }
    if (error != null) return Outcome.Err(error)
    val states = teams.map { TeamState(id = it.id, players = it.players, score = config.startScoreFor(it.id)) }
    return Outcome.Ok(SetState(config = config, teams = states, turn = states.first().id))
}

private fun GameConfig.isValidFor(teams: List<TeamId>): Boolean {
    val overshoot = overshoot
    val penalty = onMissLimit
    return missLimit >= 1 &&
        teamOverrides.keys.all { it in teams } &&
        teams.all { team ->
            val target = targetFor(team)
            target >= 1 &&
                startScoreFor(team) in 0 until target &&
                (overshoot !is Overshoot.ResetTo || overshoot.score in 0 until target) &&
                (penalty !is MissPenalty.ResetScore || penalty.score in 0 until target)
        }
}

/** Applique un événement de jeu. Fonction pure : l'état reçu n'est jamais modifié. */
fun reduce(state: SetState, event: PlayEvent): Outcome<SetState> {
    if (state.status != SetStatus.IN_PROGRESS) return Outcome.Err(ScoringError.SET_FINISHED)
    return when (event) {
        is Throw -> applyThrow(state, event)
        is Fault -> applyFault(state, event)
        is WrongTeam -> applyWrongTeam(state, event)
        is Abandon -> Outcome.Ok(state.copy(status = SetStatus.ABANDONED, turn = null))
    }
}

private fun applyThrow(state: SetState, event: Throw): Outcome<SetState> {
    val error =
        checkThrower(state, event.team, event.player, anyTeammate = false)
            ?: validatePins(event.pins) { it in ALL_PINS }
    if (error != null) return Outcome.Err(error)
    val team = state.team(event.team)!!.advancePlayer()
    if (event.pins.isEmpty()) return Outcome.Ok(state.withMiss(team).nextTurn())

    val updated = team.copy(score = state.scoreAfter(team, points(event.pins)), misses = 0)
    val next = state.replace(updated)
    return Outcome.Ok(
        when {
            updated.score == state.config.targetFor(updated.id) -> next.reachTarget(updated.id)
            state.config.catchUpReset -> next.applyCatchUp(updated).nextTurn()
            else -> next.nextTurn()
        },
    )
}

/** Nouveau score après un lancer réussi, dépassement de la cible compris. */
private fun SetState.scoreAfter(team: TeamState, points: Int): Int {
    val target = config.targetFor(team.id)
    val raw = team.score + points
    if (raw <= target) return raw
    return when (val overshoot = config.overshoot) {
        is Overshoot.ResetTo -> overshoot.score
        Overshoot.Stay -> team.score
        Overshoot.Bounce -> (target - (raw - target)).coerceAtLeast(0)
    }
}

private fun applyFault(state: SetState, event: Fault): Outcome<SetState> {
    val anyTeammate = event.kind == FaultKind.WRONG_PLAYER
    checkThrower(state, event.team, event.player, anyTeammate)?.let { return Outcome.Err(it) }
    // Mauvais joueur : on avance comme si le joueur attendu avait lancé.
    val penalized = state.team(event.team)!!.advancePlayer().applyRule37(state.config, event.kind.triggersRule37)
    return Outcome.Ok(state.withFault(penalized).nextTurn())
}

private fun applyWrongTeam(state: SetState, event: WrongTeam): Outcome<SetState> {
    val team = state.team(event.team) ?: return Outcome.Err(ScoringError.UNKNOWN_TEAM)
    if (event.team == state.turn || !team.active) return Outcome.Err(ScoringError.INVALID_EVENT)
    if (event.player !in team.players) return Outcome.Err(ScoringError.UNKNOWN_PLAYER)
    validatePins(event.pins) { it in ALL_PINS }?.let { return Outcome.Err(it) }
    // Points annulés, règle des 37 appliquée tout de suite, lancer perdu au prochain tour régulier.
    val updated = team.applyRule37(state.config, triggers = true).copy(pendingForfeit = team.pendingForfeit + 1)
    return Outcome.Ok(state.replace(updated))
}

/** Vérifie que [player] de [team] peut lancer maintenant ; [anyTeammate] accepte n'importe quel coéquipier. */
private fun checkThrower(state: SetState, team: TeamId, player: PlayerId, anyTeammate: Boolean): ScoringError? {
    val current = state.team(team)
    return when {
        current == null -> ScoringError.UNKNOWN_TEAM
        state.turn != team -> ScoringError.NOT_YOUR_TURN
        player !in current.players -> ScoringError.UNKNOWN_PLAYER
        !anyTeammate && player != current.nextPlayer -> ScoringError.NOT_YOUR_TURN
        else -> null
    }
}

private fun TeamState.applyRule37(config: GameConfig, triggers: Boolean): TeamState =
    if (config.rule37 && triggers && score >= RULE_37_THRESHOLD) copy(score = RULE_37_RESET) else this

private fun SetState.replace(team: TeamState): SetState = copy(teams = teams.map { if (it.id == team.id) team else it })

/** Une faute est un raté, ou un simple lancer perdu si `faultsCountAsMiss` est désactivé. */
private fun SetState.withFault(
    team: TeamState,
): SetState = if (config.faultsCountAsMiss) withMiss(team) else replace(team)

/** Ajoute un raté à [team] (dont le joueur a déjà avancé) et applique la sanction éventuelle. */
private fun SetState.withMiss(team: TeamState): SetState {
    val misses = team.misses + 1
    if (misses < config.missLimit || config.isExemptFromMissLimit(team.id)) return replace(team.copy(misses = misses))
    return when (val penalty = config.onMissLimit) {
        MissPenalty.Eliminate -> eliminate(team.copy(misses = misses))
        is MissPenalty.ResetScore -> replace(team.copy(score = penalty.score, misses = 0))
        MissPenalty.Nothing -> replace(team.copy(misses = misses))
    }
}

private fun SetState.eliminate(team: TeamState): SetState {
    val next = replace(team.copy(eliminated = true)).copy(eliminationOrder = eliminationOrder + team.id)
    return next.finishIfOneLeft()
}

/** L'équipe [id] vient d'atteindre sa cible. */
private fun SetState.reachTarget(id: TeamId): SetState =
    when (config.endCondition) {
        EndCondition.FirstToTarget -> {
            finish(listOf(id))
        }

        EndCondition.PlayForPlaces -> {
            val rank = teams.count { it.finishedRank != null } + 1
            replace(team(id)!!.copy(finishedRank = rank)).finishIfOneLeft().nextTurn()
        }
    }

/** S'il ne reste qu'une équipe en lice, elle prend la place suivante et la manche se termine. */
private fun SetState.finishIfOneLeft(): SetState {
    val active = teams.filter { it.active }
    if (active.size > 1) return this
    val placed = teams.filter { it.finishedRank != null }.sortedBy { it.finishedRank }.map { it.id }
    return finish(placed + active.map { it.id })
}

/**
 * Termine la manche. Classement : [leaders], puis les équipes restantes par score décroissant (à égalité,
 * ordre de passage), puis les éliminées de la dernière à la première éliminée.
 */
private fun SetState.finish(leaders: List<TeamId>): SetState {
    val others =
        teams
            .filter { it.id !in leaders && !it.eliminated }
            .sortedByDescending { it.score }
            .map { it.id }
    return copy(
        status = SetStatus.FINISHED,
        turn = null,
        ranking = leaders + others + eliminationOrder.reversed(),
    )
}

/** Variante : rejoindre exactement le score d'une autre équipe la fait redescendre à la moitié de la cible. */
private fun SetState.applyCatchUp(scorer: TeamState): SetState =
    copy(
        teams =
            teams.map { other ->
                val reset = config.targetFor(other.id) / 2
                val caughtUp = other.id != scorer.id && other.active && other.score == scorer.score
                if (caughtUp && other.score > reset) other.copy(score = reset) else other
            },
    )

/**
 * Passe à la prochaine équipe en lice. Une équipe qui doit un lancer perdu (faute « mauvaise équipe »)
 * le perd automatiquement à son tour : ces lancers dérivés ne sont jamais stockés dans le journal.
 */
private fun SetState.nextTurn(): SetState {
    if (status != SetStatus.IN_PROGRESS) return this
    val from = teams.indexOfFirst { it.id == turn }
    val next = (1..teams.size).map { teams[(from + it) % teams.size] }.first { it.active }
    val moved = copy(turn = next.id)
    if (next.pendingForfeit == 0) return moved
    val forfeited = next.advancePlayer().copy(pendingForfeit = next.pendingForfeit - 1)
    return moved.withFault(forfeited).nextTurn()
}
