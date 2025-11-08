package com.cloudsheeptech.shoppinglist.recipe

import com.cloudsheeptech.shoppinglist.recipe.model.ApiIngredient
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RecipeOfflineTest {
//    private fun createRecipeLocalDataSource(): RecipeLocalDataSource {
//        val context = ApplicationProvider.getApplicationContext<Application>()
//        val database = ShoppingListDatabase.getInstance(context)
//        val localUserRepository = AppUserLocalDataSource(database)
//        val payloadProvider = UserCreationDataProvider(localUserRepository)
//        val tokenProvider = ShoppingListAuthenticationTokenProvider(payloadProvider, "tmp/")
//        val networking = Networking(tokenProvider)
//        val remoteUserRepository = AppUserRemoteDataSource(networking)
//        val appUserRepository = AppUserRepository(localUserRepository, remoteUserRepository)
//        val itemLocalDataSource = ItemLocalDataSource(database)
//        val itemRepository = ItemRepository(itemLocalDataSource)
//        val compressionHandler = CompressionHandler()
//        val binaryFileHandler = BinaryFileHandler(context, compressionHandler)
//        val recipeLocalDataSource =
//            RecipeLocalDataSource(database, appUserRepository, itemRepository, binaryFileHandler)
//        return recipeLocalDataSource
//    }

    private fun createNIngredients(items: Int): List<ApiIngredient> {
        val mutableIngredientList = mutableListOf<ApiIngredient>()
        for (i: Int in 1..items) {
            mutableIngredientList.add(
                ApiIngredient(
                    0L,
                    "Item $i",
                    "Icon $i",
                    i,
                    "g",
                ),
            )
        }
        return mutableIngredientList
    }

    @Test
    fun testCreateRecipe() =
        runTest {
//            val localRecipeDS = createRecipeLocalDataSource()
//            val recipe = localRecipeDS.create("New recipe", "new icon", 2)
//
//            val ingredients = createNIngredients(3)
//            recipe.ingredients = ingredients
        }
}
