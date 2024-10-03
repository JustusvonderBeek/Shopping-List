package com.cloudsheeptech.shoppinglist.data.recipe

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.items.ItemLocalDataSource
import com.cloudsheeptech.shoppinglist.data.items.ItemRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUserLocalDataSource
import com.cloudsheeptech.shoppinglist.data.user.AppUserRemoteDataSource
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.network.Networking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RecipeOfflineTest {

    private fun createRecipeLocalDataSource() : RecipeLocalDataSource {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(context)
        val localUserRepository = AppUserLocalDataSource(database)
        val networking = Networking("token.txt")
        val remoteUserRepository = AppUserRemoteDataSource(networking)
        val appUserRepository = AppUserRepository(localUserRepository, remoteUserRepository)
        val itemLocalDataSource = ItemLocalDataSource(database)
        val itemRepository = ItemRepository(itemLocalDataSource)
        val recipeLocalDataSource = RecipeLocalDataSource(database, appUserRepository, itemRepository)
        return recipeLocalDataSource
    }

    private fun createNIngredients(items: Int) : List<ApiIngredient> {
        val mutableIngredientList = mutableListOf<ApiIngredient>()
        for (i : Int in 1..items) {
            mutableIngredientList.add(
                ApiIngredient(
                0L,
                "Item $i",
                "Icon $i",
                i,
                "g"
            ))
        }
        return mutableIngredientList
    }

    @Test
    public fun testCreateRecipe() = runTest {
        val localRecipeDS = createRecipeLocalDataSource()
        val recipe = localRecipeDS.create("New recipe", "new icon")

        val ingredients = createNIngredients(3)
        recipe.ingredients = ingredients
    }

}