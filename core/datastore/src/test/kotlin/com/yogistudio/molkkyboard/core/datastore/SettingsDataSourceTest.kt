package com.yogistudio.molkkyboard.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.model.UserSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsDataSourceTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.source() =
        SettingsDataSource(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { folder.newFile("settings.preferences_pb") },
        )

    @Test
    fun `valeurs par defaut`() =
        runTest(UnconfinedTestDispatcher()) {
            assertEquals(UserSettings(), source().settings.first())
        }

    @Test
    fun `chaque reglage est persiste`() =
        runTest(UnconfinedTestDispatcher()) {
            val source = source()
            source.settings.test {
                assertEquals(UserSettings(), awaitItem())
                source.setInputMode(InputMode.QUICK)
                assertEquals(InputMode.QUICK, awaitItem().inputMode)
                source.setSunlight(true)
                assertTrue(awaitItem().sunlight)
                source.setVoiceAnnouncements(true)
                assertTrue(awaitItem().voiceAnnouncements)
                source.setSounds(true)
                assertTrue(awaitItem().sounds)
                source.setCoach(false)
                assertFalse(awaitItem().coach)
                source.setShortIntro(true)
                assertTrue(awaitItem().shortIntro)
                source.setOnboardingDone(true)
                assertTrue(awaitItem().onboardingDone)
            }
        }

    @Test
    fun `intro courte proposee a partir du deuxieme lancement`() =
        runTest(UnconfinedTestDispatcher()) {
            val source = source()
            assertFalse(source.recordLaunch().canChooseShortIntro)
            val second = source.recordLaunch()
            assertEquals(2, second.launchCount)
            assertTrue(second.canChooseShortIntro)
        }
}
