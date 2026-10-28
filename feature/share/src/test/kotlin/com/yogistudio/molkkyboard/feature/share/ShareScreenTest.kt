package com.yogistudio.molkkyboard.feature.share

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.yogistudio.molkkyboard.core.data.GamePlayer
import com.yogistudio.molkkyboard.core.data.GameTeam
import com.yogistudio.molkkyboard.core.data.JournalStatsRepository
import com.yogistudio.molkkyboard.core.data.NewGame
import com.yogistudio.molkkyboard.core.data.RoomGameRepository
import com.yogistudio.molkkyboard.core.data.RoomPlayerRepository
import com.yogistudio.molkkyboard.core.data.memberKey
import com.yogistudio.molkkyboard.core.database.MolkkyBoardDatabase
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.scoring.EventId
import com.yogistudio.molkkyboard.core.scoring.MatchConfig
import com.yogistudio.molkkyboard.core.scoring.Throw
import com.yogistudio.molkkyboard.core.scoring.ThrowResult
import com.yogistudio.molkkyboard.core.stats.Moment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.atomic.AtomicInteger

/** Écran de partage sur une vraie partie : moments détectés, le plus fort en premier, aperçu et réglages. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "fr-w411dp-h891dp-xxhdpi")
class ShareScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val ids = AtomicInteger()
    private lateinit var database: MolkkyBoardDatabase
    private lateinit var vm: ShareViewModel

    @Before
    fun setUp() =
        runBlocking {
            Dispatchers.setMain(UnconfinedTestDispatcher())
            val context: Context = ApplicationProvider.getApplicationContext()
            database =
                Room.inMemoryDatabaseBuilder(context, MolkkyBoardDatabase::class.java).allowMainThreadQueries().build()
            val games = RoomGameRepository(database.gameDao(), { 1L }, { "id-${ids.incrementAndGet()}" })
            val alice = PlayerId("alice")
            val bruno = PlayerId("bruno")
            val id =
                games.create(
                    NewGame(
                        GameConfig.Classic,
                        MatchConfig(setsToWin = 1),
                        listOf(TeamLineup(TeamId("A"), listOf(alice)), TeamLineup(TeamId("B"), listOf(bruno))),
                        listOf(
                            GameTeam(TeamId("A"), "Alice", 0, memberKey = memberKey(listOf(alice))),
                            GameTeam(TeamId("B"), "Bruno", 1, memberKey = memberKey(listOf(bruno))),
                        ),
                        listOf(GamePlayer(alice, "Alice"), GamePlayer(bruno, "Bruno")),
                    ),
                )
            listOf(12, 1, 12, 1, 12, 1, 12, 1, 2).forEach {
                games.append(id, Throw(EventId("e${ids.incrementAndGet()}"), ThrowResult.Pins(listOf(it))))
            }
            games.close(id)
            vm =
                ShareViewModel(
                    SavedStateHandle(mapOf(ShareViewModel.GAME_ID to id)),
                    games,
                    JournalStatsRepository(games),
                    RoomPlayerRepository(database.playerDao()) { 1L },
                )
        }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `partie parfaite - le moment le plus fort est propose en premier`() {
        compose.setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            MolkkyBoardTheme(reducedMotion = true) { ShareContentScreen(state, ShareActions()) }
        }
        compose.waitUntil(5_000) { !vm.state.value.loading }
        val state = vm.state.value
        assertEquals(Moment.Perfect(TeamId("A")), state.moment)
        assertEquals(ShareTemplate.HIGHLIGHT, state.template)
        assertTrue(state.moments.any { it is Moment.Victory })
        compose.onRoot().captureRoboImage("src/test/screenshots/ecran_partage.png")
    }
}
