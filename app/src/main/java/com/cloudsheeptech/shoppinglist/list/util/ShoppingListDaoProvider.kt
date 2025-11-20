package com.cloudsheeptech.shoppinglist.list.util

import com.cloudsheeptech.shoppinglist.database.ShoppingListDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ShoppingListDaoProvider {
    @Provides
    @Singleton
    fun provideShoppingListDao(database: ShoppingListDatabase) = database.shoppingListDao()

    @Provides
    @Singleton
    fun provideShoppingListOperationDao(database: ShoppingListDatabase) = database.pendingListOperationDao()
}
