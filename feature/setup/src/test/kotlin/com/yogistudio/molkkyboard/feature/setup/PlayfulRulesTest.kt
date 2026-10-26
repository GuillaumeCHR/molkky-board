package com.yogistudio.molkkyboard.feature.setup

import com.yogistudio.molkkyboard.core.model.EndCondition
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.MissPenalty
import com.yogistudio.molkkyboard.core.model.Overshoot
import com.yogistudio.molkkyboard.core.model.RulesMode
import com.yogistudio.molkkyboard.core.model.mode
import org.junit.Assert.assertEquals
import org.junit.Test

/** Exceptions du mode Ludique (`RULES.md` § 8.1) vues par l'éditeur. */
class PlayfulRulesTest {
    @Test
    fun `aucune exception pour les regles officielles`() {
        assertEquals(emptyList<RuleException>(), GameConfig.Classic.exceptions)
    }

    @Test
    fun `exceptions par defaut du mode Ludique`() {
        assertEquals(
            listOf(RuleException.Miss(MissChoice.NONE), RuleException.FaultsIgnored),
            GameConfig.Playful.exceptions,
        )
    }

    @Test
    fun `chaque choix d'interface donne la regle attendue et se relit`() {
        MissChoice.entries.forEach { assertEquals(it, GameConfig.Classic.withMissChoice(it).missChoice) }
        OvershootChoice.entries.forEach { assertEquals(it, GameConfig.Classic.withOvershootChoice(it).overshootChoice) }
        assertEquals(MissPenalty.ResetScore(25), GameConfig.Classic.withMissChoice(MissChoice.RESET_HALF).onMissLimit)
        assertEquals(Overshoot.Bounce, GameConfig.Classic.withOvershootChoice(OvershootChoice.BOUNCE).overshoot)
    }

    @Test
    fun `les retours a la moitie suivent la cible`() {
        val config = GameConfig.Classic.withMissChoice(MissChoice.RESET_HALF).withTarget(40)
        assertEquals(Overshoot.ResetTo(20), config.overshoot)
        assertEquals(MissPenalty.ResetScore(20), config.onMissLimit)
        assertEquals(
            Overshoot.ResetTo(0),
            GameConfig.Classic
                .withOvershootChoice(OvershootChoice.RESET_ZERO)
                .withTarget(30)
                .overshoot,
        )
    }

    @Test
    fun `revenir a 50 redonne les regles officielles`() {
        val back = GameConfig.Classic.withTarget(40).withTarget(50)
        assertEquals(GameConfig.Classic, back)
        assertEquals(RulesMode.OFFICIAL, back.mode)
    }

    @Test
    fun `bornes de la cible et des rates`() {
        assertEquals(PlayfulRules.MIN_TARGET, GameConfig.Classic.withTarget(0).targetScore)
        assertEquals(PlayfulRules.MAX_TARGET, GameConfig.Classic.withTarget(500).targetScore)
        assertEquals(PlayfulRules.MIN_MISS_LIMIT, GameConfig.Classic.withMissLimit(1).missLimit)
        assertEquals(PlayfulRules.MAX_MISS_LIMIT, GameConfig.Classic.withMissLimit(9).missLimit)
    }

    @Test
    fun `toutes les exceptions dans l'ordre de l'editeur`() {
        val config =
            GameConfig.Classic
                .withMissChoice(MissChoice.RESET_ZERO)
                .withMissLimit(4)
                .withOvershootChoice(OvershootChoice.STAY)
                .withTarget(60)
                .copy(faultsCountAsMiss = false, endCondition = EndCondition.PlayForPlaces, catchUpReset = true)
        assertEquals(
            listOf(
                RuleException.Miss(MissChoice.RESET_ZERO),
                RuleException.MissLimit(4),
                RuleException.Overshoot(OvershootChoice.STAY),
                RuleException.Target(60),
                RuleException.FaultsIgnored,
                RuleException.PlayForPlaces,
                RuleException.CatchUpReset,
            ),
            config.exceptions,
        )
    }
}
