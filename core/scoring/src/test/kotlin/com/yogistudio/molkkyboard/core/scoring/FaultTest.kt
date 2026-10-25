package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.GameConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/** `SCORING_SPEC.md` § 7. */
class FaultTest {
    private val imo = GameConfig.ImoTournament

    @Test
    fun `F1 un mordu est un rate`() {
        val s = newSet().given(A, score = 30).fault(FaultKind.TRESPASS)
        assertEquals(30, s.score(A))
        assertEquals(1, s.misses(A))
        assertEquals(B, s.turn)
    }

    @Test
    fun `F2 regle des 37 a 40 points`() {
        val s = newSet(imo).given(A, score = 40).fault(FaultKind.TRESPASS)
        assertEquals(25, s.score(A))
        assertEquals(1, s.misses(A))
    }

    @Test
    fun `F3 regle des 37 seuil inclus`() =
        assertEquals(
            25,
            newSet(imo).given(A, score = 37).fault(FaultKind.TRESPASS).score(A),
        )

    @Test
    fun `F4 pas de regle des 37 sous le seuil`() {
        val s = newSet(imo).given(A, score = 36).fault(FaultKind.TRESPASS)
        assertEquals(36, s.score(A))
        assertEquals(1, s.misses(A))
    }

    @Test
    fun `F5 regle des 37 puis elimination`() {
        val s = newSet(imo).given(A, score = 40, misses = 2).fault(FaultKind.TRESPASS)
        assertEquals(25, s.score(A))
        assertEquals(3, s.misses(A))
        assertTrue(s.team(A)!!.eliminated)
    }

    @ParameterizedTest
    @EnumSource(FaultKind::class, names = ["DROPPED", "TIMEOUT", "ABSENT"])
    fun `F6 F7 fautes sans regle des 37`(kind: FaultKind) {
        val s = newSet(imo).given(A, score = 40).fault(kind)
        assertEquals(40, s.score(A))
        assertEquals(1, s.misses(A))
    }

    @Test
    fun `F8 mauvais joueur l'ordre continue comme si le bon joueur avait lance`() {
        val s =
            newSet(imo, listOf(lineup(A, alice, arthur), lineup(B, bruno)))
                .given(A, score = 40)
                .fault(FaultKind.WRONG_PLAYER, player = arthur)
        assertEquals(25, s.score(A))
        assertEquals(1, s.misses(A))
        assertEquals(arthur, s.team(A)!!.nextPlayer)
    }

    @Test
    fun `F9 faute sans rate lancer perdu compteur inchange`() {
        val s = newSet(GameConfig(faultsCountAsMiss = false)).given(A, score = 30, misses = 1).fault(FaultKind.TRESPASS)
        assertEquals(30, s.score(A))
        assertEquals(1, s.misses(A))
        assertEquals(B, s.turn)
    }

    private fun wrongTeamByB(config: GameConfig): SetState {
        val s = newSet(config, listOf(lineup(A, alice), lineup(B, bruno, bea))).given(B, score = 40)
        return reduce(s, WrongTeam(nextId(), B, bruno, listOf(5))).getOrThrow()
    }

    @Test
    fun `F10 mauvaise equipe points annules regle des 37 et lancer perdu en attente`() {
        val s = wrongTeamByB(imo)
        assertEquals(25, s.score(B))
        assertEquals(0, s.misses(B))
        assertEquals(1, s.team(B)!!.pendingForfeit)
        assertEquals(A, s.turn)
        assertEquals(bruno, s.team(B)!!.nextPlayer)
    }

    @Test
    fun `F11 le lancer perdu est applique automatiquement au tour suivant de l'equipe fautive`() {
        val s = wrongTeamByB(imo).play(3)
        assertEquals(3, s.score(A))
        assertEquals(25, s.score(B))
        assertEquals(1, s.misses(B))
        assertEquals(0, s.team(B)!!.pendingForfeit)
        assertEquals(bea, s.team(B)!!.nextPlayer)
        assertEquals(A, s.turn)
    }

    @Test
    fun `F12 mauvaise equipe en mode classique pas de regle des 37`() {
        val s = wrongTeamByB(GameConfig.Classic)
        assertEquals(40, s.score(B))
        assertEquals(1, s.team(B)!!.pendingForfeit)
    }

    @Test
    fun `un lancer perdu peut eliminer et terminer la manche`() {
        val s = newSet().given(B, misses = 2)
        val afterWrongTeam = reduce(s, WrongTeam(nextId(), B, bruno, emptyList())).getOrThrow()
        val end = afterWrongTeam.play(4)
        assertEquals(SetStatus.FINISHED, end.status)
        assertEquals(listOf(A, B), end.ranking)
    }

    @Test
    fun `fautes invalides`() {
        val s = newSet(teams = listOf(lineup(A, alice, arthur), lineup(B, bruno)))

        fun err(event: PlayEvent) = reduce(s, event).errorOrNull()
        assertEquals(ScoringError.NOT_YOUR_TURN, err(Fault(nextId(), A, arthur, FaultKind.TRESPASS)))
        assertEquals(ScoringError.UNKNOWN_PLAYER, err(Fault(nextId(), A, bruno, FaultKind.TRESPASS)))
        assertEquals(ScoringError.NOT_YOUR_TURN, err(Fault(nextId(), B, bruno, FaultKind.TRESPASS)))
        assertEquals(ScoringError.INVALID_EVENT, err(WrongTeam(nextId(), A, alice, listOf(1))))
        assertEquals(ScoringError.UNKNOWN_TEAM, err(WrongTeam(nextId(), C, chloe, listOf(1))))
        assertEquals(ScoringError.UNKNOWN_PLAYER, err(WrongTeam(nextId(), B, alice, listOf(1))))
        assertEquals(ScoringError.INVALID_PIN, err(WrongTeam(nextId(), B, bruno, listOf(13))))
    }
}
