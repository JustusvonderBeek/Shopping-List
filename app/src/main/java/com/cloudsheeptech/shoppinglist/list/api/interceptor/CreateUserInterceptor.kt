package com.cloudsheeptech.shoppinglist.list.api.interceptor

import android.util.Log
import com.cloudsheeptech.shoppinglist.user.api.UserUnauthenticatedApi
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
    ) : Authenticator {
        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            Log.d("CreateUserInterceptor", "Refreshing auth token or creating user if not exists")
            try {
                val userCreated = createUserIfNotExists()
                if (!userCreated) {
                    Log.e("CreateUserInterceptor", "Failed to create new user")
                }
                val currentUser = userRepository.read()
                if (currentUser == null) {
                    Log.e("CreateUserInterceptor", "The current user is null, cannot authenticate!")
                    return null
                }
                val userInApiFormat = UserFormatAdapter.fromAppToApiUser(currentUser)
                val token =
                    runBlocking {
                        val token = userUnauthenticatedApi.login(userInApiFormat)
                        token
                    }
                if (token == null) {
                    Log.e("CreateUserInterceptor", "Failed to authenticate with current user online")
                    return null
                }
                response.header("Authentication", "Bearer ${token.token}")
            } catch (ex: Exception) {
                Log.e("CreateUserInterceptor", "Failed to authenticate: $ex")
            }
            return null
        }

        private fun createUserIfNotExists(): Boolean {
            val user = userRepository.read() ?: return false
            val onlineUser =
                runBlocking {
                    userUnauthenticatedApi.create(user)
                }
            if (onlineUser == null || onlineUser.onlineId == 0L) {
                Log.e("CreateUserInterceptor", "Failed to create user online")
                return false
            }
            if (onlineUser.password != "accepted") {
                Log.e("CreateUserInterceptor", "The remote endpoint did not replace the password, protocol violation")
                return false
            }
            return true
        }
    }
