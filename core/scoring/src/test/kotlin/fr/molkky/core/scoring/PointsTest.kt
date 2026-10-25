package fr.molkky.core.scoring

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** `SCORING_SPEC.md` § 3. */
class PointsTest {
    @Test
    fun `P1 une quille seule vaut sa valeur`() {
        val s = newSet().play(7)
        assertEquals(7, s.score(A))
        assertEquals(0, s.misses(A))
        assertEquals(B, s.turn)
    }

    @Test
    fun `P2 plusieurs quilles valent leur nombre`() = assertEquals(2, newSet().play(12, 4).score(A))

    @Test
    fun `P3 deux quilles valent 2 et non leur somme`() = assertEquals(2, newSet().play(3, 5).score(A))

    @Test
    fun `P4 la quille 1 seule vaut 1`() = assertEquals(1, newSet().play(1).score(A))

    @Test
    fun `P5 les 12 quilles valent 12`() = assertEquals(12, newSet().play(*ALL_PINS.toList().toIntArray()).score(A))

    @Test
    fun `P6 aucune quille est un rate`() {
        val s = newSet().play()
        assertEquals(0, s.score(A))
        assertEquals(1, s.misses(A))
        assertEquals(B, s.turn)
    }

    @Test
    fun `P7 un lancer reussi remet le compteur de rates a zero`() {
        val s = newSet().given(A, score = 10, misses = 2).play(2)
        assertEquals(12, s.score(A))
        assertEquals(0, s.misses(A))
    }
}
