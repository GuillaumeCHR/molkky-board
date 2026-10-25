package com.yogistudio.molkkyboard.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.yogistudio.molkkyboard.core.database.entity.PlayerProfileEntity
import com.yogistudio.molkkyboard.core.database.entity.TeamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM player_profile WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeProfiles(): Flow<List<PlayerProfileEntity>>

    @Upsert
    suspend fun upsertProfile(profile: PlayerProfileEntity)

    @Query("UPDATE player_profile SET archived = 1 WHERE id = :id")
    suspend fun archiveProfile(id: String)

    @Query("SELECT * FROM team WHERE memberKey = :memberKey")
    suspend fun team(memberKey: String): TeamEntity?

    @Upsert
    suspend fun upsertTeam(team: TeamEntity)
}
