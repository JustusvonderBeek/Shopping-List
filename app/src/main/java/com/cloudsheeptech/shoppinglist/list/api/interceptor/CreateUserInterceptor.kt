package com.cloudsheeptech.shoppinglist.list.api.interceptor

import android.util.Log
import com.cloudsheeptech.shoppinglist.network.token.ShoppingListTokenStorage
import com.cloudsheeptech.shoppinglist.user.api.UserUnauthenticatedApi
import com.cloudsheeptech.shoppinglist.user.model.AppUser
import com.cloudsheeptech.shoppinglist.user.repo.AppUserRepository
import com.cloudsheeptech.shoppinglist.user.util.UserFormatAdapter
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreateUserInterceptor
    @Inject
    constructor(
        private val userRepository: AppUserRepository,
        private val userUnauthenticatedApi: UserUnauthenticatedApi,
        private val appFileDir: String,
    ) : Authenticator {
        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            Log.d("CreateUserInterceptor", "Refreshing auth token or creating user if not exists")
            try {
                val userCreated = createUserIfNotExists()
                if (userCreated == null || userCreated.OnlineID <= 0L) {
                    Log.e("CreateUserInterceptor", "Failed to create new user")
                    return null
                }
                runBlocking {
                    userRepository.updateOnlineId(userCreated!!.OnlineID)
                }
                val currentUser = userRepository.read()
                if (currentUser == null) {
                    Log.e("CreateUserInterceptor", "The current user is null, cannot authenticate!")
                    return null
                }
                val userInApiFormat = UserFormatAdapter.fromAppToApiUser(currentUser)
                val token =
                    runBlocking {
                        userUnauthenticatedApi.login(currentUser.OnlineID, userInApiFormat)
                    }
                if (token == null || token.token.isNullOrBlank()) {
                    Log.e("CreateUserInterceptor", "Failed to authenticate with current user online")
                    return null
                }
                ShoppingListTokenStorage.storeTokenToDisk(appFileDir, "token.tkn", token.token)
                return response.request
                    .newBuilder()
                    .addHeader("Authorization", "Bearer ${token.token}")
                    .build()
            } catch (ex: Exception) {
                Log.e("CreateUserInterceptor", "Failed to authenticate: $ex")
            }
            return response.request
        }

        private fun createUserIfNotExists(): AppUser? {
            val user = userRepository.read() ?: return null
            if (user.OnlineID > 0) {
                return user
            }
            val onlineUser =
                runBlocking {
                    userUnauthenticatedApi.create(user)
                }
            if (onlineUser == null || onlineUser.onlineId == 0L) {
                Log.e("CreateUserInterceptor", "Failed to create user online")
                return null
            }
            if (onlineUser.password != "accepted") {
                Log.e("CreateUserInterceptor", "The remote endpoint did not replace the password, protocol violation")
                return null
            }

            return UserFormatAdapter.fromApiToAppUser(onlineUser)
        }
    }
