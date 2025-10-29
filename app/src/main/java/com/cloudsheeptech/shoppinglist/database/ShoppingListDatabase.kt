package com.cloudsheeptech.shoppinglist.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cloudsheeptech.shoppinglist.list.dao.ItemDao
import com.cloudsheeptech.shoppinglist.list.dao.ItemListMappingDao
import com.cloudsheeptech.shoppinglist.list.dao.SharedDao
import com.cloudsheeptech.shoppinglist.list.dao.ShoppingListDao
import com.cloudsheeptech.shoppinglist.list.model.DbItem
import com.cloudsheeptech.shoppinglist.list.model.DbListShare
import com.cloudsheeptech.shoppinglist.list.model.DbShoppingList
import com.cloudsheeptech.shoppinglist.list.model.ItemToList
import com.cloudsheeptech.shoppinglist.list.model.ListCreator
import com.cloudsheeptech.shoppinglist.recipe.dao.ReceiptDescriptionDao
import com.cloudsheeptech.shoppinglist.recipe.dao.ReceiptItemDao
import com.cloudsheeptech.shoppinglist.recipe.dao.RecipeDao
import com.cloudsheeptech.shoppinglist.recipe.dao.RecipeImageDao
import com.cloudsheeptech.shoppinglist.recipe.model.DbRecipe
import com.cloudsheeptech.shoppinglist.recipe.model.ReceiptDescriptionMapping
import com.cloudsheeptech.shoppinglist.recipe.model.ReceiptItemMapping
import com.cloudsheeptech.shoppinglist.recipe.model.RecipeImage
import com.cloudsheeptech.shoppinglist.sharing.dao.OnlineUserDao
import com.cloudsheeptech.shoppinglist.sharing.dao.RecipeShareDao
import com.cloudsheeptech.shoppinglist.sharing.model.RecipeShare
import com.cloudsheeptech.shoppinglist.ui.uiPreference.UIPreference
import com.cloudsheeptech.shoppinglist.ui.uiPreference.UIPreferencesDao
import com.cloudsheeptech.shoppinglist.user.dao.AppUserDao
import com.cloudsheeptech.shoppinglist.user.model.AppUser
import com.cloudsheeptech.shoppinglist.util.DatabaseTypeConverter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Singleton
@Database(
    version = 37,
    entities = [
        DbShoppingList::class, DbItem::class, ItemToList::class, AppUser::class,
        ListCreator::class, DbListShare::class, UIPreference::class, DbRecipe::class,
        ReceiptDescriptionMapping::class, ReceiptItemMapping::class, RecipeShare::class, RecipeImage::class,
    ],
    exportSchema = true,
)
@TypeConverters(value = [DatabaseTypeConverter::class])
abstract class ShoppingListDatabase : RoomDatabase() {
    abstract fun shoppingListDao(): ShoppingListDao

    abstract fun itemDao(): ItemDao

    abstract fun mappingDao(): ItemListMappingDao

    abstract fun userDao(): AppUserDao

    abstract fun onlineUserDao(): OnlineUserDao

    abstract fun sharedDao(): SharedDao

    abstract fun preferenceDao(): UIPreferencesDao

    abstract fun recipeDao(): RecipeDao

    abstract fun recipeDescriptionDao(): ReceiptDescriptionDao

    abstract fun recipeItemDao(): ReceiptItemDao

    abstract fun recipeShareDao(): RecipeShareDao

    abstract fun recipeImageDao(): RecipeImageDao

    companion object {
        const val LATEST_VERSION = 34

        @Suppress("ktlint:standard:property-naming")
        @Volatile
        private var INSTANCE: ShoppingListDatabase? = null

        fun getInstance(
            @ApplicationContext context: Context,
        ): ShoppingListDatabase {
            var instance = INSTANCE
            if (instance == null) {
                Log.i("ShoppingListDatabase", "Creating new database")
                instance =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            ShoppingListDatabase::class.java,
                            "shopping_list_database",
                        ).fallbackToDestructiveMigration()
                        .build()
                INSTANCE = instance
            }
            return instance
        }
    }
}
