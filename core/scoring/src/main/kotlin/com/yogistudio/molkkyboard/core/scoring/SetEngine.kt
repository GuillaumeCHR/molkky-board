package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.EndCondition
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.MissPenalty
import com.yogistudio.molkkyboard.core.model.Overshoot
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.model.startScoreFor
import com.yogistudio.molkkyboard.core.model.targetFor

/** Crée l'état initial d'une manche ; l'ordre de [teams] est l'ordre de passage. */
fun start(config: GameConfig, teams: List<TeamLineup>, rules: ScoringRules = StandardRules): Outcome<SetState> {
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
    return Outcome.Ok(SetState(config = config, teams = states, turn = states.first().id, rules = rules))
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

/**
 * Contrôle de saisie (avant d'ajouter un événement au journal) : en plus des règles de [reduce], le joueur qui
 * saisit un lancer ou une faute ([Throw.by], [Fault.by]) doit être le lanceur attendu. Au rejeu, ce contrôle
 * n'est pas fait : un lancer appartient au lanceur de sa position dans le journal.
 */
fun checkInput(state: SetState, event: PlayEvent): ScoringError? {
    val by =
        when (event) {
            is Throw -> event.by
            is Fault -> event.by.takeIf { event.kind != FaultKind.WRONG_PLAYER }
            else -> null
        }
    val current = state.currentTeam
    val error =
        when {
            by == null || current == null -> null
            state.teams.none { by in it.players } -> ScoringError.UNKNOWN_PLAYER
            by != current.nextPlayer -> ScoringError.NOT_YOUR_TURN
            else -> null
        }
    return error ?: reduce(state, event).errorOrNull()
}

/** Applique un événement de jeu. Fonction pure : l'état reçu n'est jamais modifié. */
fun reduce(state: SetState, event: PlayEvent): Outcome<SetState> {
    if (state.status != SetStatus.IN_PROGRESS) return Outcome.Err(ScoringError.SET_FINISHED)
    return when (event) {
        is Throw -> applyThrow(state, event)
        is Fault -> applyFault(state, event)
        is WrongTeam -> applyWrongTeam(state, event)
        is Abandon -> Outcome.Ok(state.copy(status = SetStatus.ABANDONED, turn = null))
        is AddPlayer -> applyAddPlayer(state, event)
        is RemovePlayer -> applyRemovePlayer(state, event)
        is Reorder -> applyReorder(state, event)
    }
}

private fun applyThrow(state: SetState, event: Throw): Outcome<SetState> {
    event.result.validate()?.let { return Outcome.Err(it) }
    val team = state.currentTeam!!.consumeTurn()
    val points = event.result.points
    if (points == 0) return Outcome.Ok(state.withMiss(team).nextTurn())

    val updated = team.copy(score = state.rules.scoreAfterThrow(state.config, team, points), misses = 0)
    val next = state.replace(updated)
    return Outcome.Ok(
        if (state.rules.hasWon(state.config, updated)) {
            next.reachTarget(updated.id)
        } else {
            next.afterOpponentScored(updated).nextTurn()
        },
    )
}

private fun applyFault(state: SetState, event: Fault): Outcome<SetState> {
    val current = state.currentTeam!!
    if (event.kind == FaultKind.WRONG_PLAYER) {
        // Mauvais joueur : le fautif doit être un coéquipier ; on avance comme si le bon joueur avait lancé.
        val by = event.by ?: return Outcome.Err(ScoringError.INVALID_EVENT)
        if (by !in current.players) return Outcome.Err(ScoringError.UNKNOWN_PLAYER)
    }
    val penalized = state.rules.afterFault(state.config, current.consumeTurn(), event.kind)
    return Outcome.Ok(state.withFault(penalized).nextTurn())
}

private fun applyWrongTeam(state: SetState, event: WrongTeam): Outcome<SetState> {
    val team = state.team(event.team) ?: return Outcome.Err(ScoringError.UNKNOWN_TEAM)
    val error =
        when {
            event.team == state.turn || !team.active -> ScoringError.INVALID_EVENT
            event.player !in team.players -> ScoringError.UNKNOWN_PLAYER
            else -> event.result.validate()
        }
    if (error != null) return Outcome.Err(error)
    // Points annulés, règle des 37 appliquée tout de suite, lancer perdu au prochain tour régulier.
    val updated = state.rules.afterFault(state.config, team, kind = null).copy(pendingForfeit = team.pendingForfeit + 1)
    return Outcome.Ok(state.replace(updated))
}

private fun applyAddPlayer(state: SetState, event: AddPlayer): Outcome<SetState> {
    if (state.teams.any { event.player in it.players }) return Outcome.Err(ScoringError.PLAYER_IN_MULTIPLE_TEAMS)
    val existing = state.team(event.team)
    if (existing != null) {
        if (existing.eliminated || existing.finishedRank != null) return Outcome.Err(ScoringError.INVALID_EVENT)
        val turns = if (existing.left) state.teams.filter { it.active }.minOf { it.turns } else existing.turns
        val rejoined = existing.copy(players = existing.players + event.player, left = false, turns = turns)
        return Outcome.Ok(state.replace(rejoined).copy(departureOrder = state.departureOrder - existing.id))
    }
    // Nouvelle équipe en fin d'ordre : elle lancera à la fin du tour en cours.
    val turns = state.teams.filter { it.active }.minOf { it.turns }
    val newcomer =
        TeamState(
            id = event.team,
            players = listOf(event.player),
            score = state.config.startScoreFor(event.team).coerceIn(0, state.config.targetFor(event.team) - 1),
            turns = turns,
        )
    return Outcome.Ok(state.copy(teams = state.teams + newcomer))
}

private fun applyRemovePlayer(state: SetState, event: RemovePlayer): Outcome<SetState> {
    val team = state.teams.firstOrNull { event.player in it.players } ?: return Outcome.Err(ScoringError.UNKNOWN_PLAYER)
    // Une équipe éliminée ou déjà classée garde sa place : un départ n'y change rien.
    if (!team.active) return Outcome.Err(ScoringError.INVALID_EVENT)
    val index = team.players.indexOf(event.player)
    val players = team.players - event.player
    if (players.isNotEmpty()) {
        // Le prochain lanceur reste le même, sauf si c'est lui qui part : on passe au suivant de la rotation.
        val next =
            when {
                index < team.nextPlayerIdx -> team.nextPlayerIdx - 1
                else -> team.nextPlayerIdx % players.size
            }
        return Outcome.Ok(state.replace(team.copy(players = players, nextPlayerIdx = next)))
    }
    // Plus personne : l'équipe est partie, classée en dernier ; si c'était son tour, il passe.
    val departed =
        state
            .replace(team.copy(players = players, nextPlayerIdx = 0, left = true))
            .copy(departureOrder = state.departureOrder + team.id)
            .finishIfOneLeft()
    return Outcome.Ok(if (state.turn == team.id) departed.nextTurn() else departed)
}

private fun applyReorder(state: SetState, event: Reorder): Outcome<SetState> {
    val sameTeams = event.teams.size == state.teams.size && event.teams.toSet() == state.teams.map { it.id }.toSet()
    val samePlayers =
        event.players.all { (id, players) ->
            val team = state.team(id)
            team != null && players.size == team.players.size && players.toSet() == team.players.toSet()
        }
    val error =
        when {
            !sameTeams || !samePlayers -> ScoringError.INVALID_EVENT
            !state.betweenRounds -> ScoringError.NOT_BETWEEN_ROUNDS
            else -> null
        }
    if (error != null) return Outcome.Err(error)
    val reordered =
        event.teams.map { id ->
            val team = state.team(id)!!
            event.players[id]?.let { team.copy(players = it, nextPlayerIdx = 0) } ?: team
        }
    return Outcome.Ok(state.copy(teams = reordered, turn = reordered.first { it.active }.id))
}

private fun SetState.replace(team: TeamState): SetState = copy(teams = teams.map { if (it.id == team.id) team else it })

/** Une faute est un raté, ou un simple lancer perdu si `faultsCountAsMiss` est désactivé. */
private fun SetState.withFault(
    team: TeamState,
): SetState = if (config.faultsCountAsMiss) withMiss(team) else replace(team)

/** Ajoute un raté à [team] (dont le tour est déjà consommé) et applique la sanction éventuelle. */
private fun SetState.withMiss(team: TeamState): SetState {
    val updated = rules.afterMiss(config, team)
    if (!updated.eliminated) return replace(updated)
    return replace(updated).copy(eliminationOrder = eliminationOrder + team.id).finishIfOneLeft()
}

private fun SetState.afterOpponentScored(scorer: TeamState): SetState =
    copy(
        teams =
            teams.map {
                if (it.id != scorer.id &&
                    it.active
                ) {
                    rules.afterOpponentScored(config, scorer, it)
                } else {
                    it
                }
            },
    )

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
 * ordre de passage), puis les éliminées (de la dernière à la première), puis les parties (idem).
 */
private fun SetState.finish(leaders: List<TeamId>): SetState {
    val others =
        teams
            .filter { it.id !in leaders && !it.eliminated && !it.left }
            .sortedByDescending { it.score }
            .map { it.id }
    return copy(
        status = SetStatus.FINISHED,
        turn = null,
        ranking = leaders + others + eliminationOrder.reversed() + departureOrder.reversed(),
    )
}

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
    val forfeited = next.consumeTurn().copy(pendingForfeit = next.pendingForfeit - 1)
    return moved.withFault(forfeited).nextTurn()
}
