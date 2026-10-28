package com.yogistudio.molkkyboard.core.data

import com.yogistudio.molkkyboard.core.database.dao.GameDao
import com.yogistudio.molkkyboard.core.database.entity.GameEntity
import com.yogistudio.molkkyboard.core.database.entity.GameEventEntity
import com.yogistudio.molkkyboard.core.database.entity.GamePlayerEntity
import com.yogistudio.molkkyboard.core.database.entity.GameSetEntity
import com.yogistudio.molkkyboard.core.database.entity.GameTeamEntity
import com.yogistudio.molkkyboard.core.database.entity.MolkkoutThrowEntity
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.scoring.AddPlayer
import com.yogistudio.molkkyboard.core.scoring.MatchConfig
import com.yogistudio.molkkyboard.core.scoring.MatchStatus
import com.yogistudio.molkkyboard.core.scoring.Molkkout
import com.yogistudio.molkkyboard.core.scoring.MolkkoutState
import com.yogistudio.molkkyboard.core.scoring.MolkkoutThrow
import com.yogistudio.molkkyboard.core.scoring.NextStarter
import com.yogistudio.molkkyboard.core.scoring.Outcome
import com.yogistudio.molkkyboard.core.scoring.PlayEvent
import com.yogistudio.molkkyboard.core.scoring.ReplayResult
import com.yogistudio.molkkyboard.core.scoring.ScoringError
import com.yogistudio.molkkyboard.core.scoring.SetEvent
import com.yogistudio.molkkyboard.core.scoring.getOrThrow
import com.yogistudio.molkkyboard.core.scoring.matchStatus
import com.yogistudio.molkkyboard.core.scoring.replay
import com.yogistudio.molkkyboard.core.scoring.toResult
import com.yogistudio.molkkyboard.core.sync.Author
import com.yogistudio.molkkyboard.core.sync.SetLog
import com.yogistudio.molkkyboard.core.sync.Validation
import com.yogistudio.molkkyboard.core.sync.validate
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
     * Valide [event] (fonction partagée `validate` de `core:sync` : droits de [author], tour, moteur) puis l'écrit en
     * fin de journal de la manche en cours. Rien n'est écrit si l'événement est refusé : l'erreur est renvoyée.
     */
    suspend fun append(gameId: String, event: SetEvent, author: Author = Author.Local): Outcome<ReplayResult>

    suspend fun startNextSet(gameId: String, lineup: List<TeamLineup>)

    /**
     * Un joueur rejoint la partie en cours : l'événement `AddPlayer` est validé et écrit, puis son nom (et son
     * équipe si elle est nouvelle) sont enregistrés.
     */
    suspend fun addParticipant(
        gameId: String,
        event: AddPlayer,
        player: GamePlayer,
        team: GameTeam,
    ): Outcome<ReplayResult>

    /** Parties closes (terminées ou abandonnées), la plus récente d'abord : historique et statistiques. */
    fun observeHistory(): Flow<List<GameSnapshot>>

    /** Ajoute un lancer de Mölkkout, validé par le moteur ; rien n'est écrit s'il est refusé. */
    suspend fun appendMolkkout(gameId: String, throwEvent: MolkkoutThrow): Outcome<MolkkoutState>

    /** Annule le dernier lancer de Mölkkout. */
    suspend fun undoMolkkout(gameId: String)

    /**
     * Revanche : nouvelle partie avec les mêmes équipes, joueurs et règles. L'ordre de passage découle de la
     * dernière manche selon [order]. Renvoie l'identifiant de la nouvelle partie.
     */
    suspend fun createRematch(gameId: String, order: NextStarter): String?

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
                combine(dao.observeGame(gameId), dao.observeSets(gameId), dao.observeTeams(gameId), ::Triple),
                dao.observePlayers(gameId),
                dao.observeEvents(gameId),
                dao.observeMolkkout(gameId),
            ) { (game, sets, teams), players, events, molkkout ->
                game ?: return@combine null
                snapshot(game, sets, teams, players, events, molkkout)
            }

        override fun observeHistory(): Flow<List<GameSnapshot>> =
            combine(
                dao.observeClosed(),
                dao.observeClosedSets(),
                dao.observeClosedTeams(),
                dao.observeClosedPlayers(),
                dao.observeClosedEvents(),
            ) { games, sets, teams, players, events ->
                val setsBy = sets.groupBy { it.gameId }
                val teamsBy = teams.groupBy { it.gameId }
                val playersBy = players.groupBy { it.gameId }
                val eventsBy = events.groupBy { it.gameId }
                games.mapNotNull { game ->
                    val gameSets = setsBy[game.id] ?: return@mapNotNull null
                    // Le Mölkkout ne sert qu'à l'ordre de la belle : inutile à l'historique.
                    runCatching {
                        snapshot(
                            game,
                            gameSets,
                            teamsBy[game.id].orEmpty(),
                            playersBy[game.id].orEmpty(),
                            eventsBy[game.id].orEmpty(),
                            emptyList(),
                        )
                    }.getOrNull()
                }
            }

        private fun snapshot(
            game: GameEntity,
            sets: List<GameSetEntity>,
            teams: List<GameTeamEntity>,
            players: List<GamePlayerEntity>,
            events: List<GameEventEntity>,
            molkkout: List<MolkkoutThrowEntity>,
        ): GameSnapshot {
            val config = json.decodeFromString<GameConfig>(game.configJson)
            val bySet = events.groupBy { it.setIndex }
            return GameSnapshot(
                id = game.id,
                createdAt = game.createdAt,
                closedAt = game.closedAt,
                config = config,
                matchConfig = json.decodeFromString<MatchConfig>(game.matchConfigJson),
                teams = teams.map { GameTeam(TeamId(it.teamId), it.name, it.colorIndex, it.emblem, it.memberKey) },
                sets =
                    sets.map { set ->
                        val lineup = json.decodeFromString<List<TeamLineup>>(set.lineupJson)
                        val log = bySet[set.setIndex].orEmpty().map { json.decodeFromString<SetEvent>(it.payloadJson) }
                        GameSetSnapshot(set.setIndex, lineup, log, replay(config, lineup, log).getOrThrow())
                    },
                place = game.place,
                players = players.map { GamePlayer(PlayerId(it.playerId), it.name, it.profileId) },
                molkkout = molkkout.map { json.decodeFromString<MolkkoutThrow>(it.payloadJson) },
            )
        }

        override suspend fun append(gameId: String, event: SetEvent, author: Author): Outcome<ReplayResult> =
            writes.withLock {
                val snapshot = currentSet(gameId) ?: return Outcome.Err(ScoringError.UNKNOWN_EVENT)
                val config = snapshot.config
                val lineup = snapshot.lineup
                val log = snapshot.log
                // Même porte que l'hôte d'une partie à côté : la validation partagée de `core:sync`.
                val after =
                    when (val validation = validate(SetLog(config, lineup, log), event, author)) {
                        is Validation.Accepted -> validation.result
                        is Validation.Rejected -> return Outcome.Err(validation.error ?: ScoringError.INVALID_EVENT)
                    }
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

        override suspend fun addParticipant(
            gameId: String,
            event: AddPlayer,
            player: GamePlayer,
            team: GameTeam,
        ): Outcome<ReplayResult> {
            val outcome = append(gameId, event)
            if (outcome is Outcome.Ok) {
                dao.insertTeamIfAbsent(
                    GameTeamEntity(gameId, team.id.value, team.name, team.colorIndex, team.emblem, team.memberKey),
                )
                dao.upsertPlayer(GamePlayerEntity(gameId, player.id.value, player.name, player.profileId))
            }
            return outcome
        }

        override suspend fun appendMolkkout(gameId: String, throwEvent: MolkkoutThrow): Outcome<MolkkoutState> =
            writes.withLock {
                val snapshot = observe(gameId).first() ?: return Outcome.Err(ScoringError.UNKNOWN_EVENT)
                val teams = snapshot.molkkoutTeams() ?: return Outcome.Err(ScoringError.INVALID_CONFIG)
                val after =
                    when (val before = Molkkout.replay(teams, snapshot.molkkout)) {
                        is Outcome.Ok -> Molkkout.reduce(before.value, throwEvent)
                        is Outcome.Err -> before
                    }
                if (after is Outcome.Ok) {
                    dao.insertMolkkoutThrow(
                        MolkkoutThrowEntity(gameId, snapshot.molkkout.size + 1, json.encodeToString(throwEvent)),
                    )
                }
                after
            }

        override suspend fun undoMolkkout(gameId: String) = writes.withLock { dao.deleteLastMolkkoutThrow(gameId) }

        override suspend fun createRematch(gameId: String, order: NextStarter): String? {
            val snapshot = observe(gameId).first() ?: return null
            val last = snapshot.currentSet.replay.state
            val result = last.toResult()
            val remaining = last.teams.filterNot { it.left }
            val ids = remaining.map { it.id }
            val nextIds =
                if (result == null) {
                    ids
                } else {
                    val next = matchStatus(MatchConfig(setsToWin = 2, nextStarter = order), ids, listOf(result))
                    (next as? MatchStatus.NextSet)?.order ?: ids
                }
            val lineup = nextIds.map { id -> TeamLineup(id, remaining.first { it.id == id }.players) }
            val playerIds = lineup.flatMap { it.players }.toSet()
            return create(
                NewGame(
                    config = snapshot.config,
                    matchConfig = snapshot.matchConfig,
                    lineup = lineup,
                    teams = snapshot.teams.filter { team -> team.id in nextIds },
                    players = snapshot.players.filter { it.id in playerIds },
                    place = snapshot.place,
                ),
            )
        }

        override fun observeOngoingGameId(): Flow<String?> = dao.observeOngoing().map { it?.id }

        override suspend fun close(gameId: String) = dao.close(gameId, time.now())

        override suspend fun reopen(gameId: String) = dao.reopen(gameId, time.now())

        override suspend fun delete(gameId: String) = dao.deleteGame(gameId)
    }

/** JSON du journal et des configurations (champ `type` comme discriminant des événements). */
val MolkkyJson: Json = Json { ignoreUnknownKeys = true }
