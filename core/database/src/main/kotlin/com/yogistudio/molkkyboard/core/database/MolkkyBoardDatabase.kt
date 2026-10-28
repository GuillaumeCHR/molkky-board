package com.yogistudio.molkkyboard.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yogistudio.molkkyboard.core.database.dao.BackupDao
import com.yogistudio.molkkyboard.core.database.dao.GameDao
import com.yogistudio.molkkyboard.core.database.dao.PlayerDao
import com.yogistudio.molkkyboard.core.database.entity.GameEntity
import com.yogistudio.molkkyboard.core.database.entity.GameEventEntity
import com.yogistudio.molkkyboard.core.database.entity.GamePlayerEntity
import com.yogistudio.molkkyboard.core.database.entity.GameSetEntity
import com.yogistudio.molkkyboard.core.database.entity.GameTeamEntity
import com.yogistudio.molkkyboard.core.database.entity.MolkkoutThrowEntity
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
        MolkkoutThrowEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class MolkkyBoardDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    abstract fun playerDao(): PlayerDao

    abstract fun backupDao(): BackupDao

    companion object {
        const val NAME = "molkkyboard.db"

        /** Toutes les migrations, dans l'ordre : toute modification du schéma impose une version et une migration. */
        val MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
    }
}
