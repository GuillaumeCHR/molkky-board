package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.MissPenalty
import com.yogistudio.molkkyboard.core.model.Overshoot
import com.yogistudio.molkkyboard.core.model.isExemptFromMissLimit
import com.yogistudio.molkkyboard.core.model.targetFor

/**
 * Stratégie de règles injectée dans le moteur. Le moteur gère le journal, l'ordre de passage, les joueurs
 * et le classement ; la stratégie décide de l'effet de chaque lancer sur les scores. Les futures variantes
 * de jeu (« Killer », mort subite…) seront d'autres implémentations, sur le même journal.
 */
interface ScoringRules {
    /** Score après un lancer réussi de [points] (dépassement de la cible compris). */
    fun scoreAfterThrow(config: GameConfig, team: TeamState, points: Int): Int

    /** L'équipe après un raté : compteur et sanction éventuelle (`eliminated = true` si elle est éliminée). */
    fun afterMiss(config: GameConfig, team: TeamState): TeamState

    /** Effet d'une faute sur le score, avant le raté qu'elle compte (règle des 37). */
    fun afterFault(config: GameConfig, team: TeamState, kind: FaultKind?): TeamState

    /** Effet du lancer réussi de [scorer] sur une autre équipe en lice ([other]). */
    fun afterOpponentScored(config: GameConfig, scorer: TeamState, other: TeamState): TeamState

    /** L'équipe a gagné (règles standard : exactement la cible). */
    fun hasWon(config: GameConfig, team: TeamState): Boolean
}

/** Règles du Mölkky paramétrées par [GameConfig] (préréglages « Classique » et « Tournoi IMO », variantes). */
object StandardRules : ScoringRules {
    private const val RULE_37_THRESHOLD = 37
    private const val RULE_37_RESET = 25

    override fun scoreAfterThrow(config: GameConfig, team: TeamState, points: Int): Int {
        val target = config.targetFor(team.id)
        val raw = team.score + points
        if (raw <= target) return raw
        return when (val overshoot = config.overshoot) {
            is Overshoot.ResetTo -> overshoot.score
            Overshoot.Stay -> team.score
            Overshoot.Bounce -> (target - (raw - target)).coerceAtLeast(0)
        }
    }

    override fun afterMiss(config: GameConfig, team: TeamState): TeamState {
        val misses = team.misses + 1
        if (misses < config.missLimit || config.isExemptFromMissLimit(team.id)) return team.copy(misses = misses)
        return when (val penalty = config.onMissLimit) {
            MissPenalty.Eliminate -> team.copy(misses = misses, eliminated = true)
            is MissPenalty.ResetScore -> team.copy(score = penalty.score, misses = 0)
            MissPenalty.Nothing -> team.copy(misses = misses)
        }
    }

    /** Règle des 37 : mordu, mauvais joueur ou mauvaise équipe ([kind] nul) à 37 points ou plus → 25. */
    override fun afterFault(config: GameConfig, team: TeamState, kind: FaultKind?): TeamState {
        val triggers = kind?.triggersRule37 ?: true
        return if (config.rule37 && triggers &&
            team.score >= RULE_37_THRESHOLD
        ) {
            team.copy(score = RULE_37_RESET)
        } else {
            team
        }
    }

    override fun hasWon(config: GameConfig, team: TeamState): Boolean = team.score == config.targetFor(team.id)

    /** Variante « rejoindre un score » : l'équipe rejointe redescend à la moitié de sa cible. */
    override fun afterOpponentScored(config: GameConfig, scorer: TeamState, other: TeamState): TeamState {
        if (!config.catchUpReset) return other
        val reset = config.targetFor(other.id) / 2
        return if (other.score == scorer.score && other.score > reset) other.copy(score = reset) else other
    }
}
