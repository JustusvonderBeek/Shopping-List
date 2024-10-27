package com.cloudsheeptech.shoppinglist.network

import android.util.Log
import com.cloudsheeptech.shoppinglist.exception.UserAuthenticationFailedException
import com.cloudsheeptech.shoppinglist.exception.UserNotAuthenticatedException
import com.cloudsheeptech.shoppinglist.exception.UserNotCreatedException
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
                    }
                }
            }

        suspend fun GET(
            requestUrlPath: String,
            responseHandler: suspend (response: HttpResponse) -> Unit,
        ) {
            withContext(Dispatchers.IO) {
                val finalRequestUrl = "${UrlProviderEnum.BASE_URL.url}$requestUrlPath"
                try {
                    val response: HttpResponse = authClient.get(finalRequestUrl)
                    if (response.status == HttpStatusCode.Unauthorized) {
                        throw UserAuthenticationFailedException("user authentication for GET request failed")
                    }
                    responseHandler(response)
                } catch (ex: Exception) {
                    /* TODO: Check which errors this method might throw and remove this too general
                    error handling */
                    Log.e("Networking", "Failed to send GET request to $finalRequestUrl")
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
            withContext(Dispatchers.IO) {
                try {
                    var dataToPost = data
                    if (contentUpdater != null) {
                        dataToPost = contentUpdater.invoke(data)
                    }
                    post(requestUrlPath, data, responseHandler)
                } catch (ex: Exception) {
                    Log.w("Networking", "Failed to send POST request to $requestUrlPath: $ex")
                }
            }
        }

        private suspend fun post(
            requestUrlPath: String,
            data: String,
            responseHandler: suspend (HttpResponse) -> Unit,
        ) {
            withContext(Dispatchers.IO) {
                val finalRequestUrl = "${UrlProviderEnum.BASE_URL.url}$requestUrlPath"
                try {
                    val response: HttpResponse =
                        authClient.post(finalRequestUrl) {
                            setBody(data)
                        }
                    if (response.status == HttpStatusCode.Unauthorized) {
                        throw IllegalAccessException("user not authenticated online")
                    }
                    responseHandler(response)
                } catch (ex: Exception) {
                    Log.e("Networking", "Failed to send POST to $finalRequestUrl")
                }
            }
        }

        @Throws(UserNotAuthenticatedException::class, UserAuthenticationFailedException::class, UserNotCreatedException::class)
        suspend fun PUT(
            requestUrlPath: String,
            data: String,
            responseHandler: suspend (HttpResponse) -> Unit,
        ) {
            withContext(Dispatchers.IO) {
                try {
                    put(requestUrlPath, data, responseHandler)
                } catch (ex: Exception) {
                    Log.w("Networking", "Failed to send POST request to $requestUrlPath: $ex")
                }
            }
        }

        private suspend fun put(
            requestUrlPath: String,
            data: String,
            responseHandler: suspend (HttpResponse) -> Unit,
        ) {
            withContext(Dispatchers.IO) {
                val finalRequestUrl = "${UrlProviderEnum.BASE_URL.url}$requestUrlPath"
                try {
                    val response: HttpResponse =
                        authClient.put(finalRequestUrl) {
                            setBody(data)
                        }
                    if (response.status == HttpStatusCode.Unauthorized) {
                        throw IllegalAccessException("user not authenticated online")
                    }
                    responseHandler(response)
                } catch (ex: Exception) {
                    Log.w("Networking", "Failed to send POST request to $finalRequestUrl: $ex")
                }
            }
        }

        suspend fun DELETE(
            requestUrlPath: String,
            responseHandler: suspend (HttpResponse) -> Unit,
        ) {
            withContext(Dispatchers.IO) {
                val finalRequestUrl = "${UrlProviderEnum.BASE_URL.url}$requestUrlPath"
                try {
                    val response: HttpResponse = authClient.delete(finalRequestUrl)
                    if (response.status == HttpStatusCode.Unauthorized) {
                        throw IllegalAccessException("user not authenticated online")
                    }
                    responseHandler(response)
                } catch (ex: Exception) {
                    Log.w("Networking", "Failed to send DELETE request to $finalRequestUrl: $ex")
                }
            }
        }
    }
