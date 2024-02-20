package com.cloudsheeptech.shoppinglist.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.data.ListMapping
import com.cloudsheeptech.shoppinglist.data.ListShareDatabase
import com.cloudsheeptech.shoppinglist.data.ShoppingList
import com.cloudsheeptech.shoppinglist.data.DatabaseUser
import com.cloudsheeptech.shoppinglist.data.UserWire

@Database(entities = [ShoppingList::class, Item::class, ListMapping::class, DatabaseUser::class, UserWire::class, ListShareDatabase::class], version=14, exportSchema = false)
@TypeConverters(value = [DatabaseTypeConverter::class])
abstract class ShoppingListDatabase : RoomDatabase() {

    abstract fun shoppingListDao() : ShoppingListDao
    abstract fun itemDao() : ItemDao
    abstract fun mappingDao() : ItemListMappingDao
    abstract fun userDao() : UserDao
    abstract fun onlineUserDao() : OnlineUserDao
    abstract fun sharedDao() : SharedDao

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