package com.cloudsheeptech.shoppinglist.network.token

import com.cloudsheeptech.shoppinglist.BuildConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import javax.inject.Singleton

@Singleton
class ShoppingListApiTokenProvider : ITokenProvider {
    override fun loadToken(): BearerTokens {
        val apiToken = BuildConfig.SERVER_URL
        return BearerTokens(apiToken, "")
    }

    override suspend fun refreshToken(): BearerTokens = BearerTokens("", "")
}
