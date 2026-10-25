package com.yogistudio.molkkyboard.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yogistudio.molkkyboard.core.database.dao.GameDao
import com.yogistudio.molkkyboard.core.database.dao.PlayerDao
import com.yogistudio.molkkyboard.core.database.entity.GameEntity
import com.yogistudio.molkkyboard.core.database.entity.GameEventEntity
import com.yogistudio.molkkyboard.core.database.entity.GamePlayerEntity
import com.yogistudio.molkkyboard.core.database.entity.GameSetEntity
import com.yogistudio.molkkyboard.core.database.entity.GameTeamEntity
import com.yogistudio.molkkyboard.core.database.entity.PlayerProfileEntity
import com.yogistudio.molkkyboard.core.database.entity.TeamEntity

@Database(
    entities = [
        PlayerProfileEntity::class,
        TeamEntity::class,
        GameEntity::class,
        GameSetEntity::class,
        GameTeamEntity::class,
        GamePlayerEntity::class,
        GameEventEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MolkkyBoardDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    abstract fun playerDao(): PlayerDao

    companion object {
        const val NAME = "molkkyboard.db"
    }
}
