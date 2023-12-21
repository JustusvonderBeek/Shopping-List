package com.cloudsheeptech.shoppinglist.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.data.ListMapping
import com.cloudsheeptech.shoppinglist.data.ShoppingList

@Database(entities = [ShoppingList::class, Item::class, ListMapping::class], version=2, exportSchema = false)
@TypeConverters(value = [DatabaseTypeConverter::class])
abstract class ShoppingListDatabase : RoomDatabase() {

    abstract fun shoppingListDao() : ShoppingListDao
    abstract fun itemListDao() : ItemListDao
    abstract fun mappingDao() : ItemListMappingDao

    companion object {
        @Volatile
        private  var INSTANCE : ShoppingListDatabase? = null

        fun getInstance(context : Context) : ShoppingListDatabase {
            var instance = INSTANCE
            if (instance == null) {
                Log.i("ShoppingListDatabase", "Creating new database")
                instance = Room.databaseBuilder(context.applicationContext, ShoppingListDatabase::class.java, "shopping_list_database").fallbackToDestructiveMigration().build()
                INSTANCE = instance
            }
            return instance
        }
    }

}