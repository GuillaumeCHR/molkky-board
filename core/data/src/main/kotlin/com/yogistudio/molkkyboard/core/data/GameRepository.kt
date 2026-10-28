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
import com.yogistudio.molkkyboard.core.sync.CommandOutcome
import com.yogistudio.molkkyboard.core.sync.GameCommand
import com.yogistudio.molkkyboard.core.sync.RejectCode
import com.yogistudio.molkkyboard.core.sync.SetLog
import com.yogistudio.molkkyboard.core.sync.Validation
import com.yogistudio.molkkyboard.core.sync.allowedFor
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
    suspend fun append(
        gameId: String,
        event: SetEvent,
        author: Author = Author.Local,
        baseSeq: Int? = null,
    ): Outcome<ReplayResult>

    /**
     * Unique porte d'entrée des commandes ([GameCommand]) : droits de [author], idempotence (manche déjà ouverte,
     * lancer de Mölkkout déjà saisi, revanche déjà créée) et moteur, puis écriture. Celle qu'utilisera le téléphone
     * hôte d'une partie à côté pour les propositions des autres téléphones.
     */
    suspend fun apply(gameId: String, command: GameCommand, author: Author = Author.Local): CommandOutcome

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

        override suspend fun create(game: NewGame): String = insertGame(ids.newId(), game)

        private suspend fun insertGame(id: String, game: NewGame): String {
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

        override suspend fun append(
            gameId: String,
            event: SetEvent,
            author: Author,
            baseSeq: Int?,
        ): Outcome<ReplayResult> =
            when (val validation = write(gameId, event, author, baseSeq)) {
                is Validation.Accepted -> Outcome.Ok(validation.result)
                is Validation.Rejected -> Outcome.Err(validation.error ?: ScoringError.INVALID_EVENT)
            }

        /**
         * Même porte que l'hôte d'une partie à côté : la validation partagée de `core:sync`, puis l'écriture en fin
         * de journal. Le refus garde son motif (droits, fraîcheur, moteur).
         */
        private suspend fun write(
            gameId: String,
            event: SetEvent,
            author: Author,
            baseSeq: Int?,
        ): Validation =
            writes.withLock {
                val snapshot =
                    currentSet(gameId) ?: return Validation.Rejected(RejectCode.INVALID, ScoringError.UNKNOWN_EVENT)
                val log = snapshot.log
                val validation =
                    validate(
                        SetLog(snapshot.config, snapshot.lineup, log),
                        event,
                        author,
                        baseSeq ?: log.size,
                    )
                if (validation is Validation.Accepted) {
                    dao.appendEvent(
                        GameEventEntity(
                            id = event.id.value,
                            gameId = gameId,
                            setIndex = snapshot.index,
                            localSeq = 0,
                            createdAt = time.now(),
                            payloadJson = json.encodeToString(event),
                            authorDevice = author.deviceOrNull(),
                        ),
                    )
                }
                validation
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

        override suspend fun apply(gameId: String, command: GameCommand, author: Author): CommandOutcome {
            if (!command.allowedFor(author)) return CommandOutcome.Rejected(RejectCode.FORBIDDEN)
            return when (command) {
                is GameCommand.Play -> {
                    when (val validation = write(gameId, command.event, author, command.baseSeq)) {
                        is Validation.Accepted -> CommandOutcome.Applied
                        is Validation.Rejected -> CommandOutcome.Rejected(validation.code, validation.error)
                    }
                }

                is GameCommand.NextSet -> {
                    writes.withLock { nextSet(gameId, command.setIndex) }
                }

                is GameCommand.MolkkoutPlay -> {
                    writes.withLock { molkkoutPlay(gameId, command, author) }
                }

                is GameCommand.MolkkoutUndo -> {
                    writes.withLock { molkkoutUndo(gameId, command.seq) }
                }

                is GameCommand.Rematch -> {
                    rematch(gameId, command)
                }
            }
        }

        /** Manche suivante, dans l'ordre que le match impose ; une seule fois par numéro de manche. */
        private suspend fun nextSet(gameId: String, setIndex: Int): CommandOutcome {
            val snapshot = observe(gameId).first() ?: return invalid(ScoringError.UNKNOWN_EVENT)
            if (setIndex != snapshot.sets.size) return CommandOutcome.Rejected(RejectCode.STALE)
            val next = snapshot.match as? MatchStatus.NextSet ?: return invalid(ScoringError.INVALID_EVENT)
            val last = snapshot.currentSet.replay.state
            val lineup =
                next.order.mapNotNull { id ->
                    last.team(id)?.takeUnless { it.left }?.let { TeamLineup(id, it.players) }
                }
            dao.insertSet(GameSetEntity(gameId, setIndex, json.encodeToString(lineup), time.now()))
            return CommandOutcome.Applied
        }

        /** Lancer de Mölkkout : rang attendu, lanceur attendu, puis le moteur du Mölkkout. */
        private suspend fun molkkoutPlay(
            gameId: String,
            command: GameCommand.MolkkoutPlay,
            author: Author,
        ): CommandOutcome {
            val snapshot = observe(gameId).first() ?: return invalid(ScoringError.UNKNOWN_EVENT)
            val teams = snapshot.molkkoutTeams() ?: return invalid(ScoringError.INVALID_CONFIG)
            if (command.seq != snapshot.molkkout.size + 1) return CommandOutcome.Rejected(RejectCode.STALE)
            val before = Molkkout.replay(teams, snapshot.molkkout).getOrThrow()
            val expected =
                command.throwEvent.team == before.nextTeam?.id && command.throwEvent.player == before.nextPlayer
            val after =
                if (expected) Molkkout.reduce(before, command.throwEvent) else Outcome.Err(ScoringError.NOT_YOUR_TURN)
            if (after is Outcome.Err) return CommandOutcome.Rejected(after.error.toCode(), after.error)
            dao.insertMolkkoutThrow(
                MolkkoutThrowEntity(
                    gameId,
                    command.seq,
                    json.encodeToString(command.throwEvent),
                    author.deviceOrNull(),
                ),
            )
            return CommandOutcome.Applied
        }

        private suspend fun molkkoutUndo(gameId: String, seq: Int): CommandOutcome {
            val count = observe(gameId).first()?.molkkout?.size ?: 0
            if (seq < 1 || seq != count) return CommandOutcome.Rejected(RejectCode.STALE)
            dao.deleteLastMolkkoutThrow(gameId)
            return CommandOutcome.Applied
        }

        /**
         * Revanche : nouvelle partie avec les mêmes équipes, joueurs et règles ; l'ordre de passage découle de la
         * dernière manche selon l'ordre demandé. Une deuxième demande avec le même identifiant ne crée rien de plus.
         */
        private suspend fun rematch(gameId: String, command: GameCommand.Rematch): CommandOutcome {
            if (dao.observeGame(command.gameId).first() != null) return CommandOutcome.Applied
            val snapshot = observe(gameId).first() ?: return invalid(ScoringError.UNKNOWN_EVENT)
            val last = snapshot.currentSet.replay.state
            val result = last.toResult()
            val remaining = last.teams.filterNot { it.left }
            val ids = remaining.map { it.id }
            val nextIds =
                if (result == null) {
                    ids
                } else {
                    val config = MatchConfig(setsToWin = 2, nextStarter = command.order)
                    (matchStatus(config, ids, listOf(result)) as? MatchStatus.NextSet)?.order ?: ids
                }
            val lineup = nextIds.map { id -> TeamLineup(id, remaining.first { it.id == id }.players) }
            val playerIds = lineup.flatMap { it.players }.toSet()
            insertGame(
                command.gameId,
                NewGame(
                    config = snapshot.config,
                    matchConfig = snapshot.matchConfig,
                    lineup = lineup,
                    teams = snapshot.teams.filter { team -> team.id in nextIds },
                    players = snapshot.players.filter { it.id in playerIds },
                    place = snapshot.place,
                ),
            )
            return CommandOutcome.Applied
        }

        private fun invalid(error: ScoringError) = CommandOutcome.Rejected(RejectCode.INVALID, error)

        override fun observeOngoingGameId(): Flow<String?> = dao.observeOngoing().map { it?.id }

        override suspend fun close(gameId: String) = dao.close(gameId, time.now())

        override suspend fun reopen(gameId: String) = dao.reopen(gameId, time.now())

        override suspend fun delete(gameId: String) = dao.deleteGame(gameId)
    }

/** JSON du journal et des configurations (champ `type` comme discriminant des événements). */
val MolkkyJson: Json = Json { ignoreUnknownKeys = true }

private fun ScoringError.toCode(): RejectCode =
    if (this == ScoringError.NOT_YOUR_TURN) RejectCode.NOT_YOUR_TURN else RejectCode.INVALID

/** Le téléphone auteur à enregistrer : rien pour ce téléphone (partie locale). */
private fun Author.deviceOrNull(): String? = device.value.takeUnless { this == Author.Local }
