package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.hasUnsafeChars
import com.yogistudio.molkkyboard.core.model.nameKey
import com.yogistudio.molkkyboard.core.model.nextFreeName
import com.yogistudio.molkkyboard.core.model.withoutUnsafeChars
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Noms saisis : caractères de contrôle et de mise en forme invisibles refusés (retour C16 du test). */
class DisplayTextTest {
    @Test
    fun `un nom ordinaire avec accents et emoji est accepte tel quel`() {
        val name = "Léa 🌲 Les Bouleaux 👨‍👩‍👧"
        assertFalse(name.hasUnsafeChars())
        assertSame(name, name.withoutUnsafeChars())
    }

    @Test
    fun `l'inversion du sens de lecture est refusee`() {
        val name = "Léa‮éliminée"
        assertTrue(name.hasUnsafeChars())
        assertEquals("Léaéliminée", name.withoutUnsafeChars())
    }

    @Test
    fun `isolats, espaces de largeur nulle, separateurs et controles sont retires`() {
        val name = "⁦A⁩​B C\u0007D﻿"
        assertTrue(name.hasUnsafeChars())
        assertEquals("ABCD", name.withoutUnsafeChars())
    }

    @Test
    fun `les caracteres d'etiquette hors du plan de base sont retires`() {
        val name = "A" + String(Character.toChars(0xE0041)) + "B"
        assertEquals("AB", name.withoutUnsafeChars())
    }

    @Test
    fun `un texte libre garde ses retours a la ligne si on le demande`() {
        assertEquals("Bravo\nà tous", "Bravo\n\u202Eà tous".withoutUnsafeChars(keepNewlines = true))
        assertEquals("Bravoà tous", "Bravo\nà tous".withoutUnsafeChars())
    }

    @Test
    fun `deux noms identiques aux majuscules, accents et espaces pres ont la meme cle`() {
        assertEquals("lea", "  Léa ".nameKey())
        assertEquals("lea".nameKey(), "LÉA".nameKey())
        assertEquals("jean pierre", "Jean   Pierre".nameKey())
        assertTrue("Lea".nameKey() != "Leo".nameKey())
    }

    @Test
    fun `nom libre suivant - Lea 2, puis Lea 3, borne en longueur`() {
        assertEquals("Léa 2", "Léa".nextFreeName(listOf("Léa"), 20))
        assertEquals("Léa 3", "Léa".nextFreeName(listOf("lea", "LÉA 2"), 20))
        assertEquals("Abcdefghijklmnopqr 2", "Abcdefghijklmnopqrst".nextFreeName(listOf("Abcdefghijklmnopqrst"), 20))
    }
}
