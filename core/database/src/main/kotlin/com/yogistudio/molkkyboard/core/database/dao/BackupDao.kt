package com.yogistudio.molkkyboard.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.yogistudio.molkkyboard.core.database.entity.GameEntity
import com.yogistudio.molkkyboard.core.database.entity.GameEventEntity
import com.yogistudio.molkkyboard.core.database.entity.GamePlayerEntity
import com.yogistudio.molkkyboard.core.database.entity.GameSetEntity
import com.yogistudio.molkkyboard.core.database.entity.GameTeamEntity
import com.yogistudio.molkkyboard.core.database.entity.MolkkoutThrowEntity
import com.yogistudio.molkkyboard.core.database.entity.PlayerProfileEntity
import com.yogistudio.molkkyboard.core.database.entity.TeamEntity

/** Toutes les données locales en bloc : sauvegarde, restauration et suppression complète. */
@Suppress("TooManyFunctions")
@Dao
interface BackupDao {
    @Query("SELECT * FROM player_profile ORDER BY createdAt")
    suspend fun profiles(): List<PlayerProfileEntity>

    @Query("SELECT * FROM team ORDER BY createdAt")
    suspend fun teams(): List<TeamEntity>

    @Query("SELECT * FROM game ORDER BY createdAt")
    suspend fun games(): List<GameEntity>

    @Query("SELECT * FROM game_set ORDER BY gameId, setIndex")
    suspend fun sets(): List<GameSetEntity>

    @Query("SELECT * FROM game_team")
    suspend fun gameTeams(): List<GameTeamEntity>

    @Query("SELECT * FROM game_player")
    suspend fun gamePlayers(): List<GamePlayerEntity>

    @Query("SELECT * FROM game_event ORDER BY gameId, setIndex, localSeq")
    suspend fun events(): List<GameEventEntity>

    @Query("SELECT * FROM molkkout_throw ORDER BY gameId, seq")
    suspend fun molkkout(): List<MolkkoutThrowEntity>

    @Query("SELECT id FROM game")
    suspend fun gameIds(): List<String>

    @Query("SELECT id FROM game_event")
    suspend fun eventIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProfiles(profiles: List<PlayerProfileEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTeams(teams: List<TeamEntity>)

    @Insert
    suspend fun insertGames(games: List<GameEntity>)

    @Insert
    suspend fun insertSets(sets: List<GameSetEntity>)

    @Insert
    suspend fun insertGameTeams(teams: List<GameTeamEntity>)

    @Insert
    suspend fun insertGamePlayers(players: List<GamePlayerEntity>)

    @Insert
    suspend fun insertEvents(events: List<GameEventEntity>)

    @Insert
    suspend fun insertMolkkout(throws: List<MolkkoutThrowEntity>)

    /**
     * Ajoute le contenu d'une sauvegarde, en une seule transaction (tout ou rien). Les profils et équipes déjà
     * présents sont conservés tels quels ; l'appelant écarte les parties déjà présentes.
     */
    @Suppress("LongParameterList")
    @Transaction
    suspend fun restore(
        profiles: List<PlayerProfileEntity>,
        teams: List<TeamEntity>,
        games: List<GameEntity>,
        sets: List<GameSetEntity>,
        gameTeams: List<GameTeamEntity>,
        gamePlayers: List<GamePlayerEntity>,
        events: List<GameEventEntity>,
        molkkout: List<MolkkoutThrowEntity>,
    ) {
        insertProfiles(profiles)
        insertTeams(teams)
        insertGames(games)
        insertSets(sets)
        insertGameTeams(gameTeams)
        insertGamePlayers(gamePlayers)
        insertEvents(events)
        insertMolkkout(molkkout)
    }

    @Query("DELETE FROM game")
    suspend fun deleteGames()

    @Query("DELETE FROM player_profile")
    suspend fun deleteProfiles()

    @Query("DELETE FROM team")
    suspend fun deleteTeams()

    /** Supprime toutes les parties (les tables liées suivent par cascade), les profils et les équipes. */
    @Transaction
    suspend fun deleteAll() {
        deleteGames()
        deleteProfiles()
        deleteTeams()
    }
}
