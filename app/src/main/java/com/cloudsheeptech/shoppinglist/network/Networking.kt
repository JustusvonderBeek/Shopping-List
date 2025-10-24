package com.cloudsheeptech.shoppinglist.network

import android.util.Log
import com.cloudsheeptech.shoppinglist.list.model.UserAuthenticationFailedException
import com.cloudsheeptech.shoppinglist.list.model.UserNotAuthenticatedException
import com.cloudsheeptech.shoppinglist.network.token.ShoppingListAuthenticationTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLHandshakeException

/*
* This class captures the authentication logic of the application,
* allowing to reuse the same HTTP client for all networking requests
 */
@Singleton
class Networking
    @Inject
    constructor(
        private val tokenProvider: ShoppingListAuthenticationTokenProvider,
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
                            tokenProvider.loadToken()
                        }
                        refreshTokens {
                            tokenProvider.refreshTokenAndCreateUserIfNotExists()
                        }
                    }
                }
            }

        suspend fun get(
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
                    Log.e("Networking", "Failed to send GET request to $finalRequestUrl: $ex")
                } catch (ex: SocketTimeoutException) {
                    Log.e("Networking", "Timeout while reading from remote: $ex")
                } catch (ex: SSLHandshakeException) {
                    Log.e("Networking", "SSL handshake failed during GET: $ex")
                }
            }
        }

        suspend fun POST(
            requestUrlPath: String,
            data: String?,
            responseHandler: suspend (HttpResponse) -> Unit,
        ) {
            withContext(Dispatchers.IO) {
                val finalRequestUrl = "${UrlProviderEnum.BASE_URL.url}$requestUrlPath"
                try {
                    val response: HttpResponse =
                        authClient.post(finalRequestUrl) {
                            if (!data.isNullOrEmpty()) {
                                setBody(data)
                            }
                        }
                    if (response.status == HttpStatusCode.Unauthorized) {
                        throw UserNotAuthenticatedException("user not authenticated online")
                    }
                    responseHandler(response)
                } catch (ex: ConnectException) {
                    Log.e("Networking", "failed to send POST request to $finalRequestUrl: $ex")
                } catch (ex: SocketTimeoutException) {
                    Log.e("Networking", "POST request to $finalRequestUrl timed out: $ex")
                } catch (ex: SSLHandshakeException) {
                    Log.e("Networking", "SSL handshake failed during POST: $ex")
                }
            }
        }

        @Throws(UserNotAuthenticatedException::class)
        suspend fun MULTIFORM_POST(
            requestUrlPath: String,
            data: String?,
            binaryContent: List<ByteArray>,
            responseHandler: suspend (HttpResponse) -> Unit,
        ) {
            withContext(Dispatchers.IO) {
                val finalRequestUrl = "${UrlProviderEnum.BASE_URL.url}$requestUrlPath"
                try {
                    val response =
                        authClient.post(finalRequestUrl) {
                            setBody(
                                MultiPartFormDataContent(
                                    formData {
                                        append("object", data ?: "")
                                        if (binaryContent.isNotEmpty()) {
                                            for ((i, bytes) in binaryContent.withIndex()) {
                                                append(
                                                    "content",
                                                    bytes,
                                                    Headers.build {
                                                        append(HttpHeaders.ContentType, "image/png")
                                                        append(
                                                            HttpHeaders.ContentDisposition,
                                                            "filename=\"$i.png\"",
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                    },
                                ),
                            )
                        }
                    if (response.status == HttpStatusCode.Unauthorized) {
                        throw UserNotAuthenticatedException("user not authenticated online")
                    }
                    responseHandler(response)
                } catch (ex: ConnectException) {
                    Log.e(
                        "Networking",
                        "Failed to send MULTIFORM_POST request to $finalRequestUrl: $ex",
                    )
                } catch (ex: SocketTimeoutException) {
                    Log.e("Networking", "MULTIFORM_POST request to $finalRequestUrl timed out: $ex")
                } catch (ex: SSLHandshakeException) {
                    Log.e("Networking", "SSL handshake failed during M_POST: $ex")
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
                } catch (ex: SSLHandshakeException) {
                    Log.e("Networking", "SSL handshake failed during PUT: $ex")
                }
            }
        }

        suspend fun PUT_MULTIPART(
            requestUrlPath: String,
            data: String,
            binaryContent: List<ByteArray>,
            responseHandler: suspend (HttpResponse) -> Unit,
        ) {
            withContext(Dispatchers.IO) {
                val finalRequestUrl = "${UrlProviderEnum.BASE_URL.url}$requestUrlPath"
                try {
                    val response: HttpResponse =
                        authClient.put(finalRequestUrl) {
                            setBody(
                                MultiPartFormDataContent(
                                    formData {
                                        append("object", data ?: "")
                                        if (binaryContent.isNotEmpty()) {
                                            for ((i, bytes) in binaryContent.withIndex()) {
                                                append(
                                                    "content",
                                                    bytes,
                                                    Headers.build {
                                                        append(HttpHeaders.ContentType, "image/png")
                                                        append(
                                                            HttpHeaders.ContentDisposition,
                                                            "filename=\"$i.png\"",
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                    },
                                ),
                            )
                        }
                    if (response.status == HttpStatusCode.Unauthorized) {
                        throw UserNotAuthenticatedException("user not authenticated online")
                    }
                    responseHandler(response)
                } catch (ex: ConnectException) {
                    Log.w("Networking", "Failed to send PUT request to $finalRequestUrl: $ex")
                } catch (ex: SSLHandshakeException) {
                    Log.e("Networking", "SSL handshake failed during PUT: $ex")
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
                } catch (ex: SSLHandshakeException) {
                    Log.e("Networking", "SSL handshake failed during PATCH: $ex")
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
                        throw UserNotAuthenticatedException("user not authenticated online")
                    }
                    responseHandler(response)
                } catch (ex: ConnectException) {
                    Log.w("Networking", "Failed to send DELETE request to $finalRequestUrl: $ex")
                } catch (ex: SSLHandshakeException) {
                    Log.e("Networking", "SSL handshake failed during DELETE: $ex")
                }
            }
        }
    }
