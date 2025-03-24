package com.cloudsheeptech.shoppinglist.network

import io.ktor.client.plugins.auth.providers.BearerTokens

interface ITokenProvider {
    suspend fun loadToken(): BearerTokens?

    suspend fun refreshToken(): BearerTokens?
}
