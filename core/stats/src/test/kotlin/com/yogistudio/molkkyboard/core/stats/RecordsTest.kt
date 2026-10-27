package com.yogistudio.molkkyboard.core.stats

import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.scoring.SetEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// Cas `B` de `docs/STATS_SPEC.md` § 9 : records secondaires et badges.
class RecordsTest {
    private val ab = listOf(T("A"), T("B"))
    private val a = PlayerId("A")

    /** A : 12, 12, 12, 9 (45), 9 (dépasse : 25), 12, 12, 1 → 50. B marque 1 entre chaque lancer. */
    private fun phoenixLog(): List<SetEvent> = firstWins(12, 12, 12, 9, 9, 12, 12, 1)

    /** B fait 12, 12, 12 (A à 3) puis trois ratés : éliminé, A gagne après 33 points de retard. */
    private fun comebackLog(): List<SetEvent> =
        listOf(
            pins(1),
            pins(12),
            pins(1),
            pins(12),
            pins(1),
            pins(12),
            pins(1),
            pins(),
            pins(1),
            pins(),
            pins(1),
            pins(),
        )

    @Test
    fun `B1 douze reussis et retours a 25 - un 12 qui fait depasser ne compte pas`() {
        val overshootTwelve = firstWins(12, 12, 12, 12, 12) // 5e 12 : 48 → 60, retour à 25
        val line = summarize(game("g", 1, ab, overshootTwelve)).single().players.getValue(a)
        assertEquals(4, line.twelves)
        assertEquals(1, line.resets)
        val set = summarize(game("p", 1, ab, phoenixLog())).single()
        assertEquals(5, set.players.getValue(a).twelves)
        assertEquals(1, set.team("A")!!.resets)
    }

    @Test
    fun `B2 plus grand retard et plus belle remontee`() {
        val set = summarize(game("c", 1, ab, comebackLog())).single()
        assertEquals("A", set.winner)
        assertEquals(33, set.team("A")!!.maxDeficit)
        val record = secondaryRecords(listOf(set)).single { it.kind == SecondaryRecordKind.BIGGEST_COMEBACK }
        assertEquals(SecondaryRecord(SecondaryRecordKind.BIGGEST_COMEBACK, "A", 33, "c", 1), record)
    }

    @Test
    fun `B3 records secondaires - un detenteur par record, le premier garde le record a egalite`() {
        val records =
            secondaryRecords(
                summaries(
                    game("g1", 10, ab, phoenixLog()),
                    game("g2", 5, ab, phoenixLog()),
                    game("g3", 20, ab, firstWins(9, 9, 9, 9, 9, 9, 12, 12, 1)),
                ),
            ).associateBy { it.kind }
        assertEquals(
            SecondaryRecord(SecondaryRecordKind.MOST_TWELVES, "A", 5, "g2", 5),
            records[SecondaryRecordKind.MOST_TWELVES],
        )
        assertEquals(
            SecondaryRecord(SecondaryRecordKind.MOST_RESETS, "A", 3, "g3", 20),
            records[SecondaryRecordKind.MOST_RESETS],
        )
        // Série : 9 lancers sans raté en g3, contre 8 dans les parties « phénix ».
        assertEquals(
            SecondaryRecord(SecondaryRecordKind.LONGEST_STREAK, "A", 9, "g3", 20),
            records[SecondaryRecordKind.LONGEST_STREAK],
        )
    }

    @Test
    fun `B4 records secondaires - mode Ludique et parties abandonnees exclus`() {
        val records =
            secondaryRecords(
                summaries(
                    game("l", 1, ab, phoenixLog(), config = GameConfig.Playful),
                    game("x", 2, ab, phoenixLog(), abandoned = true),
                ),
            )
        assertTrue(records.isEmpty())
    }

    @Test
    fun `B5 badges - premiere victoire, partie parfaite, phenix, sniper`() {
        val perfect = badges(summaries(game("p", 1, ab, perfectLog())), a).map { it.badge }
        assertEquals(listOf(Badge.FIRST_WIN, Badge.PERFECT_GAME, Badge.SNIPER), perfect)
        val all = badges(summaries(game("p", 1, ab, perfectLog()), game("f", 2, ab, phoenixLog())), a)
        assertEquals(Badge.entries.toList(), all.map { it.badge })
        assertEquals("f", all.single { it.badge == Badge.PHOENIX }.gameId)
        assertEquals("p", all.single { it.badge == Badge.SNIPER }.gameId)
        assertTrue(badges(summaries(game("p", 1, ab, perfectLog())), PlayerId("B")).isEmpty())
    }

    @Test
    fun `B6 badges - la partie parfaite exige les regles officielles, les autres badges non`() {
        val casual = badges(summaries(game("l", 1, ab, perfectLog(), config = GameConfig.Playful)), a).map { it.badge }
        assertEquals(listOf(Badge.FIRST_WIN, Badge.SNIPER), casual)
    }
}
