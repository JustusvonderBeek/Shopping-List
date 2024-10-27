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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenProvider
    @Inject
    constructor(
        private val payloadProvider: UserCreationPayloadProvider,
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
        private fun readTokenFromDisk(tokenFile: String): BearerTokens {
            var token = BearerTokens("", "")
            if (tokenFile.isEmpty()) {
                Log.w("Networking", "Given tokenFile value is empty")
                return token
            }
            if (!File(tokenFile).exists()) {
                Log.d("Networking", "Token File does not exist")
                return token
            }
            val content = File(tokenFile).readText(Charsets.UTF_8)
            try {
                val decodedToken = Json.decodeFromString<Token>(content)
                token = BearerTokens(decodedToken.token, decodedToken.token)
            } catch (ex: SerializationException) {
                Log.d("Networking", "The type of the token file is in incorrect format!")
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
                val tokenInFileformat = Token(token.accessToken)
                val encodedToken = Json.encodeToString(tokenInFileformat)
                // Overwriting the file in case it does exist
                File(tokenFile).writeText(encodedToken)
            } catch (ex: IOException) {
                Log.w("Networking", "Failed to store token on disk: $ex")
            }
        }

        fun getToken(): BearerTokens? = if (jwtToken != null) BearerTokens(jwtToken!!, jwtToken!!) else null

        fun setToken(token: String) {
            this.jwtToken = token
        }

        private suspend fun createUserIfNotExists(): Boolean {
            val success =
                withContext(Dispatchers.IO) {
                    val finalRequestUrl = "${UrlProviderEnum.BASE_URL.url}${UrlProviderEnum.BASE_USER_URL.url}"
                    try {
                        val payload = payloadProvider.provideUserCreationPayload() ?: return@withContext true
                        val response: HttpResponse =
                            unauthenticatedClient.post("$finalRequestUrl") {
                                setBody(payload)
                            }
                        if (response.status != HttpStatusCode.Created) {
                            return@withContext false
                        }
                        val rawBody = response.bodyAsText(Charsets.UTF_8)
                        payloadProvider.processUserCreationResponse(rawBody)
                        return@withContext true
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
                    val finalRequestUrl = "${UrlProviderEnum.BASE_URL.url}${UrlProviderEnum.BASE_USER_URL.url}"
                    try {
                        val successfullyCreated = createUserIfNotExists()
                        if (!successfullyCreated) {
                            return@withContext null
                        }
                        val (payload, userId) = payloadProvider.provideLoginPayload()
                        if (userId == -1L || payload.isEmpty()) {
                            return@withContext null
                        }
                        val response: HttpResponse =
                            unauthenticatedClient.post(
                                "$finalRequestUrl/$userId/login",
                            ) {
                                setBody(payload)
                            }
                        when (response.status) {
                            HttpStatusCode.NotFound -> {
                                Log.e("TokenProvider", "The user login request was made but the userId not found online")
                                // TODO: This should only happen during testing, never in a production system
                                return@withContext null
                            }
                            HttpStatusCode.OK -> {
                                val rawBody = response.bodyAsText(Charsets.UTF_8)
                                val decodedToken = Json.decodeFromString<Token>(rawBody)
                                return@withContext BearerTokens(decodedToken.token, decodedToken.token)
                            }
                            else -> {
                                // empty
                            }
                        }
                        return@withContext null
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
