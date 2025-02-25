package com.cloudsheeptech.shoppinglist.network

import com.cloudsheeptech.shoppinglist.BuildConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import javax.inject.Singleton

@Singleton
class ShoppingListApiTokenProvider : ITokenProvider {

    override suspend fun getToken(): BearerTokens {
        val apiToken = BuildConfig.SERVER_URL
        return BearerTokens(apiToken, "")
    }

    override suspend fun refreshToken(): BearerTokens? {
        TODO("Not yet implemented")
    }
}