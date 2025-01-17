package com.cloudsheeptech.shoppinglist.data.sharing.recipe

import android.util.Log
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.network.UrlProviderEnum
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class RecipeShareRemoteDataSource @Inject constructor(private val networking: Networking) {

    private val LOG_TAG = "RecipeShareRemoteDataSource"

    private val json = Json {
        ignoreUnknownKeys = false
    }

    suspend fun create(recipeShare: RecipeShare): Boolean {
        var success = false
        withContext(Dispatchers.IO) {
            val serialized = Json.encodeToString(recipeShare)
            networking.POST(
                "${UrlProviderEnum.BASE_URL.url}${UrlProviderEnum.RECIPE_SHARE.url}",
                serialized
            ) { response ->
                if (response.status != HttpStatusCode.Created) {
                    Log.e(LOG_TAG, "Failed to create recipe share online")
                    return@POST
                }
                success = true
            }
        }
        return success
    }

    suspend fun read(recipeId: Long, createdBy: Long): List<RecipeShare> {
        val remoteStoredSharedWithIdsForList = mutableListOf<RecipeShare>()
        withContext(Dispatchers.IO) {
            networking.GET("${UrlProviderEnum.BASE_URL.url}${UrlProviderEnum.RECIPE_SHARE.url}/$recipeId?createdBy=$createdBy") { response ->
                if (response.status != HttpStatusCode.OK) {
                    Log.e(LOG_TAG, "Failed to read recipe share online")
                    return@GET
                }
                val rawBody = response.bodyAsText(Charsets.UTF_8)
                if (rawBody.isEmpty() || rawBody == "null") {
                    Log.w(LOG_TAG, "Received list of shares is empty")
                    return@GET
                }
                val decoded = json.decodeFromString<List<RecipeShare>>(rawBody)
                remoteStoredSharedWithIdsForList.addAll(decoded)
            }
        }
        return remoteStoredSharedWithIdsForList
    }

    suspend fun delete(recipeId: Long, createdBy: Long, sharedWith: Long): Boolean {
        var success = false
        withContext(Dispatchers.IO) {
            networking.DELETE("${UrlProviderEnum.BASE_URL.url}${UrlProviderEnum.RECIPE_SHARE.url}/$recipeId?createdBy=$createdBy&sharedWith=$sharedWith") { response ->
                if (response.status != HttpStatusCode.OK) {
                    Log.e(LOG_TAG, "Failed to delete recipe share online")
                    return@DELETE
                }
                success = true
            }
        }
        return success
    }

    suspend fun deleteAll(recipeId: Long, createdBy: Long): Boolean {
        var success = false
        withContext(Dispatchers.IO) {
            networking.DELETE("${UrlProviderEnum.BASE_URL.url}${UrlProviderEnum.RECIPE_SHARE.url}/$recipeId?createdBy=$createdBy") { response ->
                if (response.status != HttpStatusCode.OK) {
                    Log.e(LOG_TAG, "Failed to delete recipe share online")
                    return@DELETE
                }
                success = true
            }
        }
        return success
    }

}