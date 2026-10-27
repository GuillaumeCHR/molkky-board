package com.yogistudio.molkkyboard.feature.history

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.yogistudio.molkkyboard.core.data.GamePlayer
import com.yogistudio.molkkyboard.core.data.GameTeam
import com.yogistudio.molkkyboard.core.data.NewGame
import com.yogistudio.molkkyboard.core.data.RoomGameRepository
import com.yogistudio.molkkyboard.core.data.memberKey
import com.yogistudio.molkkyboard.core.database.MolkkyBoardDatabase
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.scoring.Abandon
import com.yogistudio.molkkyboard.core.scoring.Correct
import com.yogistudio.molkkyboard.core.scoring.EventId
import com.yogistudio.molkkyboard.core.scoring.MatchConfig
import com.yogistudio.molkkyboard.core.scoring.Throw
import com.yogistudio.molkkyboard.core.scoring.ThrowResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "fr-w411dp-h891dp-xxhdpi")
class HistoryTest {
    @get:Rule
    val compose = createComposeRule()

    private val ids = AtomicInteger()
    private val clock = AtomicLong(1_767_000_000_000)
    private lateinit var database: MolkkyBoardDatabase
    private lateinit var games: RoomGameRepository

    private val alice = PlayerId("alice")
    private val bruno = PlayerId("bruno")
    private val newGame =
        NewGame(
            config = GameConfig.Classic,
            matchConfig = MatchConfig(setsToWin = 1),
            lineup = listOf(TeamLineup(TeamId("A"), listOf(alice)), TeamLineup(TeamId("B"), listOf(bruno))),
            teams =
                listOf(
                    GameTeam(TeamId("A"), "Alice", 0, memberKey = memberKey(listOf(alice))),
                    GameTeam(TeamId("B"), "Bruno", 1, memberKey = memberKey(listOf(bruno))),
                ),
            players = listOf(GamePlayer(alice, "Alice"), GamePlayer(bruno, "Bruno")),
        )

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database =
            Room.inMemoryDatabaseBuilder(context, MolkkyBoardDatabase::class.java).allowMainThreadQueries().build()
        games =
            RoomGameRepository(database.gameDao(), { clock.addAndGet(3_600_000) }, { "id-${ids.incrementAndGet()}" })
    }

    @After
    fun tearDown() = database.close()

    private fun pins(vararg pins: Int) = Throw(EventId("e-${ids.incrementAndGet()}"), ThrowResult.Pins(pins.toList()))

    private suspend fun won(): String {
        val id = games.create(newGame)
        listOf(12, 1, 12, 1, 12, 1, 12, 1, 2).forEach { games.append(id, pins(it)) }
        games.close(id)
        return id
    }

    @Test
    fun `chaque partie close apparait avec son vainqueur et ses mentions`() =
        runTest {
            val first = won()
            val abandoned = games.create(newGame)
            games.append(abandoned, pins(5))
            games.append(abandoned, Abandon(EventId("ab")))
            games.close(abandoned)
            val corrected = won()
            val target =
                games
                    .observe(corrected)
                    .first()!!
                    .currentSet.log
                    .first()
                    .id
            games.append(corrected, Correct(EventId("c"), target, Throw(EventId("r"), ThrowResult.Pins(listOf(12)))))
            val items = games.observeHistory().first().map { it.toItem() }
            assertEquals(listOf(corrected, abandoned, first), items.map { it.gameId })
            assertTrue(items[0].modifiedAfterFinish)
            assertTrue(items[1].abandoned)
            assertEquals(listOf(true, false), items[2].teams.map { it.winner })
            assertEquals(listOf(50, 4), items[2].teams.map { it.score })
            assertFalse(items[2].corrected)
        }

    @Test
    fun `capture de l'historique`() =
        runTest {
            won()
            val abandoned = games.create(newGame)
            games.append(abandoned, pins(5))
            games.append(abandoned, Abandon(EventId("ab")))
            games.close(abandoned)
            val state = HistoryUiState(loading = false, items = games.observeHistory().first().map { it.toItem() })
            compose.setContent { MolkkyBoardTheme(reducedMotion = true) { HistoryContent(state, {}, {}) } }
            compose.onRoot().captureRoboImage("src/test/screenshots/historique.png")
        }

    @Test
    fun `capture de l'historique vide`() {
        compose.setContent {
            MolkkyBoardTheme(reducedMotion = true) { HistoryContent(HistoryUiState(loading = false), {}, {}) }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/historique_vide.png")
    }
}
