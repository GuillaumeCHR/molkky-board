package fr.molkky.core.scoring

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/** `SCORING_SPEC.md` § 4. */
class OvershootTest {
    @Test
    fun `O1 exactement 50 termine la manche`() {
        val s = newSet().given(A, score = 45).play(5)
        assertEquals(50, s.score(A))
        assertEquals(SetStatus.FINISHED, s.status)
        assertEquals(listOf(A, B), s.ranking)
        assertNull(s.turn)
    }

    @ParameterizedTest(name = "{0} : {1} + {2} -> {3}")
    @CsvSource(
        "O2 51 redescend a 25, 45, 6, 25",
        "O3 deux quilles valent 2, 45, 6 7, 47",
        "O4 52 redescend a 25, 40, 12, 25",
        "O5 51 par plusieurs quilles, 49, 2 3, 25",
        "O10 atteindre 25 normalement, 20, 5, 25",
    )
    fun `depassement de la cible`(case: String, before: Int, pins: String, after: Int) {
        val s = newSet().given(A, score = before).play(*pins.split(" ").map(String::toInt).toIntArray())
        assertEquals(after, s.score(A), case)
        assertEquals(SetStatus.IN_PROGRESS, s.status)
        assertEquals(B, s.turn)
    }

    @Test
    fun `O6 50 atteint avec plusieurs quilles`() {
        val s = newSet().given(A, score = 48).play(1, 2)
        assertEquals(50, s.score(A))
        assertEquals(SetStatus.FINISHED, s.status)
    }

    @Test
    fun `O7 38 plus la 12 gagne`() = assertEquals(SetStatus.FINISHED, newSet().given(A, score = 38).play(12).status)

    @Test
    fun `O8 le depassement n'est pas un rate et remet le compteur a zero`() {
        val s = newSet().given(A, score = 45, misses = 2).play(9)
        assertEquals(25, s.score(A))
        assertEquals(0, s.misses(A))
    }

    @Test
    fun `O9 aucun lancer apres la fin`() {
        val finished = newSet().given(A, score = 45).play(5)
        assertEquals(ScoringError.SET_FINISHED, finished.tryThrow(B, 3).errorOrNull())
    }
}
