package com.yogistudio.molkkyboard.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Une base existante sur un téléphone doit toujours s'ouvrir après une mise à jour de l'app. */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), MolkkyBoardDatabase::class.java)

    private fun openMigrated(name: String): MolkkyBoardDatabase =
        Room
            .databaseBuilder(ApplicationProvider.getApplicationContext(), MolkkyBoardDatabase::class.java, name)
            .addMigrations(*MolkkyBoardDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()

    @Test
    fun `une base du J3 sans la table game_player s'ouvre apres migration`() =
        runTest {
            // Cas du plantage constaté sur téléphone : la table game_player n'existait pas encore.
            helper.createDatabase(DB, 1).apply {
                execSQL("DROP TABLE `game_player`")
                execSQL("INSERT INTO player_profile VALUES ('p1', 'Alice', 0, '🦊', NULL, 1, 0)")
                close()
            }
            helper.runMigrationsAndValidate(DB, 2, true, MIGRATION_1_2).close()
            val database = openMigrated(DB)
            assertEquals(
                listOf("Alice"),
                database
                    .playerDao()
                    .observeProfiles()
                    .first()
                    .map { it.name },
            )
            database.close()
        }

    @Test
    fun `une base recente qui a deja la table game_player migre aussi`() {
        helper.createDatabase(DB_RECENT, 1).close()
        helper.runMigrationsAndValidate(DB_RECENT, 2, true, MIGRATION_1_2).close()
    }

    private companion object {
        const val DB = "migration-test.db"
        const val DB_RECENT = "migration-recent.db"
    }
}
