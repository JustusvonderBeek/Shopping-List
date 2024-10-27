package com.cloudsheeptech.shoppinglist.data.user

import android.util.Log
import com.cloudsheeptech.shoppinglist.data.typeConverter.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.network.Networking
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

/*
* This class is responsible for handling all online actions including
* creating, updating, retrieving and deleting user information.
 */
@Singleton
class AppUserRemoteDataSource
    @Inject
    constructor(
        private val remoteApi: Networking,
    ) {
        val json =
            Json {
                serializersModule =
                    SerializersModule {
                        contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
                    }
                // Because this class is only used to handle the user information and not
                // arbitrary user data, unknown keys represent a protocol violation
                ignoreUnknownKeys = false
                // Again, all information should be contained in the serialized JSON
                // that is sent to the server
                encodeDefaults = true
            }

        private fun AppUser.toApiUser(): ApiUser =
            ApiUser(
                this.OnlineID,
                this.Username,
                this.Password,
                this.Created,
                null,
            )

        private suspend fun userOnlineCreatedCallback(
            response: HttpResponse,
            user: AppUser,
        ) {
            withContext(Dispatchers.IO) {
                if (response.status != HttpStatusCode.Created) {
                    Log.e("AppUserRemoteRepository", "Failed to create user online")
                    return@withContext
                }
                val rawBody = response.bodyAsText(Charsets.UTF_8)
                val decodedUser = json.decodeFromString<ApiUser>(rawBody)
                decodedUser.password = user.Password
                val encodedUser = json.encodeToString(decodedUser)
                remoteApi.resetSerializedUser(encodedUser, decodedUser.onlineId)
            }
        }

        fun registerCreateUser(user: AppUser) {
            val serializedUser = json.encodeToString(user.toApiUser())
            remoteApi.resetSerializedUser(serializedUser, user.OnlineID)
            remoteApi.registerUserCallback {
                if (user.OnlineID != 0L) {
                    Triple("", "", { })
                } else {
                    Triple(serializedUser, "/v1/users") { response ->
                        userOnlineCreatedCallback(response, user)
                    }
                }
            }
        }

        // TODO: Include a method to determine if the information was successfully sent
        suspend fun update(user: AppUser) {
            withContext(Dispatchers.IO) {
                val encodedUser = json.encodeToString(user.toApiUser())
                remoteApi.PUT("/v1/users/${user.OnlineID}", encodedUser) { resp ->
                    if (resp.status != HttpStatusCode.OK) {
                        Log.w("AppUserRemoteDataSource", "Failed to update user online!")
                        throw IllegalStateException("no internet connectivity")
                    }
                    Log.i("AppUserRemoteDataSource", "Updated user ${user.OnlineID} online")
                }
            }
        }

        suspend fun delete(user: AppUser) {
            if (user.OnlineID == 0L) {
                return
            }
            withContext(Dispatchers.IO) {
                try {
                    remoteApi.DELETE("/v1/users/${user.OnlineID}") { resp ->
                        if (resp.status != HttpStatusCode.OK) {
                            Log.w("AppUserRemoteDataSource", "Failed to delete user online!")
                            // In case the server cannot be reached, the call throws a
                            // ConnectException, therefore this only happens when the request was bad
                            throw IllegalArgumentException("bad request")
                        }
                        Log.i("AppUserRemoteDataSource", "Deleted user ${user.OnlineID} online")
                    }
                } catch (ex: IllegalAccessException) {
                    Log.w("AppUserRemoteDataSource", "Failed to delete user online: $ex")
                }
            }
        }
    }
