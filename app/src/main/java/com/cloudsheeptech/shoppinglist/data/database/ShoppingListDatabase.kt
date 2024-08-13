package com.cloudsheeptech.shoppinglist.data.database

import android.content.Context
import android.util.Log
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import com.cloudsheeptech.shoppinglist.data.items.Item
import com.cloudsheeptech.shoppinglist.data.ListMapping
import com.cloudsheeptech.shoppinglist.data.ListShareDatabase
import com.cloudsheeptech.shoppinglist.data.DbShoppingList
import com.cloudsheeptech.shoppinglist.data.ListCreator
import com.cloudsheeptech.shoppinglist.data.UIPreference
import com.cloudsheeptech.shoppinglist.data.user.AppUser
import com.cloudsheeptech.shoppinglist.data.user.AppUserDao

@Database(
    version = 20,
    entities = [DbShoppingList::class, Item::class, ListMapping::class, AppUser::class, ListCreator::class, ListShareDatabase::class, UIPreference::class],
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 18, to = 19),
        AutoMigration(from = 19, to = 20, ShoppingListDatabase.Database19To20Migration::class),
    ],
)
@TypeConverters(value = [DatabaseTypeConverter::class])
abstract class ShoppingListDatabase : RoomDatabase() {

    abstract fun shoppingListDao() : ShoppingListDao
    abstract fun itemDao() : ItemDao
    abstract fun mappingDao() : ItemListMappingDao
    abstract fun userDao() : AppUserDao
    abstract fun onlineUserDao() : OnlineUserDao
    abstract fun sharedDao() : SharedDao
    abstract fun preferenceDao() : UIPreferencesDao

    companion object {
        const val LATEST_VERSION = 20

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

    @RenameColumn.Entries(
        RenameColumn(
            tableName = "list_table",
            fromColumnName = "CreatedBy",
            toColumnName = "CreatedByID"
        ),
        RenameColumn(
            tableName = "user",
            fromColumnName = "UserId",
            toColumnName = "OnlineID"
        )
    )
    class Database19To20Migration : AutoMigrationSpec
}

