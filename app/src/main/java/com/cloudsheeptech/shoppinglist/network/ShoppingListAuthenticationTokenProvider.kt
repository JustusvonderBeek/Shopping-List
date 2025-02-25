package com.cloudsheeptech.shoppinglist.network

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.net.ConnectException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingListAuthenticationTokenProvider
@Inject
constructor(
    private val payloadProvider: IUserCreationDataProvider,
) : ITokenProvider {

    private var jwtToken: String? = null
    private var apiToken: String? = null

    private val json = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
    }
    private val unauthenticatedClient =
        HttpClient(OkHttp) {
            engine {
                config {
                    hostnameVerifier { hostname, sslSession ->
                        HostnameVerification.verifyHostname(hostname, sslSession)
                    }
                }
            }
        }

    // We only store the latest token on disk
    private fun readTokenFromDisk(tokenFile: String): BearerTokens? {
        var token = BearerTokens("", "")
        if (tokenFile.isEmpty()) {
            Log.w("Networking", "Given tokenFile value is empty")
            return null
        }
        if (!File(tokenFile).exists()) {
            Log.d("Networking", "Token File does not exist")
            return null
        }
        val content = File(tokenFile).readText(Charsets.UTF_8)
        try {
            val decodedToken = Json.decodeFromString<NetworkToken>(content)
            token = BearerTokens(decodedToken.token, decodedToken.token)
        } catch (ex: SerializationException) {
            Log.d("Networking", "The type of the token file is in incorrect format!")
            return null
        }
        return token
    }

    private fun storeTokenToDisk(
        tokenFile: String,
        token: BearerTokens,
    ) {
        if (tokenFile.isEmpty()) {
            Log.w("Networking", "Given tokenFile is empty")
            return
        }
        try {
            val tokenInFileformat = NetworkToken(token.accessToken)
            val encodedToken = Json.encodeToString(tokenInFileformat)
            // Overwriting the file in case it does exist
            File(tokenFile).writeText(encodedToken)
        } catch (ex: IOException) {
            Log.w("Networking", "Failed to store token on disk: $ex")
        }
    }

    private fun setToken(token: String) {
        this.jwtToken = token
        storeTokenToDisk("token.txt", BearerTokens(this.jwtToken ?: "", this.apiToken ?: ""))
    }

    override suspend fun getToken(): BearerTokens? {
        if (jwtToken != null) {
            return BearerTokens(jwtToken!!, jwtToken!!)
        }
        val token = readTokenFromDisk("token.txt")
        if (token != null) {
            return BearerTokens(token.accessToken, token.refreshToken)
        }
        return loginAndGetToken()
    }

    private suspend fun loginAndGetToken(): BearerTokens? {
        val user = payloadProvider.provideLoginPayload()
        if (user.first.isEmpty() || user.second == -1L) {
            return null
        }
        var token: BearerTokens? = null
        try {
            token = withContext(Dispatchers.IO) {
                val response =
                    unauthenticatedClient.post("${UrlProviderEnum.BASE_URL.url}/${UrlProviderEnum.LOGIN}/${user.second}") {
                        setBody(user.first)
                    }
                if (response.status != HttpStatusCode.OK) {
                    Log.e(
                        "ShoppingListAuthenticationTokenProvider",
                        "Login failed, cannot get token"
                    )
                    return@withContext null
                }
                val loginResponseBody = response.bodyAsText(Charsets.UTF_8)
                if (loginResponseBody.isNullOrEmpty()) {
                    Log.e(
                        "ShoppingListAuthenticationTokenProvider",
                        "Login response is empty, failed to get token"
                    )
                    return@withContext null
                }
                val parsedLoginTokenResponse =
                    json.decodeFromString<NetworkToken>(loginResponseBody)
                setToken(parsedLoginTokenResponse.token)
                return@withContext BearerTokens(
                    parsedLoginTokenResponse.token,
                    parsedLoginTokenResponse.token
                )
            }
        } catch (ex: ConnectException) {
            Log.e("ShoppingListAuthenticationTokenProvider", "Network failed during login: $ex")
        } catch (ex: IllegalArgumentException) {
            Log.e(
                "ShoppingListAuthenticationTokenProvider",
                "Given login response in incorrect format: $ex"
            )
        } catch (ex: SerializationException) {
            Log.e("ShoppingListAuthenticationTokenProvider", "Failed to parse login response: $ex")
        } catch (ex: Exception) {
            Log.e("ShoppingListAuthenticationTokenProvider", "Unknown exception during login: $ex")
        }
        return token
    }

    override suspend fun refreshToken(): BearerTokens? {
        val token = refreshTokenAndCreateUserIfNotExists()
        return token
    }

    private suspend fun createUserIfNotExists(): Boolean {
        val success =
            withContext(Dispatchers.IO) {
                val finalRequestUrl =
                    "${UrlProviderEnum.BASE_URL.url}${UrlProviderEnum.BASE_USER_URL.url}"
                try {
                    val payload =
                        payloadProvider.provideUserCreationPayload() ?: return@withContext true
                    val response: HttpResponse =
                        unauthenticatedClient.post(finalRequestUrl) {
                            setBody(payload)
                        }
                    if (response.status != HttpStatusCode.Created) {
                        return@withContext false
                    }
                    val rawBody = response.bodyAsText(Charsets.UTF_8)
                    val processingSuccess = payloadProvider.processUserCreationResponse(rawBody)
                    return@withContext processingSuccess
                } catch (ex: Exception) {
                    Log.e(
                        "TokenProvider",
                        "Failed to send POST to $finalRequestUrl",
                    )
                }
                return@withContext false
            }
        return success
    }

    suspend fun refreshTokenAndCreateUserIfNotExists(): BearerTokens? {
        Log.d("Networking", "refreshing token...")
        val tokens: BearerTokens? =
            withContext(Dispatchers.IO) {
                try {
                    val successfullyCreated = createUserIfNotExists()
                    if (!successfullyCreated) {
                        return@withContext null
                    }
                    val token = loginAndGetToken()
                    return@withContext token
                } catch (ex: Exception) {
                    Log.e("TokenProvider", "Failed to refresh tokens: $ex")
                }
                return@withContext null
            }
        tokens?.let { setToken(it.accessToken) }
        return tokens
    }
}
