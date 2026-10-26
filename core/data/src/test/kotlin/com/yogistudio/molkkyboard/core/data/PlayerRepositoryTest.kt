package com.yogistudio.molkkyboard.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yogistudio.molkkyboard.core.database.MolkkyBoardDatabase
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
class PlayerRepositoryTest {
    private lateinit var database: MolkkyBoardDatabase
    private lateinit var repository: PlayerRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database =
            Room.inMemoryDatabaseBuilder(context, MolkkyBoardDatabase::class.java).allowMainThreadQueries().build()
        repository = RoomPlayerRepository(database.playerDao()) { 1L }
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `profils tries, mis a jour et archives`() =
        runTest {
            repository.saveProfile(PlayerProfile("b", "bruno", 1, "🐻"))
            repository.saveProfile(PlayerProfile("a", "Alice", 0, "🦊"))
            assertEquals(listOf("Alice", "bruno"), repository.profiles.first().map { it.name })
            repository.saveProfile(PlayerProfile("a", "Alice", 0, "🐸", photoUri = "file:///a.jpg"))
            assertEquals(
                "file:///a.jpg",
                repository.profiles
                    .first()
                    .first()
                    .photoUri,
            )
            repository.archiveProfile("b")
            assertEquals(listOf("a"), repository.profiles.first().map { it.id })
        }

    @Test
    fun `une equipe se retrouve par ses joueurs`() =
        runTest {
            assertNull(repository.team("key"))
            repository.saveTeam(KnownTeam("key", "Les Bouleaux", 3, "leaf"))
            assertEquals(KnownTeam("key", "Les Bouleaux", 3, "leaf"), repository.team("key"))
        }
}
