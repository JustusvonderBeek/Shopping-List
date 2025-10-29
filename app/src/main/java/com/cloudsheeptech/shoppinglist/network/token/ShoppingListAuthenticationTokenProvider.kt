package com.cloudsheeptech.shoppinglist.network.token

import android.util.Log
import com.cloudsheeptech.shoppinglist.network.HostnameVerification
import com.cloudsheeptech.shoppinglist.network.IUserCreationDataProvider
import com.cloudsheeptech.shoppinglist.network.UrlProviderEnum
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
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.net.ConnectException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingListAuthenticationTokenProvider
    @Inject
    constructor(
        private val payloadProvider: IUserCreationDataProvider,
        private val appFileDir: String,
    ) : ITokenProvider {
        private val tokenFile = "token.tkn"

        private var jwtToken: BearerTokens? = null

        private val json =
            Json {
                ignoreUnknownKeys = false
                encodeDefaults = true
            }
        private val unauthenticatedClient =
            HttpClient(OkHttp) {
                engine {
                    config {
                        hostnameVerifier { hostname, sslSession ->
                            HostnameVerification.Companion.verifyHostname(hostname, sslSession)
                        }
                    }
                }
            }

        override fun loadToken(): BearerTokens? {
            if (jwtToken != null) {
                return jwtToken
            }
            return ShoppingListTokenStorage.readTokenFromDisk(appFileDir, tokenFile)
        }

        override suspend fun refreshToken(): BearerTokens? {
            val token = refreshTokenAndCreateUserIfNotExists()
            if (token?.accessToken?.isNotEmpty() == true) {
                this.jwtToken = token
                ShoppingListTokenStorage.storeTokenToDisk(
                    appFileDir,
                    tokenFile,
                    token,
                )
            }
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
                            "Failed to send POST to $finalRequestUrl: $ex",
                        )
                    }
                    return@withContext false
                }
            return success
        }

        @OptIn(InternalSerializationApi::class)
        private suspend fun loginAndGetToken(): BearerTokens? {
            val user = payloadProvider.provideLoginPayload() ?: return null
            var token: BearerTokens? = null
            try {
                token =
                    withContext(Dispatchers.IO) {
                        val response =
                            unauthenticatedClient.post("${UrlProviderEnum.BASE_URL.url}${UrlProviderEnum.LOGIN.url}/${user.second}") {
                                setBody(user.first)
                            }
                        if (response.status != HttpStatusCode.OK) {
                            Log.e(
                                "ShoppingListAuthenticationTokenProvider",
                                "Login failed, cannot get token",
                            )
                            return@withContext null
                        }
                        val loginResponseBody = response.bodyAsText(Charsets.UTF_8)
                        if (loginResponseBody.isNullOrEmpty()) {
                            Log.e(
                                "ShoppingListAuthenticationTokenProvider",
                                "Login response is empty, failed to get token",
                            )
                            return@withContext null
                        }
                        val parsedLoginTokenResponse =
                            json.decodeFromString<NetworkToken>(loginResponseBody)
                        return@withContext BearerTokens(
                            parsedLoginTokenResponse.token,
                            parsedLoginTokenResponse.token,
                        )
                    }
            } catch (ex: ConnectException) {
                Log.e("ShoppingListAuthenticationTokenProvider", "Network failed during login: $ex")
            } catch (ex: IllegalArgumentException) {
                Log.e(
                    "ShoppingListAuthenticationTokenProvider",
                    "Given login response in incorrect format: $ex",
                )
            } catch (ex: SerializationException) {
                Log.e("ShoppingListAuthenticationTokenProvider", "Failed to parse login response: $ex")
            } catch (ex: Exception) {
                Log.e("ShoppingListAuthenticationTokenProvider", "Unknown exception during login: $ex")
            }
            if (token != null) {
                this.jwtToken = token
                ShoppingListTokenStorage.storeTokenToDisk(
                    appFileDir,
                    tokenFile,
                    token,
                )
            }
            return token
        }

        suspend fun refreshTokenAndCreateUserIfNotExists(): BearerTokens? {
            Log.d("ShoppingListAuthenticationTokenProvider", "Refreshing the Authentication Token")
            val authToken: BearerTokens? =
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
        return authToken
    }
}
