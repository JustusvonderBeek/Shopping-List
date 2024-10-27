package com.cloudsheeptech.shoppinglist.data.recipe

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.cloudsheeptech.shoppinglist.data.receipt.ApiDescription
import com.cloudsheeptech.shoppinglist.data.receipt.ApiIngredient
import com.cloudsheeptech.shoppinglist.data.receipt.ApiReceipt
import com.cloudsheeptech.shoppinglist.data.receipt.ReceiptRemoteDataSource
import com.cloudsheeptech.shoppinglist.data.typeConverter.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.data.user.ApiUser
import com.cloudsheeptech.shoppinglist.data.user.AppUser
import com.cloudsheeptech.shoppinglist.data.user.AppUserRemoteDataSource
import com.cloudsheeptech.shoppinglist.network.Networking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.OffsetDateTime

@RunWith(JUnit4::class)
class RecipeRemoteDataSourceTest {

    private fun AppUser.toApiUser() : ApiUser {
        return ApiUser(
            onlineId = this.OnlineID,
            username = this.Username,
            password = this.Password,
            created = this.Created,
            lastLogin = this.Created,
        )
    }

    private suspend fun createRecipeRemoteDataSource() : Pair<ReceiptRemoteDataSource, AppUser> {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val networking = Networking("token.txt")
        val remoteUserDs = AppUserRemoteDataSource(networking)
        val appUser = AppUser(OnlineID = 0L, Created = OffsetDateTime.now(), ID = 1L, Password = "secure", Username = "username")
        val remoteAppUser = remoteUserDs.create(appUser)
        val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = false
            serializersModule = SerializersModule {
                contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
            }
        }
        val encoded = json.encodeToString(remoteAppUser!!.toApiUser())
        networking.resetSerializedUser(encoded, remoteAppUser!!.OnlineID)
        val recipeRemoteDS = ReceiptRemoteDataSource(networking)
        return Pair(recipeRemoteDS, remoteAppUser)
    }

    private fun createNIngredients(n : Int) : List<ApiIngredient> {
        val ingredients = mutableListOf<ApiIngredient>()
        for (i : Int in 1..n) {
            val newIngredient = ApiIngredient(
                i.toLong(),
                "Ingredient $i",
                "Icon Ingredient $i",
                i,
                "g"
            )
            ingredients.add(newIngredient)
        }
        return ingredients
    }

    private fun createNDescription(n : Int) : List<ApiDescription> {
        val descriptions = mutableListOf<ApiDescription>()
        for (i: Int in 1..n) {
            val newDescription = ApiDescription(
                i,
                "Step $i",
            )
            descriptions.add(newDescription)
        }
        return descriptions
    }

    @Test
    public fun testCreateRecipe() = runTest{
        val (recipeRemoteDS, appUser) = createRecipeRemoteDataSource()
        val recipeId = 0L
        val ingredients = createNIngredients(3)
        val descriptions = createNDescription(3)
        val recipe = ApiReceipt(
            recipeId,
            "New recipe",
            appUser.OnlineID,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            ingredients,
            descriptions,
        )
        val success = recipeRemoteDS.create(recipe)
        assert(success)
    }

    @Test
    public fun testGetRecipe() = runTest {
        val (recipeRemoteDS, appUser) = createRecipeRemoteDataSource()
        val recipeId = 1L
        val ingredients = createNIngredients(3)
        val descriptions = createNDescription(3)
        val recipe = ApiReceipt(
            recipeId,
            "New recipe",
            appUser.OnlineID,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            ingredients,
            descriptions,
        )
        val success = recipeRemoteDS.create(recipe)
        assert(success)
        val remoteRecipe = recipeRemoteDS.read(recipeId, appUser.OnlineID)
        Assert.assertEquals(recipe, remoteRecipe)
    }

}