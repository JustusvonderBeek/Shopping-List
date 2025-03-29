package com.cloudsheeptech.shoppinglist.network.token

import io.ktor.client.plugins.auth.providers.BearerTokens

interface ITokenStorage {

    fun storeTokenToDisk(fileName: String, token: String): Boolean

    fun storeTokenToDisk(fileName: String, token: BearerTokens): Boolean

    fun readTokenFromDisk(fileName: String): BearerTokens?

    fun resetTokens(): Int

}