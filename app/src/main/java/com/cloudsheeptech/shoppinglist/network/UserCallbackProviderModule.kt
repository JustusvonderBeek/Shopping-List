package com.cloudsheeptech.shoppinglist.network

import com.cloudsheeptech.shoppinglist.data.user.AppUserLocalDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UserCallbackProviderModule {
    @Provides
    @Singleton
    fun provideUserCreationPayloadProvider(localUserLocalDataSource: AppUserLocalDataSource): () -> String? {
    }
}
