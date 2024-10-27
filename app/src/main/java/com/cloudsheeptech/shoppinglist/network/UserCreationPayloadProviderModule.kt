package com.cloudsheeptech.shoppinglist.network

import com.cloudsheeptech.shoppinglist.data.user.AppUserLocalDataSource
import com.cloudsheeptech.shoppinglist.data.user.UserCreationDataProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UserCreationPayloadProviderModule {
    @Provides
    @Singleton
    fun provideUserCreationPayloadProvider(appUserLocalDataSource: AppUserLocalDataSource): UserCreationPayloadProvider =
        UserCreationDataProvider(appUserLocalDataSource)
}
