package com.yogistudio.molkkyboard.core.data

import com.yogistudio.molkkyboard.core.database.dao.GameDao
import com.yogistudio.molkkyboard.core.database.entity.GameEntity
import com.yogistudio.molkkyboard.core.database.entity.GameEventEntity
import com.yogistudio.molkkyboard.core.database.entity.GamePlayerEntity
import com.yogistudio.molkkyboard.core.database.entity.GameSetEntity
import com.yogistudio.molkkyboard.core.database.entity.GameTeamEntity
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.scoring.MatchConfig
import com.yogistudio.molkkyboard.core.scoring.Outcome
import com.yogistudio.molkkyboard.core.scoring.PlayEvent
import com.yogistudio.molkkyboard.core.scoring.ReplayResult
import com.yogistudio.molkkyboard.core.scoring.ScoringError
import com.yogistudio.molkkyboard.core.scoring.SetEvent
import com.yogistudio.molkkyboard.core.scoring.checkInput
import com.yogistudio.molkkyboard.core.scoring.getOrThrow
import com.yogistudio.molkkyboard.core.scoring.replay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Parties locales : Room est l'unique source de vérité, l'état de chaque manche est recalculé par rejeu. */
interface GameRepository {
    suspend fun create(game: NewGame): String

    fun observe(gameId: String): Flow<GameSnapshot?>

    /**
     * Valide [event] avec le moteur puis l'écrit en fin de journal de la manche en cours. Rien n'est écrit si
     * l'événement est refusé : l'erreur est renvoyée.
     */
    suspend fun append(gameId: String, event: SetEvent): Outcome<ReplayResult>

    suspend fun startNextSet(gameId: String, lineup: List<TeamLineup>)

    /** Identifiant de la partie en cours la plus récente, à proposer en reprise. */
    fun observeOngoingGameId(): Flow<String?>

    suspend fun close(gameId: String)

    suspend fun reopen(gameId: String)

    suspend fun delete(gameId: String)
}

@Singleton
class RoomGameRepository
    @Inject
    constructor(
        private val dao: GameDao,
        private val time: TimeSource,
        private val ids: IdGenerator,
    ) : GameRepository {
        private val json = MolkkyJson
        private val writes = Mutex()

        override suspend fun create(game: NewGame): String {
            val id = ids.newId()
            val now = time.now()
            dao.createGame(
                GameEntity(
                    id = id,
                    createdAt = now,
                    updatedAt = now,
                    configJson = json.encodeToString(game.config),
                    matchConfigJson = json.encodeToString(game.matchConfig),
                    place = game.place,
                    closedAt = null,
                ),
                GameSetEntity(id, 0, json.encodeToString(game.lineup), now),
                game.teams.map { GameTeamEntity(id, it.id.value, it.name, it.colorIndex, it.emblem, it.memberKey) },
                game.players.map { GamePlayerEntity(id, it.id.value, it.name, it.profileId) },
            )
            return id
        }

        override fun observe(gameId: String): Flow<GameSnapshot?> =
            combine(
                dao.observeGame(gameId),
                dao.observeSets(gameId),
                dao.observeTeams(gameId),
                dao.observePlayers(gameId),
                dao.observeEvents(gameId),
            ) { game, sets, teams, players, events ->
                game ?: return@combine null
                val config = json.decodeFromString<GameConfig>(game.configJson)
                val bySet = events.groupBy { it.setIndex }
                GameSnapshot(
                    id = game.id,
                    createdAt = game.createdAt,
                    closedAt = game.closedAt,
                    config = config,
                    matchConfig = json.decodeFromString<MatchConfig>(game.matchConfigJson),
                    teams = teams.map { GameTeam(TeamId(it.teamId), it.name, it.colorIndex, it.emblem, it.memberKey) },
                    sets =
                        sets.map { set ->
                            val lineup = json.decodeFromString<List<TeamLineup>>(set.lineupJson)
                            val log =
                                bySet[set.setIndex].orEmpty().map {
                                    json.decodeFromString<SetEvent>(
                                        it.payloadJson,
                                    )
                                }
                            GameSetSnapshot(set.setIndex, lineup, log, replay(config, lineup, log).getOrThrow())
                        },
                    place = game.place,
                    players = players.map { GamePlayer(PlayerId(it.playerId), it.name, it.profileId) },
                )
            }

        override suspend fun append(gameId: String, event: SetEvent): Outcome<ReplayResult> =
            writes.withLock {
                val snapshot = currentSet(gameId) ?: return Outcome.Err(ScoringError.UNKNOWN_EVENT)
                val config = snapshot.config
                val lineup = snapshot.lineup
                val log = snapshot.log
                val before = replay(config, lineup, log).getOrThrow()
                val error =
                    when (event) {
                        is PlayEvent -> checkInput(before.state, event)
                        else -> null
                    }
                if (error != null) return Outcome.Err(error)
                val after = replay(config, lineup, log + event).getOrThrow()
                // Un Undo, une correction ou une suppression refusés par le moteur ne sont pas écrits.
                after.rejected
                    .lastOrNull()
                    ?.takeIf { it.event == event }
                    ?.let { return Outcome.Err(it.error) }
                dao.appendEvent(
                    GameEventEntity(
                        id = event.id.value,
                        gameId = gameId,
                        setIndex = snapshot.index,
                        localSeq = 0,
                        createdAt = time.now(),
                        payloadJson = json.encodeToString(event),
                    ),
                )
                Outcome.Ok(after)
            }

        private data class CurrentSet(
            val config: GameConfig,
            val index: Int,
            val lineup: List<TeamLineup>,
            val log: List<SetEvent>,
        )

        private suspend fun currentSet(gameId: String): CurrentSet? {
            val snapshot = observe(gameId).first() ?: return null
            val set = snapshot.currentSet
            return CurrentSet(snapshot.config, set.index, set.lineup, set.log)
        }

        override suspend fun startNextSet(gameId: String, lineup: List<TeamLineup>) {
            val index = currentSet(gameId)?.index?.plus(1) ?: return
            dao.insertSet(GameSetEntity(gameId, index, json.encodeToString(lineup), time.now()))
        }

        override fun observeOngoingGameId(): Flow<String?> = dao.observeOngoing().map { it?.id }

        override suspend fun close(gameId: String) = dao.close(gameId, time.now())

        override suspend fun reopen(gameId: String) = dao.reopen(gameId, time.now())

        override suspend fun delete(gameId: String) = dao.deleteGame(gameId)
    }

/** JSON du journal et des configurations (champ `type` comme discriminant des événements). */
val MolkkyJson: Json = Json { ignoreUnknownKeys = true }
