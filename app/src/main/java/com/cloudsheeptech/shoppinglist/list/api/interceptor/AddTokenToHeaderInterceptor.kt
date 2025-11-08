package com.cloudsheeptech.shoppinglist.list.api.interceptor

import com.cloudsheeptech.shoppinglist.network.token.ShoppingListAuthenticationTokenProvider
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddTokenToHeaderInterceptor
    @Inject
    constructor(
        private val tokenProvider: ShoppingListAuthenticationTokenProvider,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val token = tokenProvider.loadToken()
            val request =
                if (token == null) {
                    // No token found:
                    // Perform normal request (which might fail due to missing token)
                    // Then use UserCreationInterceptor to create account if no account created yet
                    chain.request()
                } else {
                    // Add token if found
                    chain
                        .request()
                        .newBuilder()
                        .addHeader("Authorization", "Bearer ${token.accessToken}")
                        .build()
                }

            return chain.proceed(request)
        }
    }
