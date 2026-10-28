package com.yogistudio.molkkyboard.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yogistudio.molkkyboard.core.database.MolkkyBoardDatabase
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.scoring.EventId
import com.yogistudio.molkkyboard.core.scoring.MatchConfig
import com.yogistudio.molkkyboard.core.scoring.Throw
import com.yogistudio.molkkyboard.core.scoring.ThrowResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger

/** Sauvegarde, restauration (fichier non fiable) et suppression de toutes les données. */
@RunWith(RobolectricTestRunner::class)
class BackupRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = AtomicInteger(1_000)
    private val ids = AtomicInteger()
    private val databases = mutableListOf<MolkkyBoardDatabase>()

    private val alice = PlayerId("alice")
    private val bruno = PlayerId("bruno")
    private val newGame =
        NewGame(
            config = GameConfig.Classic,
            matchConfig = MatchConfig.Classic,
            lineup = listOf(TeamLineup(TeamId("A"), listOf(alice)), TeamLineup(TeamId("B"), listOf(bruno))),
            teams =
                listOf(
                    GameTeam(TeamId("A"), "Alice", 0, memberKey = memberKey(listOf(alice))),
                    GameTeam(TeamId("B"), "Bruno", 1, memberKey = memberKey(listOf(bruno))),
                ),
            players = listOf(GamePlayer(alice, "Alice", profileId = "alice"), GamePlayer(bruno, "Bruno")),
        )

    private class Phone(
        val database: MolkkyBoardDatabase,
        val games: GameRepository,
        val players: PlayerRepository,
        val backup: BackupRepository,
    )

    private fun phone(): Phone {
        val db =
            Room
                .inMemoryDatabaseBuilder(context, MolkkyBoardDatabase::class.java)
                .allowMainThreadQueries()
                .build()
                .also(databases::add)
        val time = TimeSource { clock.incrementAndGet().toLong() }
        return Phone(
            db,
            RoomGameRepository(db.gameDao(), time) { "id-${ids.incrementAndGet()}" },
            RoomPlayerRepository(db.playerDao(), time),
            RoomBackupRepository(db.backupDao(), time, { "Molkky Board test" }, context),
        )
    }

    private lateinit var source: Phone

    @Before
    fun setUp() {
        source = phone()
    }

    @After
    fun tearDown() = databases.forEach { it.close() }

    private fun pins(vararg pins: Int) = Throw(EventId("e-${ids.incrementAndGet()}"), ThrowResult.Pins(pins.toList()))

    private suspend fun BackupRepository.exportText(): String =
        ByteArrayOutputStream().also { export(it) }.toString(Charsets.UTF_8.name())

    private suspend fun BackupRepository.restoreText(text: String) = restore(ByteArrayInputStream(text.toByteArray()))

    private suspend fun playedGame(): String {
        source.players.saveProfile(PlayerProfile("alice", "Alice", 0, "🦊", photoUri = "file:///avatars/alice.jpg"))
        source.players.saveTeam(KnownTeam("key", "Les Bouleaux", 3, "🌲"))
        val id = source.games.create(newGame)
        listOf(pins(12), pins(3, 4), pins(), pins(7)).forEach { source.games.append(id, it) }
        return id
    }

    private fun failure(block: suspend () -> Unit): RestoreError? =
        try {
            kotlinx.coroutines.runBlocking { block() }
            null
        } catch (e: RestoreException) {
            e.error
        }

    @Test
    fun `aller-retour - les parties, profils et equipes reviennent a l'identique, sans photo`() =
        runTest {
            val id = playedGame()
            val text = source.backup.exportText()
            assertTrue(text.contains("\"format\": \"molkky-board-backup\""))
            assertFalse("aucune photo dans le fichier", text.contains("alice.jpg"))

            val target = phone()
            val report = target.backup.restoreText(text)
            assertEquals(
                RestoreReport(gamesAdded = 1, gamesAlreadyThere = 0, gamesInvalid = 0, playersAdded = 1),
                report,
            )
            val before = source.games.observe(id).first()!!
            val after = target.games.observe(id).first()!!
            assertEquals(before.currentSet.log, after.currentSet.log)
            assertEquals(before.currentSet.replay.state, after.currentSet.replay.state)
            assertEquals(before.teams, after.teams)
            assertEquals(before.players, after.players)
            val profile =
                target.players.profiles
                    .first()
                    .single()
            assertEquals("🦊", profile.avatar)
            assertNull(profile.photoUri)
            assertEquals(KnownTeam("key", "Les Bouleaux", 3, "🌲"), target.players.team("key"))
            // On peut continuer à jouer la partie restaurée.
            assertTrue(target.games.append(id, pins(9)) is com.yogistudio.molkkyboard.core.scoring.Outcome.Ok)
        }

    @Test
    fun `restaurer deux fois n'ajoute rien et n'ecrase rien`() =
        runTest {
            playedGame()
            val text = source.backup.exportText()
            val report = source.backup.restoreText(text)
            assertEquals(
                RestoreReport(gamesAdded = 0, gamesAlreadyThere = 1, gamesInvalid = 0, playersAdded = 0),
                report,
            )
            assertEquals(
                1,
                source.database
                    .backupDao()
                    .games()
                    .size,
            )
            assertEquals(
                4,
                source.database
                    .backupDao()
                    .events()
                    .size,
            )
        }

    @Test
    fun `fichiers refuses - autre format, version future, JSON invalide, trop gros`() {
        assertEquals(RestoreError.NOT_A_BACKUP, failure { source.backup.restoreText("pas du JSON") })
        assertEquals(
            RestoreError.NOT_A_BACKUP,
            failure {
                source.backup.restoreText("""{"format":"autre","exportedAt":1,"app":"x"}""")
            },
        )
        assertEquals(
            RestoreError.NEWER_VERSION,
            failure {
                source.backup.restoreText(
                    """{"format":"molkky-board-backup","version":99,"exportedAt":1,"app":"x"}""",
                )
            },
        )
        val endless =
            object : InputStream() {
                override fun read(): Int = ' '.code

                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    b.fill(' '.code.toByte(), off, off + len)
                    return len
                }
            }
        assertEquals(RestoreError.TOO_LARGE, failure { source.backup.restore(endless) })
    }

    @Test
    fun `une partie alteree est ecartee, les autres sont restaurees`() =
        runTest {
            playedGame()
            source.games.create(newGame)
            val text = source.backup.exportText()
            val target = phone()
            val file = BackupJson.decodeFromString<BackupFile>(text)
            val (first, second) = file.games
            val tampered =
                file.copy(
                    games =
                        listOf(
                            first.copy(teams = first.teams.map { it.copy(name = "Alice\u0000") }),
                            second,
                            second.copy(
                                id = "autre",
                                sets =
                                    second.sets.map {
                                        it.copy(
                                            lineup = listOf(TeamLineup(TeamId("Z"), listOf(alice))),
                                        )
                                    },
                            ),
                            first.copy(id = "copie"),
                            first.copy(id = "copie-2"),
                            second.copy(id = "<script>"),
                        ),
                )
            val report = target.backup.restoreText(BackupJson.encodeToString(tampered))
            // « copie » reprend les événements de la partie altérée (écartée) : elle passe, pas « copie-2 ».
            assertEquals(2, report.gamesAdded)
            assertEquals(4, report.gamesInvalid)
            assertEquals(
                setOf(second.id, "copie"),
                target.database
                    .backupDao()
                    .games()
                    .map { it.id }
                    .toSet(),
            )
        }

    @Test
    fun `un nom qui inverse le sens de lecture est refuse a la restauration`() =
        runTest {
            playedGame()
            val file = BackupJson.decodeFromString<BackupFile>(source.backup.exportText())
            val tampered =
                file.copy(
                    games =
                        file.games.map { g ->
                            g.copy(teams = g.teams.map { it.copy(name = "Léa\u202Ezob") })
                        },
                )
            val report = phone().backup.restoreText(BackupJson.encodeToString(tampered))
            assertEquals(0, report.gamesAdded)
            assertEquals(1, report.gamesInvalid)
        }

    @Test
    fun `tout supprimer vide la base et les photos, sans toucher aux reglages`() =
        runTest {
            playedGame()
            val photo = File(context.filesDir, "avatars/alice-1.jpg").apply { parentFile?.mkdirs() }
            photo.writeText("jpg")
            source.backup.deleteAll()
            val dao = source.database.backupDao()
            assertTrue(dao.games().isEmpty())
            assertTrue(dao.events().isEmpty())
            assertTrue(dao.profiles().isEmpty())
            assertTrue(dao.teams().isEmpty())
            assertFalse(photo.exists())
        }
}
