package com.cloudsheeptech.shoppinglist.network

import android.util.Log
import com.cloudsheeptech.shoppinglist.data.ApiObjectWithOnlineId
import com.cloudsheeptech.shoppinglist.exception.UserAuthenticationFailedException
import com.cloudsheeptech.shoppinglist.exception.UserNotAuthenticatedException
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.full.memberProperties

/*
* This class captures the authentication logic of the application,
* allowing to reuse the same HTTP client for all networking requests
 */
@Singleton
class Networking
    @Inject
    constructor(
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

        /**
         * This function allows to update all objects fed into the post or delete functions
         * to contain the new user onlineId in case a user has to be created
         */
        private fun updateOnlineId(
            item: Any,
            newOnlineId: Long,
        ) {
            if (item is ApiObjectWithOnlineId) {
                item.onlineId = newOnlineId
            }
            // The idea is to recursively search for members that are ApiObject interface
            // and change the contained onlineId to the new one
            // This is using reflection
            item::class.memberProperties.forEach { property ->
                val nestedItem = property.call(item)
                if (nestedItem is ApiObjectWithOnlineId) {
                    nestedItem.onlineId = newOnlineId
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
                } catch (ex: ConnectException) {
                    Log.e("Networking", "Failed to send GET request to $finalRequestUrl")
                }
            }
        }

        suspend fun POST(
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
                        throw UserNotAuthenticatedException("user not authenticated online")
                    }
                    responseHandler(response)
                } catch (ex: ConnectException) {
                    Log.e("Networking", "failed to send POST request to $finalRequestUrl")
                }
            }
        }

        @Throws(
            UserNotAuthenticatedException::class,
        )
        suspend fun PUT(
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
                        throw UserNotAuthenticatedException("user not authenticated online")
                    }
                    responseHandler(response)
                } catch (ex: ConnectException) {
                    Log.w("Networking", "Failed to send PUT request to $finalRequestUrl: $ex")
                }
            }
        }

        suspend fun PATCH(
            requestUrlPath: String,
            data: String,
            responseHandler: suspend (HttpResponse) -> Unit,
        ) {
            withContext(Dispatchers.IO) {
                val finalRequestUrl = "${UrlProviderEnum.BASE_URL.url}$requestUrlPath"
                try {
                    val response: HttpResponse =
                        authClient.patch(finalRequestUrl) {
                            setBody(data)
                        }
                    if (response.status == HttpStatusCode.Unauthorized) {
                        throw UserNotAuthenticatedException("user not authenticated online")
                    }
                    responseHandler(response)
                } catch (ex: ConnectException) {
                    Log.w("Networking", "Failed to send PATH request to $finalRequestUrl: $ex")
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
                } catch (ex: ConnectException) {
                    Log.w("Networking", "Failed to send DELETE request to $finalRequestUrl: $ex")
                }
            }
        }
    }
