package com.cloudsheeptech.shoppinglist.data.recipe

import android.util.Log
import com.cloudsheeptech.shoppinglist.data.typeConverter.OffsetDateTimeFormatHandler
import com.cloudsheeptech.shoppinglist.exception.UserNotAuthenticatedException
import com.cloudsheeptech.shoppinglist.network.Networking
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRemoteDataSource
@Inject
constructor(
    private val networking: Networking,
    private val certificate: InputStream,
) {
    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = false
            serializersModule =
                SerializersModule {
                    contextual(OffsetDateTime::class, OffsetDateTimeFormatHandler())
                }
        }

    @OptIn(InternalSerializationApi::class)
    suspend fun create(recipe: ApiRecipe, recipeImages: List<ByteArray>): Boolean {
        var success = false
        withContext(Dispatchers.IO) {
            try {
                val encodedReceipt = json.encodeToString(recipe)
                networking.MULTIFORM_POST("/v1/recipe", encodedReceipt, recipeImages) { response ->
                    if (response.status != HttpStatusCode.Created) {
                        Log.e("RecipeRemoteDataSource", "Failed to create remote receipt")
                        return@MULTIFORM_POST
                    }
                    success = true
                }
            } catch (ex: SerializationException) {
                Log.e(
                    "RecipeRemoteDataSource",
                    "Recipe cannot be serialized: $ex",
                )
            } catch (ex: IOException) {
                Log.e("RecipeRemoteDataSource", "Failed to connect to remote: $ex")
            } catch (ex: ConnectException) {
                Log.e("RecipeRemoteDataSource", "Failed to connect to remote: $ex")
            } catch (ex: UserNotAuthenticatedException) {
                Log.e("RecipeRemoteDataSource", "User not authenticated: $ex")
            } catch (ex: Exception) {
                Log.e("RecipeRemoteDataSource", "Unknown exception while creating recipe: $ex")
            }
        }
        return success
    }

    @OptIn(InternalSerializationApi::class)
    suspend fun read(
        recipeId: Long,
        createdBy: Long,
    ): ApiRecipe? {
        var onlineReceipt: ApiRecipe? = null
        networking.GET("/v1/recipe/$recipeId?createdBy=$createdBy") { response ->
            if (response.status != HttpStatusCode.OK) {
                Log.e(
                    "ReceiptRemoteDataSource",
                    "Failed to get receipt $recipeId from $createdBy online",
                )
                return@GET
            }
            val rawBody = response.bodyAsText(Charsets.UTF_8)
            if (rawBody.isEmpty() || rawBody == "null") {
                Log.w("ReceiptRemoteDataSource", "List $recipeId from $createdBy not found online")
                return@GET
            }
            val decoded = json.decodeFromString<ApiRecipe>(rawBody)
            onlineReceipt = decoded
            Log.d(
                "ReceiptRemoteDataSource",
                "Found receipt $recipeId with ${onlineReceipt?.ingredients?.size} online",
            )
        }
        return onlineReceipt
    }

    @OptIn(InternalSerializationApi::class)
    suspend fun update(recipe: ApiRecipe): Boolean {
        var success = false
        val encodedReceipt = json.encodeToString(recipe)
        networking.PUT(
            "/v1/recipe/${recipe.onlineId}?createdBy=${recipe.createdBy.onlineId}",
            encodedReceipt,
        ) { response ->
            if (response.status != HttpStatusCode.OK) {
                Log.e(
                    "ReceiptRemoteDataSource",
                    "Failed to update receipt ${recipe.onlineId} online",
                )
                return@PUT
            }
            success = true
        }
        return success
    }

    suspend fun delete(
        recipeId: Long,
        createdBy: Long,
    ): Boolean {
        var success = false
        networking.DELETE("/v1/recipe/$recipeId?createdBy=$createdBy") { response ->
            if (response.status != HttpStatusCode.OK) {
                Log.e("ReceiptRemoteDataSource", "Failed to delete receipt $recipeId online")
                return@DELETE
            }
            success = true
        }
        return success
    }
}
