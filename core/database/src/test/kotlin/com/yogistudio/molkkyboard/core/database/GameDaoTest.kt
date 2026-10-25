package com.yogistudio.molkkyboard.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yogistudio.molkkyboard.core.database.entity.GameEntity
import com.yogistudio.molkkyboard.core.database.entity.GameEventEntity
import com.yogistudio.molkkyboard.core.database.entity.GameSetEntity
import com.yogistudio.molkkyboard.core.database.entity.GameTeamEntity
import com.yogistudio.molkkyboard.core.database.entity.PlayerProfileEntity
import com.yogistudio.molkkyboard.core.database.entity.TeamEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameDaoTest {
    private lateinit var database: MolkkyBoardDatabase

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database =
            Room.inMemoryDatabaseBuilder(context, MolkkyBoardDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = database.close()

    private fun game(id: String, updatedAt: Long = 1) =
        GameEntity(
            id,
            1,
            updatedAt,
            "{}",
            "{}",
            place = null,
            closedAt = null,
        )

    private suspend fun create(id: String, updatedAt: Long = 1) =
        database.gameDao().createGame(
            game(id, updatedAt),
            GameSetEntity(id, 0, "[]", 1),
            listOf(GameTeamEntity(id, "A", "Alice", 0, null, "key-a")),
        )

    private fun event(id: String, gameId: String, at: Long) =
        GameEventEntity(id = id, gameId = gameId, setIndex = 0, localSeq = 0, createdAt = at, payloadJson = "{}")

    @Test
    fun `le journal est numerote dans l'ordre d'ajout`() =
        runTest {
            val dao = database.gameDao()
            create("g")
            dao.appendEvent(event("e1", "g", 10))
            dao.appendEvent(event("e2", "g", 11))
            val stored = dao.appendEvent(event("e3", "g", 12))
            assertEquals(3L, stored.localSeq)
            assertEquals(listOf("e1", "e2", "e3"), dao.events("g", 0).map { it.id })
            assertEquals(listOf(1L, 2L, 3L), dao.observeEvents("g").first().map { it.localSeq })
            assertEquals(12L, dao.observeGame("g").first()!!.updatedAt)
        }

    @Test
    fun `supprimer une partie supprime ses manches equipes et evenements`() =
        runTest {
            val dao = database.gameDao()
            create("g")
            dao.appendEvent(event("e1", "g", 10))
            dao.deleteGame("g")
            assertNull(dao.observeGame("g").first())
            assertEquals(emptyList<Any>(), dao.observeSets("g").first())
            assertEquals(emptyList<Any>(), dao.observeTeams("g").first())
            assertEquals(emptyList<Any>(), dao.observeEvents("g").first())
        }

    @Test
    fun `partie en cours la plus recente et historique`() =
        runTest {
            val dao = database.gameDao()
            create("ancienne", updatedAt = 5)
            create("recente", updatedAt = 9)
            assertEquals("recente", dao.observeOngoing().first()!!.id)
            dao.close("recente", 20)
            assertEquals("ancienne", dao.observeOngoing().first()!!.id)
            assertEquals(listOf("recente"), dao.observeClosed().first().map { it.id })
            dao.reopen("recente", 30)
            assertEquals("recente", dao.observeOngoing().first()!!.id)
        }

    @Test
    fun `profils et identites d'equipe`() =
        runTest {
            val dao = database.playerDao()
            dao.upsertProfile(PlayerProfileEntity("p2", "bruno", 1, "🦊", null, 1))
            dao.upsertProfile(PlayerProfileEntity("p1", "Alice", 0, null, null, 1))
            assertEquals(listOf("Alice", "bruno"), dao.observeProfiles().first().map { it.name })
            dao.archiveProfile("p2")
            assertEquals(listOf("Alice"), dao.observeProfiles().first().map { it.name })
            dao.upsertTeam(TeamEntity("key", "Les Bouleaux", 4, "leaf", 1))
            assertEquals("Les Bouleaux", dao.team("key")!!.name)
            assertNull(dao.team("autre"))
        }
}
