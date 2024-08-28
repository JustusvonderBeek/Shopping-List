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
import androidx.room.migration.Migration
import com.cloudsheeptech.shoppinglist.data.items.DbItem
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ListMapping
import com.cloudsheeptech.shoppinglist.data.sharing.ListShareDatabase
import com.cloudsheeptech.shoppinglist.data.list.DbShoppingList
import com.cloudsheeptech.shoppinglist.data.onlineUser.ListCreator
import com.cloudsheeptech.shoppinglist.data.UIPreference
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ItemListMappingDao
import com.cloudsheeptech.shoppinglist.data.items.ItemDao
import com.cloudsheeptech.shoppinglist.data.onlineUser.OnlineUserDao
import com.cloudsheeptech.shoppinglist.data.receipt.DbReceipt
import com.cloudsheeptech.shoppinglist.data.receipt.ReceiptDao
import com.cloudsheeptech.shoppinglist.data.receiptItemAndDescriptionMapping.ReceiptDescriptionDao
import com.cloudsheeptech.shoppinglist.data.receiptItemAndDescriptionMapping.ReceiptDescriptionMapping
import com.cloudsheeptech.shoppinglist.data.receiptItemAndDescriptionMapping.ReceiptItemDao
import com.cloudsheeptech.shoppinglist.data.receiptItemAndDescriptionMapping.ReceiptItemMapping
import com.cloudsheeptech.shoppinglist.data.sharing.SharedDao
import com.cloudsheeptech.shoppinglist.data.typeConverter.DatabaseTypeConverter
import com.cloudsheeptech.shoppinglist.data.user.AppUser
import com.cloudsheeptech.shoppinglist.data.user.AppUserDao
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Singleton
@Database(
    version = 24,
    entities = [DbShoppingList::class, DbItem::class, ListMapping::class, AppUser::class, ListCreator::class, ListShareDatabase::class, UIPreference::class, DbReceipt::class, ReceiptDescriptionMapping::class, ReceiptItemMapping::class],
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 18, to = 19),
        AutoMigration(from = 19, to = 20, ShoppingListDatabase.Database19To20Migration::class),
        AutoMigration(from = 20, to = 21, ShoppingListDatabase.Database20To21Migration::class),
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
    abstract fun receiptDao() : ReceiptDao
    abstract fun receiptDescriptionDao() : ReceiptDescriptionDao
    abstract fun receiptItemDao() : ReceiptItemDao

    companion object {
        const val LATEST_VERSION = 24

        @Volatile
        private  var INSTANCE : ShoppingListDatabase? = null

        fun getInstance(@ApplicationContext context : Context) : ShoppingListDatabase {
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
            toColumnName = "createdBy"
        ),
        RenameColumn(
            tableName = "list_table",
            fromColumnName = "ID",
            toColumnName = "listId",
        ),
        RenameColumn(
            tableName = "list_table",
            fromColumnName = "Name",
            toColumnName = "title",
        ),
        RenameColumn(
            tableName = "list_table",
            fromColumnName = "CreatedByName",
            toColumnName = "createdByName",
        ),
        RenameColumn(
            tableName = "list_table",
            fromColumnName = "LastEdited",
            toColumnName = "lastUpdated",
        ),
        RenameColumn(
            tableName = "items",
            fromColumnName = "ID",
            toColumnName = "id",
        ),
        RenameColumn(
            tableName = "items",
            fromColumnName = "Name",
            toColumnName = "name",
        ),
        RenameColumn(
            tableName = "items",
            fromColumnName = "Icon",
            toColumnName = "icon",
        ),
        RenameColumn(
            tableName = "user",
            fromColumnName = "UserId",
            toColumnName = "OnlineID"
        )
    )
    class Database19To20Migration : AutoMigrationSpec

    @RenameColumn.Entries(
        RenameColumn(
            tableName = "online_user",
            fromColumnName = "ID",
            toColumnName = "onlineId",
        ),
        RenameColumn(
            tableName = "online_user",
            fromColumnName = "Name",
            toColumnName = "username",
        ),
    )
    class Database20To21Migration : AutoMigrationSpec
}

