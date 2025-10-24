package com.cloudsheeptech.shoppinglist.data.user

import android.util.Log
import com.cloudsheeptech.shoppinglist.list.model.UserAuthenticationFailedException
import com.cloudsheeptech.shoppinglist.list.model.UserNotCreatedException
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.network.UrlProviderEnum
import com.cloudsheeptech.shoppinglist.util.OffsetDateTimeFormatHandler
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
                        contextual(OffsetDateTime::class, OffsetDateTimeFormatHandler())
                    }
                // Because this class is only used to handle the user information and not
                // arbitrary user data, unknown keys represent a protocol violation
                ignoreUnknownKeys = false
                // Again, all information should be contained in the serialized JSON
                // that is sent to the server
                encodeDefaults = true
            }

        // Because the creation is handled by the request itself, we only
        // ping the endpoint which should be enough to create a new account
        suspend fun create() {
            withContext(Dispatchers.IO) {
                var requestSuccess = false
                try {
                    remoteApi.get(
                        "${UrlProviderEnum.PING.url}",
                    ) { resp ->
                        if (resp.status != HttpStatusCode.OK) {
                            Log.e("AppUserRemoteRepository", "Current status is not online")
                            return@get
                        }
                        requestSuccess = true
                    }
                    return@withContext requestSuccess
                } catch (ex: UserNotCreatedException) {
                    Log.e("AppUserRemoteRepository", "User is not authenticated online")
                } catch (ex: UserAuthenticationFailedException) {
                    Log.e("AppUserRemoteRepository", "User authentication failed")
                }
                return@withContext false
            }
            return
        }

        suspend fun read(): ApiUser = throw NotImplementedError("this method should never be called")

        suspend fun update(user: AppUser): Boolean {
            val success =
                withContext(Dispatchers.IO) {
                    try {
                        val encodedUser = json.encodeToString(UserFormatAdapter.fromAppToApiUser(user))
                        remoteApi.PUT(
                            "${UrlProviderEnum.BASE_USER_URL.url}/${user.OnlineID}",
                            encodedUser,
                        ) { resp ->
                            if (resp.status != HttpStatusCode.OK) {
                                throw IllegalStateException("update failed with status: ${resp.status}")
                            }
                            Log.i("AppUserRemoteDataSource", "Updated user ${user.OnlineID} online")
                        }
                        return@withContext true
                    } catch (ex: UserNotCreatedException) {
                        Log.e("AppUserRemoteRepository", "User is not authenticated online")
                    } catch (ex: UserAuthenticationFailedException) {
                        Log.e("AppUserRemoteRepository", "User authentication failed")
                    }
                    return@withContext false
                }
            return success
        }

        suspend fun delete(user: AppUser): Boolean {
            if (user.OnlineID == 0L) {
                return true
            }
            val success =
                withContext(Dispatchers.IO) {
                    try {
                        remoteApi.DELETE("${UrlProviderEnum.BASE_USER_URL.url}/${user.OnlineID}") { resp ->
                            if (resp.status != HttpStatusCode.OK) {
                                Log.w("AppUserRemoteDataSource", "Failed to delete user online!")
                                // In case the server cannot be reached, the call throws a
                                // ConnectException, therefore this only happens when the request was bad
                                throw IllegalArgumentException("bad request")
                            }
                            Log.i("AppUserRemoteDataSource", "Deleted user ${user.OnlineID} online")
                        }
                        return@withContext true
                    } catch (ex: IllegalAccessException) {
                        Log.w("AppUserRemoteDataSource", "Failed to delete user online: $ex")
                    }
                    return@withContext false
                }
            return success
        }
    }
