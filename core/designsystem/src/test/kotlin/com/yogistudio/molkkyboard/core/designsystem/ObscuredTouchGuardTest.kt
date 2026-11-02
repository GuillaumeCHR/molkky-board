package com.yogistudio.molkkyboard.core.designsystem

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yogistudio.molkkyboard.core.designsystem.component.ObscuredTouchGuard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tapjacking : la fenêtre filtre les touchers recouverts tant que la protection est affichée, puis plus du tout. */
@RunWith(AndroidJUnit4::class)
class ObscuredTouchGuardTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `filtre les touchers recouverts seulement pendant l'affichage`() {
        var shown by mutableStateOf(true)
        lateinit var view: View
        compose.setContent {
            view = LocalView.current
            if (shown) ObscuredTouchGuard()
        }
        compose.waitForIdle()
        assertTrue(view.filterTouchesWhenObscured)
        shown = false
        compose.waitForIdle()
        assertFalse(view.filterTouchesWhenObscured)
    }
}
