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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import javax.inject.Inject

class TokenProvider
    @Inject
    constructor(
        private val userCreationPayloadProvider: () -> String?,
        private val userCreationPayloadProcessor: suspend (response: String) -> Unit,
        private val userLoginPayloadProvider: () -> Pair<String, Long>,
    ) {
        @Serializable
        data class Token
            @OptIn(ExperimentalSerializationApi::class)
            constructor(
                @JsonNames("token")
                var token: String,
            )

        private var jwtToken: String? = null
        private var apiToken: String? = null

        private val authenticationClient =
            HttpClient(OkHttp) {
                engine {
                    config {
                        hostnameVerifier { hostname, sslSession ->
                            HostnameVerification.verifyHostname(hostname, sslSession)
                        }
                    }
                }
            }

        // TODO: Add loading token from disk in case we use a longer running one

        fun getToken(): BearerTokens? = if (jwtToken != null) BearerTokens(jwtToken!!, jwtToken!!) else null

        fun setToken(token: String) {
            this.jwtToken = token
        }

        private suspend fun createUserIfNotExists(): Boolean {
            val success =
                withContext(Dispatchers.IO) {
                    try {
                        val payload = userCreationPayloadProvider.invoke() ?: return@withContext true
                        val response: HttpResponse =
                            authenticationClient.post("${UrlProviderEnum.BASE_URL}${UrlProviderEnum.USER_CREATION}") {
                                setBody(payload)
                            }
                        if (response.status != HttpStatusCode.Created) {
                            return@withContext false
                        }
                        val rawBody = response.bodyAsText(Charsets.UTF_8)
                        userCreationPayloadProcessor.invoke(rawBody)
                        return@withContext true
                    } catch (ex: Exception) {
                        Log.e(
                            "TokenProvider",
                            "Failed to send POST to ${UrlProviderEnum.BASE_URL}${UrlProviderEnum.USER_CREATION}",
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
                        val (payload, userId) = userLoginPayloadProvider.invoke()
                        val response: HttpResponse =
                            authenticationClient.post("${UrlProviderEnum.BASE_URL}${UrlProviderEnum.USER_LOGIN}/$userId/login") {
                                setBody(payload)
                            }
                        if (response.status != HttpStatusCode.OK) {
                            Log.e("TokenProvider", "Login failed: ${response.status}")
                            return@withContext null
                        }
                        val rawBody = response.bodyAsText(Charsets.UTF_8)
                        val decodedToken = Json.decodeFromString<Token>(rawBody)
                        return@withContext BearerTokens(decodedToken.token, decodedToken.token)
                    } catch (ex: Exception) {
                        Log.e("TokenProvider", "Failed to refresh tokens: $ex")
                    }
                    return@withContext null
                }
            tokens?.let { setToken(it.accessToken) }
            return tokens
        }

        fun getApiToken(): String? = apiToken

        fun setApiToken(token: String) {
            this.apiToken = token
        }
    }
