package com.yogistudio.molkkyboard.core.nearby.di

import com.yogistudio.molkkyboard.core.data.NearbyRouter
import com.yogistudio.molkkyboard.core.nearby.NearbyManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** La partie à côté fournit le routage des saisies des parties partagées (voir `CommandGateway`). */
@Module
@InstallIn(SingletonComponent::class)
abstract class NearbyModule {
    @Binds
    abstract fun nearbyRouter(impl: NearbyManager): NearbyRouter
}
