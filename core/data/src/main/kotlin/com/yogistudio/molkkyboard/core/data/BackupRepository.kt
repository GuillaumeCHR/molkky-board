package com.yogistudio.molkkyboard.core.data

import android.content.Context
import com.yogistudio.molkkyboard.core.database.dao.BackupDao
import com.yogistudio.molkkyboard.core.database.entity.GameEntity
import com.yogistudio.molkkyboard.core.database.entity.GameEventEntity
import com.yogistudio.molkkyboard.core.database.entity.GamePlayerEntity
import com.yogistudio.molkkyboard.core.database.entity.GameSetEntity
import com.yogistudio.molkkyboard.core.database.entity.GameTeamEntity
import com.yogistudio.molkkyboard.core.database.entity.MolkkoutThrowEntity
import com.yogistudio.molkkyboard.core.database.entity.PlayerProfileEntity
import com.yogistudio.molkkyboard.core.database.entity.TeamEntity
import com.yogistudio.molkkyboard.core.scoring.SetEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Bilan d'une restauration : les parties déjà présentes ou invalides sont écartées, jamais écrasées. */
data class RestoreReport(
    val gamesAdded: Int,
    val gamesAlreadyThere: Int,
    val gamesInvalid: Int,
    val playersAdded: Int,
)

/** Échec de lecture d'une sauvegarde, sans rien avoir écrit. */
enum class RestoreError { NOT_A_BACKUP, NEWER_VERSION, TOO_LARGE, UNREADABLE }

class RestoreException(
    val error: RestoreError,
) : Exception(error.name)

interface BackupRepository {
    /** Écrit toutes les données locales dans [out] (sauvegarde, et export RGPD). */
    suspend fun export(out: OutputStream)

    /** Ajoute le contenu d'une sauvegarde aux données existantes. Lève [RestoreException] si le fichier est refusé. */
    suspend fun restore(input: InputStream): RestoreReport

    /** Supprime toutes les parties, les profils, les équipes et les photos de joueurs. Les réglages sont conservés. */
    suspend fun deleteAll()
}

