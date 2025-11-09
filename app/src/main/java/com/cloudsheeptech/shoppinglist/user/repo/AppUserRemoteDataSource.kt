package com.cloudsheeptech.shoppinglist.user.repo

import android.util.Log
import com.cloudsheeptech.shoppinglist.list.model.UserAuthenticationFailedException
import com.cloudsheeptech.shoppinglist.list.model.UserNotCreatedException
import com.cloudsheeptech.shoppinglist.user.api.UserAuthenticatedApi
import com.cloudsheeptech.shoppinglist.user.model.ApiUser
import com.cloudsheeptech.shoppinglist.user.model.AppUser
import com.cloudsheeptech.shoppinglist.user.util.UserFormatAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        private val userAuthApi: UserAuthenticatedApi,
    ) {
        // Because the creation is handled by the request itself, we only
        // ping the endpoint which should be enough to create a new account
        suspend fun create() {
            withContext(Dispatchers.IO) {
                try {
                    val response = userAuthApi.ping()
                    if (!response.isSuccessful) {
                        Log.e("AppUserRemoteDataSource", "Pinging the endpoint failed")
                    }
                } catch (ex: UserNotCreatedException) {
                    Log.e("AppUserRemoteRepository", "User is not authenticated online: $ex")
                } catch (ex: UserAuthenticationFailedException) {
                    Log.e("AppUserRemoteRepository", "User authentication failed: $ex")
                } catch (ex: Exception) {
                    Log.e("AppUserRemoteDataSource", "Failed to create user by pinging endpoint: $ex")
                }
            }
        }

        suspend fun read(): ApiUser = throw NotImplementedError("this method should never be called")

        suspend fun update(user: AppUser): Boolean {
            return withContext(Dispatchers.IO) {
                try {
                    val response = userAuthApi.updateUser(user.OnlineID, UserFormatAdapter.fromAppToApiUser(user))
                    return@withContext response.isSuccessful
                } catch (ex: UserNotCreatedException) {
                    Log.e("AppUserRemoteRepository", "User is not authenticated online: $ex")
                } catch (ex: UserAuthenticationFailedException) {
                    Log.e("AppUserRemoteRepository", "User authentication failed: $ex")
                } catch (ex: Exception) {
                    Log.e("AppUserRemoteDataSource", "Failed to update user: $ex")
                }
                return@withContext false
            }
        }

        suspend fun delete(user: AppUser): Boolean {
            if (user.OnlineID == 0L) {
                Log.e("AppUserRemoteDataSource", "UserId is 0 therefore nothing to delete online")
                return true
            }
            return withContext(Dispatchers.IO) {
                try {
                    val response = userAuthApi.delete(user.OnlineID)
                    if (response.isSuccessful) {
                        Log.i("AppUserRemoteDataSource", "Successfully deleted user ${user.OnlineID} online")
                        return@withContext true
                    }
                    Log.e("AppUserRemoteDataSource", "Failed to delete user ${user.OnlineID} online")
                } catch (ex: IllegalAccessException) {
                    Log.w("AppUserRemoteDataSource", "Failed to delete user online: $ex")
                }
                return@withContext false
            }
        }
    }
