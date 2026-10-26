package com.yogistudio.molkkyboard.feature.setup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SetupModule {
    @Binds
    abstract fun photoStore(impl: AvatarPhotos): PhotoStore
}