@Singleton
class RoomBackupRepository
    @Inject
    constructor(
        private val dao: BackupDao,
        private val time: TimeSource,
        private val app: AppInfo,
        @param:ApplicationContext private val context: Context,
    ) : BackupRepository {
        override suspend fun export(out: OutputStream) {
            val file = snapshot()
            withContext(Dispatchers.IO) {
                out.write(BackupJson.encodeToString(file).toByteArray(Charsets.UTF_8))
                out.flush()
            }
        }

        private suspend fun snapshot(): BackupFile {
            val sets = dao.sets().groupBy { it.gameId }
            val teams = dao.gameTeams().groupBy { it.gameId }
            val players = dao.gamePlayers().groupBy { it.gameId }
            val events = dao.events().groupBy { it.gameId }
            val molkkout = dao.molkkout().groupBy { it.gameId }
            return BackupFile(
                exportedAt = time.now(),
                app = app.label(),
                // Les photos restent sur le téléphone : seul l'avatar emoji est sauvegardé.
                players =
                    dao.profiles().map {
                        BackupPlayer(
                            it.id,
                            it.name,
                            it.colorIndex,
                            it.avatar,
                            it.createdAt,
                            it.archived,
                        )
                    },
                teams =
                    dao.teams().map {
                        BackupTeam(
                            it.memberKey,
                            it.name,
                            it.colorIndex,
                            it.emblem,
                            it.createdAt,
                        )
                    },
                games =
                    dao.games().map { game ->
                        game.toBackup(
                            sets[game.id].orEmpty(),
                            teams[game.id].orEmpty(),
                            players[game.id].orEmpty(),
                            events[game.id].orEmpty(),
                            molkkout[game.id].orEmpty(),
                        )
                    },
            )
        }

        private fun GameEntity.toBackup(
            sets: List<GameSetEntity>,
            teams: List<GameTeamEntity>,
            players: List<GamePlayerEntity>,
            events: List<GameEventEntity>,
            molkkout: List<MolkkoutThrowEntity>,
        ): BackupGame {
            val json = MolkkyJson
            val bySet = events.groupBy { it.setIndex }
            return BackupGame(
                id = id,
                createdAt = createdAt,
                updatedAt = updatedAt,
                closedAt = closedAt,
                place = place,
                config = json.decodeFromString(configJson),
                match = json.decodeFromString(matchConfigJson),
                teams = teams.map { BackupGameTeam(it.teamId, it.name, it.colorIndex, it.emblem, it.memberKey) },
                players = players.map { BackupGamePlayer(it.playerId, it.name, it.profileId) },
                sets =
                    sets.map { set ->
                        BackupSet(
                            index = set.setIndex,
                            startedAt = set.startedAt,
                            lineup = json.decodeFromString(set.lineupJson),
                            events =
                                bySet[set.setIndex].orEmpty().map {
                                    BackupEvent(it.createdAt, json.decodeFromString<SetEvent>(it.payloadJson))
                                },
                        )
                    },
                molkkout = molkkout.map { json.decodeFromString(it.payloadJson) },
            )
        }

        override suspend fun restore(input: InputStream): RestoreReport {
            val file = withContext(Dispatchers.IO) { parse(read(input)) }
            val known = dao.gameIds().toSet()
            val fresh = file.games.filter { it.id !in known }
            // Un identifiant d'événement déjà utilisé (ailleurs dans la base ou dans le fichier) écarte la partie.
            val usedEvents = dao.eventIds().toHashSet()
            val valid =
                fresh.filter(BackupRules::game).filter { game ->
                    val ids = game.sets.flatMap { set -> set.events.map { it.event.id.value } }
                    ids.none { it in usedEvents }.also { ok -> if (ok) usedEvents += ids }
                }
            val players = file.players.filter(BackupRules::player)
            val existingPlayers = dao.profiles().map { it.id }.toSet()
            write(players, file.teams.filter(BackupRules::team), valid)
            return RestoreReport(
                gamesAdded = valid.size,
                gamesAlreadyThere = file.games.size - fresh.size,
                gamesInvalid = fresh.size - valid.size,
                playersAdded = players.count { it.id !in existingPlayers },
            )
        }

        /** Lecture bornée : un fichier trop gros est refusé avant d'être chargé en entier. */
        private fun read(input: InputStream): String {
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER)
            try {
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    out.write(buffer, 0, n)
                    if (out.size() > BackupRules.MAX_BYTES) throw RestoreException(RestoreError.TOO_LARGE)
                }
            } catch (_: IOException) {
                throw RestoreException(RestoreError.UNREADABLE)
            }
            return out.toString(Charsets.UTF_8.name())
        }

        private fun parse(text: String): BackupFile {
            val file =
                try {
                    BackupJson.decodeFromString<BackupFile>(text)
                } catch (_: SerializationException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
            val error =
                when {
                    file == null || file.format != BackupFile.FORMAT -> RestoreError.NOT_A_BACKUP
                    file.version > BackupFile.VERSION -> RestoreError.NEWER_VERSION
                    else -> return file
                }
            throw RestoreException(error)
        }

        /** Réécrit chaque champ JSON avec l'encodeur de l'app : la base ne contient que ce que l'app a produit. */
        private suspend fun write(players: List<BackupPlayer>, teams: List<BackupTeam>, games: List<BackupGame>) {
            val json = MolkkyJson
            dao.restore(
                profiles =
                    players.map {
                        PlayerProfileEntity(it.id, it.name, it.colorIndex, it.avatar, null, it.createdAt, it.archived)
                    },
                teams = teams.map { TeamEntity(it.memberKey, it.name, it.colorIndex, it.emblem, it.createdAt) },
                games =
                    games.map {
                        GameEntity(
                            it.id,
                            it.createdAt,
                            it.updatedAt,
                            json.encodeToString(it.config),
                            json.encodeToString(it.match),
                            it.place,
                            it.closedAt,
                        )
                    },
                sets =
                    games.flatMap { g ->
                        g.sets.map { GameSetEntity(g.id, it.index, json.encodeToString(it.lineup), it.startedAt) }
                    },
                gameTeams =
                    games.flatMap { g ->
                        g.teams.map { GameTeamEntity(g.id, it.id, it.name, it.colorIndex, it.emblem, it.memberKey) }
                    },
                gamePlayers =
                    games.flatMap { g ->
                        g.players.map { GamePlayerEntity(g.id, it.id, it.name, it.profileId) }
                    },
                events =
                    games.flatMap { g ->
                        g.sets.flatMap { set ->
                            set.events.mapIndexed { i, e ->
                                GameEventEntity(
                                    id = e.event.id.value,
                                    gameId = g.id,
                                    setIndex = set.index,
                                    localSeq = i + 1L,
                                    createdAt = e.createdAt,
                                    payloadJson = json.encodeToString(e.event),
                                )
                            }
                        }
                    },
                molkkout =
                    games.flatMap { g ->
                        g.molkkout.mapIndexed { i, t -> MolkkoutThrowEntity(g.id, i + 1, json.encodeToString(t)) }
                    },
            )
        }

        override suspend fun deleteAll() {
            dao.deleteAll()
            withContext(Dispatchers.IO) {
                listOf(
                    File(context.filesDir, "avatars"),
                    File(context.cacheDir, "share"),
                    File(context.cacheDir, "camera"),
                ).forEach { it.deleteRecursively() }
            }
        }

        private companion object {
            const val BUFFER = 64 * 1024
        }
    }

/** Nom et version de l'app, inscrits dans la sauvegarde. */
fun interface AppInfo {
    fun label(): String
}
