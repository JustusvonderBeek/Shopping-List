package com.cloudsheeptech.shoppinglist.list.util

import com.cloudsheeptech.shoppinglist.list.api.AppApiProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ShoppingListApiProvider {
    @Provides
    @Singleton
    fun provideShoppingListApi(appApiProvider: AppApiProvider) = appApiProvider.shoppingListApi
}
