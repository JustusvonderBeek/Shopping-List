package com.cloudsheeptech.shoppinglist.data.sharing

import android.util.Log
import com.cloudsheeptech.shoppinglist.data.typeConverter.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.network.Networking
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListShareRemoteDataSource @Inject constructor(private val networking: Networking, private val userRepo: AppUserRepository) {

    private val json = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
        serializersModule = SerializersModule {
            contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
        }
    }

    /**
     * Creating a new sharing for a single user in the database
     * @throws IllegalStateException if the app is not correctly initialized
     * @throws IllegalArgumentException if the given list does not exist
     */
    suspend fun create(listId: Long, sharedWith: Long) : Boolean {
        var success = false
        withContext(Dispatchers.IO) {
            val user = userRepo.read() ?: throw IllegalStateException("user not initialized")
            val sharing = ListShare(CreatedBy = user.OnlineID, SharedWith = listOf(sharedWith), Created = OffsetDateTime.now())
            val encodedSharing = json.encodeToString(sharing)
            networking.POST("/v1/share/$listId", encodedSharing) { response ->
                if (response.status != HttpStatusCode.Created) {
                    Log.e("ListShareRemoteDataSource", "Failed to create sharing $listId for $sharedWith online")
                    return@POST
                }
                success = true
            }
        }
        return success
    }

    /**
     * This method is not implemented because this should never be called
     * @throws NotImplementedError
     */
    @Throws(NotImplementedError::class)
    suspend fun read(listId: Long) : List<Long> {
        throw NotImplementedError("this should never be called from online")
    }

    suspend fun update(listId: Long, sharedWith: List<Long>) : Boolean {
        var success = false
        withContext(Dispatchers.IO) {
            val user = userRepo.read() ?: throw IllegalStateException("user not initialized")
            val sharing = ListShare(CreatedBy = user.OnlineID, SharedWith = sharedWith, Created = OffsetDateTime.now())
            val encodedSharing = json.encodeToString(sharing)
            networking.PUT("/v1/share/$listId", encodedSharing) { response ->
                if (response.status != HttpStatusCode.OK) {
                    Log.e("ListShareRemoteDataSource", "Failed to update sharing $listId online")
                    return@PUT
                }
                success = true
            }
        }
        return success
    }

    suspend fun delete(listId: Long) : Boolean {
        var success = false
        withContext(Dispatchers.IO) {
            networking.DELETE("/v1/share/$listId") { response ->
                if (response.status != HttpStatusCode.OK) {
                    Log.e("ListShareRemoteDataSource", "Failed to delete sharing $listId online")
                    return@DELETE
                }
                success = true
            }
        }
        return success
    }

}