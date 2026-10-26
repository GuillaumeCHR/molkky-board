package com.yogistudio.molkkyboard.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 1 → 2 : la table `game_player` (noms des joueurs d'une partie) a été ajoutée au J4 sans changer la version.
 * Une base du J3 ne l'a pas (plantage au lancement constaté sur téléphone) ; une base plus récente l'a déjà :
 * `IF NOT EXISTS` couvre les deux cas.
 */
val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `game_player` (`gameId` TEXT NOT NULL, `playerId` TEXT NOT NULL, " +
                    "`name` TEXT NOT NULL, `profileId` TEXT, PRIMARY KEY(`gameId`, `playerId`), " +
                    "FOREIGN KEY(`gameId`) REFERENCES `game`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
        }
    }
