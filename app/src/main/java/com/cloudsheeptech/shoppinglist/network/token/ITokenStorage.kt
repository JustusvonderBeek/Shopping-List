package com.cloudsheeptech.shoppinglist.network.token

import io.ktor.client.plugins.auth.providers.BearerTokens

interface ITokenStorage {

    fun storeTokenToDisk(appFileDir: String, fileName: String, token: String): Boolean

    fun storeTokenToDisk(appFileDir: String, fileName: String, token: BearerTokens): Boolean

    fun readTokenFromDisk(appFileDir: String, fileName: String): BearerTokens?

    fun resetTokens(): Int

}