package com.yogistudio.molkkyboard.core.data.di

import com.yogistudio.molkkyboard.core.data.DataStoreSettingsRepository
import com.yogistudio.molkkyboard.core.data.GameRepository
import com.yogistudio.molkkyboard.core.data.IdGenerator
import com.yogistudio.molkkyboard.core.data.RoomGameRepository
import com.yogistudio.molkkyboard.core.data.SettingsRepository
import com.yogistudio.molkkyboard.core.data.TimeSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.UUID

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun gameRepository(impl: RoomGameRepository): GameRepository

    @Binds
    abstract fun settingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    companion object {
        @Provides
        fun timeSource(): TimeSource = TimeSource { System.currentTimeMillis() }

        @Provides
        fun idGenerator(): IdGenerator = IdGenerator { UUID.randomUUID().toString() }
    }
}
