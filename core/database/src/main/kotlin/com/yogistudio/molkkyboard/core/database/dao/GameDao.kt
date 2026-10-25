package com.yogistudio.molkkyboard.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.yogistudio.molkkyboard.core.database.entity.GameEntity
import com.yogistudio.molkkyboard.core.database.entity.GameEventEntity
import com.yogistudio.molkkyboard.core.database.entity.GamePlayerEntity
import com.yogistudio.molkkyboard.core.database.entity.GameSetEntity
import com.yogistudio.molkkyboard.core.database.entity.GameTeamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Insert
    suspend fun insertGame(game: GameEntity)

    @Insert
    suspend fun insertSet(set: GameSetEntity)

    @Insert
    suspend fun insertTeams(teams: List<GameTeamEntity>)

    @Insert
    suspend fun insertPlayers(players: List<GamePlayerEntity>)

    @Transaction
    suspend fun createGame(
        game: GameEntity,
        firstSet: GameSetEntity,
        teams: List<GameTeamEntity>,
        players: List<GamePlayerEntity> = emptyList(),
    ) {
        insertGame(game)
        insertSet(firstSet)
        insertTeams(teams)
        insertPlayers(players)
    }

    @Query("SELECT * FROM game_player WHERE gameId = :gameId")
    fun observePlayers(gameId: String): Flow<List<GamePlayerEntity>>

    @Query("SELECT * FROM game WHERE id = :id")
    fun observeGame(id: String): Flow<GameEntity?>

    @Query("SELECT * FROM game_set WHERE gameId = :gameId ORDER BY setIndex")
    fun observeSets(gameId: String): Flow<List<GameSetEntity>>

    @Query("SELECT * FROM game_team WHERE gameId = :gameId")
    fun observeTeams(gameId: String): Flow<List<GameTeamEntity>>

    @Query("SELECT * FROM game_event WHERE gameId = :gameId ORDER BY setIndex, localSeq")
    fun observeEvents(gameId: String): Flow<List<GameEventEntity>>

    @Query("SELECT * FROM game_event WHERE gameId = :gameId AND setIndex = :setIndex ORDER BY localSeq")
    suspend fun events(gameId: String, setIndex: Int): List<GameEventEntity>

    @Query("SELECT COALESCE(MAX(localSeq), 0) FROM game_event WHERE gameId = :gameId AND setIndex = :setIndex")
    suspend fun lastSeq(gameId: String, setIndex: Int): Long

    @Insert
    suspend fun insertEvent(event: GameEventEntity)

    @Query("UPDATE game SET updatedAt = :at WHERE id = :gameId")
    suspend fun touch(gameId: String, at: Long)

    /**
     * Ajoute un événement en fin de journal, dans une transaction courte : l'événement est écrit avant que
     * l'interface n'avance, aucune partie n'est perdue si l'application est tuée juste après.
     */
    @Transaction
    suspend fun appendEvent(event: GameEventEntity): GameEventEntity {
        val stored = event.copy(localSeq = lastSeq(event.gameId, event.setIndex) + 1)
        insertEvent(stored)
        touch(event.gameId, event.createdAt)
        return stored
    }

    /** Partie en cours la plus récente (non close), à proposer en reprise à l'accueil. */
    @Query("SELECT * FROM game WHERE closedAt IS NULL ORDER BY updatedAt DESC LIMIT 1")
    fun observeOngoing(): Flow<GameEntity?>

    @Query("SELECT * FROM game WHERE closedAt IS NOT NULL ORDER BY closedAt DESC")
    fun observeClosed(): Flow<List<GameEntity>>

    @Query("UPDATE game SET closedAt = :at, updatedAt = :at WHERE id = :gameId")
    suspend fun close(gameId: String, at: Long)

    @Query("UPDATE game SET closedAt = NULL, updatedAt = :at WHERE id = :gameId")
    suspend fun reopen(gameId: String, at: Long)

    @Query("DELETE FROM game WHERE id = :gameId")
    suspend fun deleteGame(gameId: String)
}
