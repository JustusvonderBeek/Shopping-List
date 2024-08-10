package com.cloudsheeptech.shoppinglist.data.user

import android.security.keystore.UserNotAuthenticatedException
import android.util.Log
import com.cloudsheeptech.shoppinglist.data.Serializer.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.network.Networking
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.io.IOException
import java.time.OffsetDateTime

/*
* This class is responsible for handling all online actions including
* creating, updating, retrieving and deleting user information.
 */
class AppUserRemoteDataSource(private val remoteApi: Networking) {

    val json = Json {
        serializersModule = SerializersModule {
            contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
        }
        // Because this class is only used to handle the user information and not
        // arbitrary user data, unknown keys represent a protocol violation
        ignoreUnknownKeys = false
        // Again, all information should be contained in the serialized JSON
        // that is sent to the server
        encodeDefaults = true
    }

    private fun AppUser.toApiUser() : ApiUser {
        return ApiUser(
            this.OnlineID,
            this.Username,
            this.Password,
            this.Created,
            null
        )
    }

    private fun ApiUser.toAppUser(): AppUser {
        return AppUser(
            1,
            this.OnlineID,
            this.Username,
            this.Password ?: "",
            this.Created
        )
    }

    @Throws(IllegalAccessError::class, IllegalStateException::class)
    suspend fun create(user: AppUser) : AppUser? {
        var appUser : AppUser? = null
        withContext(Dispatchers.IO) {
            val encodedUser = json.encodeToString(user.toApiUser())
            remoteApi.POST("auth/create", encodedUser) { resp ->
                // Authentication already handled by the networking object
                if (resp.status != HttpStatusCode.Created) {
                    Log.w("AppUserRemoteDataSource", "Failed to create the user online!")
                    throw IllegalArgumentException("bad request")
                }
                val rawBody = resp.bodyAsText(Charsets.UTF_8)
                val parsedApiUser = json.decodeFromString<ApiUser>(rawBody)
                // Automatically update the network class with the correct
                // new online id
                parsedApiUser.Password = user.Password
                val updatedUser = json.encodeToString(parsedApiUser)
                remoteApi.resetSerializedUser(updatedUser)
                // And return the update user
                appUser = parsedApiUser.toAppUser()
                Log.i("AppUserRemoteDataSource", "Created the user ${parsedApiUser.OnlineID} online")
            }
        }
        return appUser
    }

    suspend fun read() : AppUser? {
        throw NotImplementedError("This function is not implemented!")
    }

    // TODO: Include a method to determine if the information was successfully sent
    suspend fun update(user: AppUser) {
        withContext(Dispatchers.IO) {
            val encodedUser = json.encodeToString(user.toApiUser())
            remoteApi.PUT("v1/user/${user.OnlineID}", encodedUser) { resp ->
                if (resp.status != HttpStatusCode.OK) {
                    Log.w("AppUserRemoteDataSource", "Failed to update user online!")
                    throw IllegalStateException("no internet connectivity")
                }
                Log.i("AppUserRemoteDataSource", "Updated user ${user.OnlineID} online")
            }
        }
    }

    suspend fun delete(user: AppUser) {
        withContext(Dispatchers.IO) {
            val encodedUser = json.encodeToString(user.toApiUser())
            remoteApi.DELETE("v1/user/${user.OnlineID}", encodedUser) { resp ->
                if (resp.status != HttpStatusCode.OK) {
                    Log.w("AppUserRemoteDataSource", "Failed to delete user online!")
                    // In case the server cannot be reached, the call throws a
                    // ConnectException, therefore this only happens when the request was bad
                    throw IllegalArgumentException("bad request")
                }
                Log.i("AppUserRemoteDataSource", "Deleted user ${user.OnlineID} online")
            }
        }
    }
}