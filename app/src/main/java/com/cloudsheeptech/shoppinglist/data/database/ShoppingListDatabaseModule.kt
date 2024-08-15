package com.cloudsheeptech.shoppinglist.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ShoppingListDatabaseModule {

    @Singleton
    @Provides
    fun provideShoppingListDatabase(@ApplicationContext context: Context) : ShoppingListDatabase {
        return Room.databaseBuilder(
            context.applicationContext, ShoppingListDatabase::class.java, "shopping_list_database"
        ).build()
    }

}