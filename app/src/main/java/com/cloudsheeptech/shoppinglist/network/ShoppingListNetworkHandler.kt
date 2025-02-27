package com.cloudsheeptech.shoppinglist.network

import android.util.Log
import com.cloudsheeptech.shoppinglist.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.ConnectException
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException

class ShoppingListNetworkHandler @Inject constructor(private val tokenProvider: ITokenProvider) :
    INetworking {
    private val networkClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json)
        }
        install(Auth) {
            bearer {
                refreshTokens {
                    tokenProvider.refreshToken()
                }
                loadTokens {
                    tokenProvider.loadToken()
                }
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 2000
        }
    }
    private val retryCount = BuildConfig.NETWORK_RETRY_COUNT.toInt()

    override suspend fun get(
        requestUrlPath: String,
        responseHandler: IHttpResponseHandler
    ): Boolean {
        var success = false
        for (i in 0..<retryCount) {
            success = get_internal(requestUrlPath, responseHandler)
            if (success) {
                break
            }
        }
        return success
    }

    private suspend fun get_internal(
        requestUrlPath: String,
        responseHandler: IHttpResponseHandler
    ): Boolean {
        var success = false
        withContext(Dispatchers.IO) {
            try {
                val finalRequestUrl = "${UrlProviderEnum.BASE_URL.url}$requestUrlPath"
                val response = networkClient.get(finalRequestUrl)
                success = responseHandler.handle(response)
            } catch (ex: ConnectException) {
                Log.e("ShoppingListNetworkHandler", "Network failed during GET: $ex")
            } catch (ex: ConnectTimeoutException) {
                Log.e("ShoppingListNetworkHandler", "GET network request timed out: $ex")
            } catch (ex: SSLHandshakeException) {
                Log.e("ShoppingListNetworkHandler", "SSL handshake failed during GET: $ex")
            } catch (ex: Exception) {
                Log.e("ShoppingListNetworkHandler", "Unknown exception during GET: $ex")
            }
        }
        return success
    }

    override suspend fun post(
        requestUrlPath: String,
        data: String?,
        responseHandler: IHttpResponseHandler
    ): Boolean {
        var success = false
        for (i in 0..retryCount) {
            success = post_internal(requestUrlPath, data, responseHandler)
            if (success) {
                break
            }
        }
        return success
    }

    private suspend fun post_internal(
        requestUrlPath: String,
        data: String?,
        responseHandler: IHttpResponseHandler
    ): Boolean {
        var success = false
        withContext(Dispatchers.IO) {
            try {
                val finalRequestUrl = "${UrlProviderEnum.BASE_URL.url}$requestUrlPath"
                val response = networkClient.post(finalRequestUrl) {
                    if (data != null) {
                        setBody(data)
                    }
                }
                success = responseHandler.handle(response)
            } catch (ex: ConnectException) {
                Log.e("ShoppingListNetworkHandler", "Network failed during POST: $ex")
            } catch (ex: Exception) {
                Log.e("ShoppingListNetworkHandler", "Unknown exception during POST: $ex")
            }
        }
        return success
    }

    override suspend fun post(
        requestUrlPath: String,
        data: String?,
        binaryContent: ByteArray?,
        responseHandler: IHttpResponseHandler
    ): Boolean {
        var success = false
        for (i in 0..retryCount) {
            success = post_internal(requestUrlPath, data, binaryContent, responseHandler)
            if (success) {
                break
            }
        }
        return success
    }

    private suspend fun post_internal(
        requestUrlPath: String,
        data: String?,
        binaryContent: ByteArray?,
        responseHandler: IHttpResponseHandler
    ): Boolean {
        var success = false
        withContext(Dispatchers.IO) {
            try {
                val finalRequestUrl = "${UrlProviderEnum.BASE_URL.url}$requestUrlPath"
                val response = networkClient.post(finalRequestUrl) {
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                val objectContent = data ?: ""
                                val binary = binaryContent ?: ByteArray(0)
                                append("object", objectContent)
                                append("content", binary)
                            }
                        )
                    )

                }
                success = responseHandler.handle(response)
            } catch (ex: ConnectException) {
                Log.e("ShoppingListNetworkHandler", "Network failed during POST: $ex")
            } catch (ex: Exception) {
                Log.e("ShoppingListNetworkHandler", "Unknown exception during POST: $ex")
            }
        }
        return success
    }

    override suspend fun put(
        requestUrlPath: String,
        data: String?,
        responseHandler: IHttpResponseHandler
    ): Boolean {
        var success = false
        for (i in 0..retryCount) {
            success = put_internal(requestUrlPath, data, responseHandler)
            if (success) {
                break
            }
        }
        return success
    }

    private suspend fun put_internal(
        requestUrlPath: String,
        data: String?,
        responseHandler: IHttpResponseHandler
    ): Boolean {
        var success = false
        withContext(Dispatchers.IO) {
            try {
                val finalRequestUrl = "${UrlProviderEnum.BASE_URL.url}$requestUrlPath"
                val response = networkClient.put(finalRequestUrl) {
                    setBody(data)
                }
                success = responseHandler.handle(response)
            } catch (ex: ConnectException) {
                Log.e("ShoppingListNetworkHandler", "Network failed during PUT: $ex")
            } catch (ex: Exception) {
                Log.e("ShoppingListNetworkHandler", "Unknown exception during PUT: $ex")
            }
        }
        return success
    }

    override suspend fun patch(
        requestUrlPath: String,
        data: String?,
        responseHandler: IHttpResponseHandler
    ): Boolean {
        var success = false
        for (i in 0..retryCount) {
            success = patch_internal(requestUrlPath, data, responseHandler)
            if (success) {
                break
            }
        }
        return success
    }

    private suspend fun patch_internal(
        requestUrlPath: String,
        data: String?,
        responseHandler: IHttpResponseHandler
    ): Boolean {
        var success = false
        withContext(Dispatchers.IO) {
            try {
                val finalRequestUrl = "${UrlProviderEnum.BASE_URL.url}$requestUrlPath"
                val response = networkClient.patch(finalRequestUrl) {
                    setBody(data)
                }
                success = responseHandler.handle(response)
            } catch (ex: ConnectException) {
                Log.e("ShoppingListNetworkHandler", "Network failed during PATCH: $ex")
            } catch (ex: Exception) {
                Log.e("ShoppingListNetworkHandler", "Unknown exception during PATCH: $ex")
            }
        }
        return success
    }

    override suspend fun delete(
        requestUrlPath: String,
        responseHandler: IHttpResponseHandler
    ): Boolean {
        var success = false
        for (i in 0..retryCount) {
            success = delete_internal(requestUrlPath, responseHandler)
            if (success) {
                break
            }
        }
        return success
    }

    private suspend fun delete_internal(
        requestUrlPath: String,
        responseHandler: IHttpResponseHandler
    ): Boolean {
        var success = false
        withContext(Dispatchers.IO) {
            try {
                val finalRequestUrl = "${UrlProviderEnum.BASE_URL.url}$requestUrlPath"
                val response = networkClient.delete(finalRequestUrl)
                success = responseHandler.handle(response)
            } catch (ex: ConnectException) {
                Log.e("ShoppingListNetworkHandler", "Network failed during DELETE: $ex")
            } catch (ex: Exception) {
                Log.e("ShoppingListNetworkHandler", "Unknown exception during DELETE: $ex")
            }
        }
        return success
    }
}