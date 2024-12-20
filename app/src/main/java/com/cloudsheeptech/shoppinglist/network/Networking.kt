package com.cloudsheeptech.shoppinglist.network

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/*
* This class captures the authentication logic of the application,
* allowing to reuse the same HTTP client for all networking requests
 */
@Singleton
class Networking
    @Inject
    constructor(
        private val tokenFile: String,
        private val tokenProvider: TokenProvider,
    ) {
        //    private val baseUrl = "https://shop.cloudsheeptech.com:46152/"
//    private val baseUrl = "https://ec2-3-120-40-62.eu-central-1.compute.amazonaws.com:46152/"
        private val baseUrl = "https://10.0.2.2:46152"

        private var localUser = ""
        private var userId = 0L
        private var getUserRegisterCall: () -> Triple<String, String, suspend (response: HttpResponse) -> Unit> =
            { Triple("", "", {}) }

        private val authClient =
            HttpClient(OkHttp) {
                engine {
                    config {
                        connectTimeout(Duration.ofSeconds(3))
                        hostnameVerifier { hostname, sslSession ->
                            HostnameVerification.verifyHostname(hostname, sslSession)
                        }
                    }
                }
                install(Auth) {
                    bearer {
                        loadTokens {
                            tokenProvider.getToken()
                        }
                        refreshTokens {
                            tokenProvider.refreshTokenAndCreateUserIfNotExists()
                        }
                        // Always include our authToken when asking our service
//                        sendWithoutRequest { request ->
//                            request.url.encodedPath == Url("${UrlProviderEnum.BASE_URL}").encodedPath
//                        }
                    }
                }
            }

        fun resetSerializedUser(
            user: String,
            userId: Long,
        ) {
            this.localUser = user
            this.userId = userId
        }

        fun registerUserCallback(getUser: () -> Triple<String, String, suspend (response: HttpResponse) -> Unit>) {
            this.getUserRegisterCall = getUser
        }

        suspend fun GET(
            requestUrlPath: String,
            responseHandler: suspend (response: HttpResponse) -> Unit,
        ) {
            withContext(Dispatchers.IO) {
                try {
                    // This method should return "" only when the user is already registered
//                    if (userToRegister.isNotEmpty()) {
// //                        resetToken()
//                        post(registerUrl, userToRegister, registerCallback)
//                    }
                    get(requestUrlPath, responseHandler)
                } catch (ex: Exception) {
                    Log.w("Networking", "Failed to send GET request to $baseUrl$requestUrlPath: $ex")
                }
            }
        }

        private suspend fun get(
            requestUrlPath: String,
            responseHandler: suspend (HttpResponse) -> Unit,
        ) {
            withContext(Dispatchers.IO) {
                try {
                    val response: HttpResponse = authClient.get(baseUrl + requestUrlPath)
                    if (response.status == HttpStatusCode.Unauthorized) {
//                        resetToken()
                        throw IllegalAccessException("user not authenticated online")
                    }
                    responseHandler(response)
                } catch (ex: Exception) {
                    Log.e("Networking", "Failed to send GET request to $baseUrl$requestUrlPath")
                }
            }
        }

        suspend fun POST(
            requestUrlPath: String,
            data: String,
            responseHandler: suspend (HttpResponse) -> Unit,
        ) {
            POST(requestUrlPath, data, responseHandler, null)
        }

        @Throws(IllegalAccessError::class)
        suspend fun POST(
            requestUrlPath: String,
            data: String,
            responseHandler: suspend (HttpResponse) -> Unit,
            contentUpdater: (suspend (String) -> String)?,
        ) {
            val (userToRegister, registerUrl, registerCallback) = this.getUserRegisterCall.invoke()
            withContext(Dispatchers.IO) {
                try {
//                var dataToPost = data
//                if (contentUpdater != null) {
//                    dataToPost = contentUpdater.invoke(data)
//                }
                    if (userToRegister.isNotEmpty()) {
                        post(registerUrl, userToRegister, registerCallback)
                    }
                    post(requestUrlPath, data, responseHandler)
                } catch (ex: Exception) {
                    Log.w("Networking", "Failed to send POST request to $baseUrl$requestUrlPath: $ex")
                }
            }
        }

        private suspend fun post(
            requestUrlPath: String,
            data: String,
            responseHandler: suspend (HttpResponse) -> Unit,
        ) {
            withContext(Dispatchers.IO) {
                try {
                    val response: HttpResponse =
                        authClient.post(baseUrl + requestUrlPath) {
                            setBody(data)
                        }
                    if (response.status == HttpStatusCode.Unauthorized) {
//                        resetToken()
                        // This one is directly catched by the block outside?
                        throw IllegalAccessException("user not authenticated online")
                    }
                    responseHandler(response)
                } catch (ex: Exception) {
                    Log.e("Networking", "Failed to send POST to $baseUrl$requestUrlPath")
                }
            }
        }

        @Throws(IllegalAccessError::class)
        suspend fun PUT(
            requestUrlPath: String,
            data: String,
            responseHandler: suspend (HttpResponse) -> Unit,
        ) {
            val (userToRegister, registerUrl, registerCallback) = this.getUserRegisterCall.invoke()
            withContext(Dispatchers.IO) {
                try {
//                if (userToRegister.isNotEmpty()) {
//                    post(registerUrl, userToRegister, registerCallback)
//                }
                    put(requestUrlPath, data, responseHandler)
                } catch (ex: Exception) {
                    Log.w("Networking", "Failed to send POST request to $baseUrl$requestUrlPath: $ex")
                }
            }
        }

        private suspend fun put(
            requestUrlPath: String,
            data: String,
            responseHandler: suspend (HttpResponse) -> Unit,
        ) {
            withContext(Dispatchers.IO) {
                try {
                    val response: HttpResponse =
                        authClient.put(baseUrl + requestUrlPath) {
                            setBody(data)
                        }
                    if (response.status == HttpStatusCode.Unauthorized) {
//                        resetToken()
                        throw IllegalAccessException("user not authenticated online")
                    }
                    responseHandler(response)
//                response.bodyAsText()
                } catch (ex: Exception) {
                    Log.w("Networking", "Failed to send POST request to $baseUrl$requestUrlPath: $ex")
                }
            }
        }

        // Because the delete method does not allow for additional data, remove this
        // from the method body
        suspend fun DELETE(
            requestUrlPath: String,
            responseHandler: suspend (HttpResponse) -> Unit,
        ) {
            withContext(Dispatchers.IO) {
                try {
                    val response: HttpResponse = authClient.delete(baseUrl + requestUrlPath)
                    if (response.status == HttpStatusCode.Unauthorized) {
//                        resetToken()
                        throw IllegalAccessException("user not authenticated online")
                    }
                    responseHandler(response)
                    response.bodyAsText()
                } catch (ex: Exception) {
                    Log.w("Networking", "Failed to send DELETE request to $baseUrl$requestUrlPath: $ex")
                }
            }
        }

//        private fun updateToken(token: String) {
//            if (token.isEmpty()) {
//                return
//            }
//            try {
//                this.token = token
//                storeTokenToDisk(tokenFile, BearerTokens(token, ""))
// //            val jwtToken = JWT(token)
// //            tokenValid = jwtToken.expiresAt
//                tokenInterceptor.updateToken(token)
// //            Log.d("Networking", "Updated token to: $token")
// //            Log.d("Networking", "Token valid until: $tokenValid")
//            } catch (ex: Exception) {
//                Log.w("Networking", "Failed to update JWT token: $ex")
//            }
//        }

        // This is required if we delete the user but want to make a request without restarting the app
//        private fun resetToken() {
//            this.token = ""
//            storeTokenToDisk(tokenFile, BearerTokens("", ""))
//        }

//        private suspend fun refreshToken(): BearerTokens? {
//            if (userId == 0L || localUser.isEmpty()) {
//                return null
//            }
//            val response: HttpResponse =
//                authenticationClient.post("$baseUrl/v1/users/$userId/login") {
//                    contentType(ContentType.Application.Json)
//                    setBody(localUser)
//                }
//            Log.d("Networking", "Refresh token response: ${response.status}")
//            if (response.status != HttpStatusCode.OK) {
//                return null
//            }
//            val rawBody = response.bodyAsText(Charsets.UTF_8)
// //        Log.d("Networking", "Token: $rawBody")
//            val parsedBody = Json.decodeFromString<Token>(rawBody)
//            return BearerTokens(parsedBody.token, parsedBody.token)
//        }
    }
