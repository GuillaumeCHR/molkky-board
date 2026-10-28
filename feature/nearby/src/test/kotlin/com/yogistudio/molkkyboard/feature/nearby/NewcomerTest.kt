package com.yogistudio.molkkyboard.feature.nearby

import org.junit.Assert.assertEquals
import org.junit.Test

class NewcomerTest {
    @Test
    fun `un pseudo libre est gardé tel quel`() = assertEquals("Léa", newcomerName(listOf("Tom", "Les Pives"), "Léa"))

    @Test
    fun `un pseudo déjà pris, accents et majuscules ignorés, devient Léa 2`() =
        assertEquals("Léa 2", newcomerName(listOf("Tom", "LEA"), "Léa"))

    @Test
    fun `la première couleur libre est attribuée`() = assertEquals(2, freeColor(listOf(0, 1, 3)))

    @Test
    fun `palette pleine, on recommence au début`() = assertEquals(1, freeColor((0..8).toList()))
}
