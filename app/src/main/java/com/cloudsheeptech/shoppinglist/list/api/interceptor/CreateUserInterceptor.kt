package com.cloudsheeptech.shoppinglist.list.api.interceptor

import android.util.Log
import com.cloudsheeptech.shoppinglist.user.api.UserUnauthenticatedApi
import com.cloudsheeptech.shoppinglist.user.repo.AppUserRepository
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
            // TODO: Rewrite this function to make use of the userApi
            // and perform non-blocking call in here
            Log.d("CreateUserInterceptor", "Refreshing auth token or creating user if not exists")
            try {
                val userCreated = createUserIfNotExists()
                if (!userCreated) {
                    Log.e("CreateUserInterceptor", "Failed to create new user")
                }
                return null
            } catch (ex: Exception) {
                Log.e("CreateUserInterceptor", "Failed to authenticate: $ex")
            }
            return null
        }

        private fun createUserIfNotExists(): Boolean {
            val user = userRepository.read() ?: return false
            val success = userUnauthenticatedApi.create(user)
            // TODO: Test and verify if this works, and whats returned on different kind of errors
            if (success.onlineId == 0L) {
                Log.e("CreateUserInterceptor", "Failed to create user online")
                return false
            }
            return true
        }
    }
