package com.yogistudio.molkkyboard.feature.intro

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Enchaînement de l'intro : animation ET chargement, passage d'un tap, échec du chargement, animations réduites. */
@RunWith(AndroidJUnit4::class)
class IntroBehaviorTest {
    @get:Rule
    val compose = createComposeRule()

    private var finished = 0
    private var load by mutableStateOf(IntroLoadState.Loading)

    private fun start(initial: IntroLoadState, reducedMotion: Boolean = false, short: Boolean = false) {
        load = initial
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MolkkyBoardTheme(darkTheme = false, reducedMotion = reducedMotion) {
                IntroScreen(loadState = load, onFinished = { finished++ }, logotype = {}, short = short)
            }
        }
    }

    private fun advance(ms: Long) {
        compose.mainClock.advanceTimeBy(ms)
    }

    @Test
    fun `l'intro se termine apres l'animation quand le chargement est fini`() {
        start(IntroLoadState.Ready)
        advance(IntroTimeline.END - 300)
        assertEquals(0, finished)
        advance(600)
        assertEquals(1, finished)
    }

    @Test
    fun `le menu s'ouvre meme si le chargement echoue`() {
        start(IntroLoadState.Failed)
        advance(IntroTimeline.END + 300)
        assertEquals(1, finished)
    }

    @Test
    fun `l'intro attend la fin du chargement sur la signature`() {
        start(IntroLoadState.Loading)
        advance(IntroTimeline.END + 2_000)
        assertEquals(0, finished)
        load = IntroLoadState.Ready
        advance(IntroTimeline.END - IntroTimeline.SIGNATURE_END + 300)
        assertEquals(1, finished)
    }

    @Test
    fun `un tap passe directement au logotype`() {
        start(IntroLoadState.Ready)
        advance(300)
        compose.onRoot().performTouchInput { click() }
        advance(IntroTimeline.END - IntroTimeline.LOGOTYPE_SHOWN + 200)
        assertEquals(1, finished)
    }

    @Test
    fun `animations reduites simple fondu d'environ une seconde`() {
        start(IntroLoadState.Ready, reducedMotion = true)
        advance(IntroTimeline.REDUCED_END + 300)
        assertEquals(1, finished)
    }

    @Test
    fun `version courte en moins de deux secondes`() {
        start(IntroLoadState.Ready, short = true)
        advance(1_300)
        assertEquals(0, finished)
        advance(600)
        assertEquals(1, finished)
    }

    @Test
    fun `version courte attend le chargement sur la signature`() {
        start(IntroLoadState.Loading, short = true)
        advance(3_000)
        assertEquals(0, finished)
        load = IntroLoadState.Ready
        advance(IntroTimeline.END - IntroTimeline.SIGNATURE_END + 300)
        assertEquals(1, finished)
    }
}
