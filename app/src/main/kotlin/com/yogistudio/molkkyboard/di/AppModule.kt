package com.yogistudio.molkkyboard.di

import com.yogistudio.molkkyboard.BuildConfig
import com.yogistudio.molkkyboard.core.data.AppInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun appInfo(): AppInfo = AppInfo { "Molkky Board ${BuildConfig.VERSION_NAME}" }
}
