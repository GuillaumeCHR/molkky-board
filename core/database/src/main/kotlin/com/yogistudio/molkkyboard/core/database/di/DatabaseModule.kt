package com.yogistudio.molkkyboard.core.database.di

import android.content.Context
import androidx.room.Room
import com.yogistudio.molkkyboard.core.database.MolkkyBoardDatabase
import com.yogistudio.molkkyboard.core.database.dao.GameDao
import com.yogistudio.molkkyboard.core.database.dao.PlayerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): MolkkyBoardDatabase =
        Room.databaseBuilder(context, MolkkyBoardDatabase::class.java, MolkkyBoardDatabase.NAME).build()

    @Provides
    fun gameDao(database: MolkkyBoardDatabase): GameDao = database.gameDao()

    @Provides
    fun playerDao(database: MolkkyBoardDatabase): PlayerDao = database.playerDao()
}
