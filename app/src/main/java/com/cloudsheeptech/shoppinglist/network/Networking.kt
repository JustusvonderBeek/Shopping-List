package com.cloudsheeptech.shoppinglist.network

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
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
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/*
* This class captures the authentication logic of the application,
* allowing to reuse the same HTTP client for all networking requests
 */
// TODO: Add some way to intercept any request and create the user first online before the request is being made
@Singleton
class Networking @Inject constructor(val tokenFile: String) {

    @Serializable
    data class Token @OptIn(ExperimentalSerializationApi::class) constructor(
        @JsonNames("token")
        var token : String
    )

//    private val baseUrl = "https://shop.cloudsheeptech.com:46152/"
//    private val baseUrl = "https://ec2-3-120-40-62.eu-central-1.compute.amazonaws.com:46152/"
    private val baseUrl = "https://10.0.2.2:46152"
    private var tokenInterceptor = JwtTokenInterceptor()

    private var localUser = ""
    private var userId = 0L
    private var token = ""
    private var getUserRegisterCall : () -> Triple<String, String, (response: HttpResponse) -> Unit> = { Triple("", "", {}) }

    // Even though we might need this client only from time to time in order to
    // update the tokens or start the connections, save the effort and store it
    private val authenticationClient = HttpClient(OkHttp) {
        engine {
            config {
                hostnameVerifier { hostname, sslSession ->
                    HostnameVerification.verifyHostname(hostname, sslSession)
                }
            }
        }
    }
    private val client = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(Duration.ofSeconds(3))
                addInterceptor {
                    tokenInterceptor.intercept(it)
                }
                hostnameVerifier { hostname, sslSession ->
                    HostnameVerification.verifyHostname(hostname, sslSession)
                }
            }
        }
        install(ContentNegotiation) {
            json()
        }
        install(Auth) {
            bearer {
                loadTokens {
                    readTokenFromDisk(tokenFile)
                }
                refreshTokens {
                    Log.d("Networking", "Executing refresh")
                    val token = refreshToken() ?: return@refreshTokens null
                    updateToken(token.accessToken)
                    token
                }
            }
        }
    }

    fun resetSerializedUser(user: String, userId: Long) {
        this.localUser = user
        this.userId = userId
        resetToken()
    }

    fun registerUserCallback(getUser: () -> Triple<String, String, (response: HttpResponse) -> Unit>) {
        this.getUserRegisterCall = getUser
    }

    suspend fun GET(requestUrlPath : String, responseHandler : suspend (response : HttpResponse) -> Unit) {
        val (userToRegister, registerUrl, registerCallback) = this.getUserRegisterCall.invoke()
        withContext(Dispatchers.IO) {
            try {
                // This method should return "" only when the user is already registered
                if (userToRegister.isNotEmpty()) {
                    post(registerUrl, userToRegister, registerCallback)
                }
                get(requestUrlPath, responseHandler)
            } catch (ex : Exception) {
                Log.w("Networking", "Failed to send GET request to $baseUrl$requestUrlPath: $ex")
            }
        }
    }

    private suspend fun get(requestUrlPath: String, responseHandler: suspend (HttpResponse) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val response : HttpResponse = client.get(baseUrl + requestUrlPath)
                if (response.status == HttpStatusCode.Unauthorized) {
                    Log.w("Networking", "User not authorized")
                    resetToken()
                    return@withContext
                }
                responseHandler(response)
            } catch (ex: Exception) {
                Log.e("Networking", "Failed to send GET request to $baseUrl$requestUrlPath")
            }
        }
    }

    suspend fun POST(requestUrlPath: String, data : String, responseHandler: suspend (HttpResponse) -> Unit) {
        POST(requestUrlPath, data, responseHandler, null)
    }

    @Throws(IllegalAccessError::class)
    suspend fun POST(requestUrlPath: String, data : String, responseHandler: suspend (HttpResponse) -> Unit, contentUpdater: (suspend (String) -> String)?) {
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
                val response : HttpResponse = client.post(baseUrl + requestUrlPath) {
                    setBody(data)
                }
                if (response.status == HttpStatusCode.Unauthorized) {
                    token = ""
                    // This one is directly catched by the block outside?
                    throw IllegalAccessError("user not authenticated online")
                }
                responseHandler(response)
//                response.bodyAsText()
            } catch (ex : Exception) {
                Log.w("Networking", "Failed to send POST request to $baseUrl$requestUrlPath: $ex")
            }
        }
    }

    private suspend fun post(requestUrlPath: String, data: String, responseHandler: suspend (HttpResponse) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val response : HttpResponse = client.post(baseUrl + requestUrlPath) {
                    setBody(data)
                }
                if (response.status == HttpStatusCode.Unauthorized) {
                    token = ""
                    // This one is directly catched by the block outside?
                    throw IllegalAccessError("user not authenticated online")
                }
                responseHandler(response)
            } catch (ex: Exception) {
                Log.e("Networking", "Failed to send POST to $baseUrl$requestUrlPath")
            }
        }
    }

    @Throws(IllegalAccessError::class)
    suspend fun PUT(requestUrlPath: String, data : String, responseHandler: suspend (HttpResponse) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                var dataToPost = data
                val response : HttpResponse = client.put(baseUrl + requestUrlPath) {
                    setBody(dataToPost)
                }
                if (response.status == HttpStatusCode.Unauthorized) {
                    token = ""
                    throw IllegalAccessError("user not authenticated online")
                }
                responseHandler(response)
                response.bodyAsText()
            } catch (ex : Exception) {
                Log.w("Networking", "Failed to send POST request to $baseUrl$requestUrlPath: $ex")
            }
        }
    }

    // Because the delete method does not allow for additional data, remove this
    // from the method body
    suspend fun DELETE(requestUrlPath: String, responseHandler: suspend (HttpResponse) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val response : HttpResponse = client.delete(baseUrl + requestUrlPath)
                if (response.status == HttpStatusCode.Unauthorized) {
                    token = ""
                    throw IllegalAccessError("user not authenticated online")
                    // TODO: Decide how to handle this
                }
                responseHandler(response)
                response.bodyAsText()
            } catch (ex : Exception) {
                Log.w("Networking", "Failed to send DELETE request to $baseUrl$requestUrlPath: $ex")
            }
        }
    }

    /* We only store the latest token on disk */
    private fun readTokenFromDisk(tokenFile: String) : BearerTokens {
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

    private fun storeTokenToDisk(tokenFile: String, token: BearerTokens) {
        if (tokenFile.isEmpty()) {
            Log.w("Networking", "Given tokenFile is empty")
            return
        }
        try {
            val tokenInFileformat = Token(token.accessToken)
            val encodedToken = Json.encodeToString(tokenInFileformat)
            // Overwriting the file in case it does exist
            File(tokenFile).writeText(encodedToken)
        } catch (ex : IOException) {
            Log.w("Networking", "Failed to store token on disk: $ex")
        }
    }

    private fun updateToken(token : String) {
        if (token.isEmpty())
            return
        try {
            this.token = token
            storeTokenToDisk(tokenFile, BearerTokens(token, ""))
//            val jwtToken = JWT(token)
//            tokenValid = jwtToken.expiresAt
            tokenInterceptor.updateToken(token)
//            Log.d("Networking", "Updated token to: $token")
//            Log.d("Networking", "Token valid until: $tokenValid")
        } catch (ex : Exception) {
            Log.w("Networking", "Failed to update JWT token: $ex")
        }
    }

    // This is required if we delete the user but want to make a request without restarting the app
    private fun resetToken() {
        this.token = ""
        storeTokenToDisk(tokenFile, BearerTokens("", ""))
    }

    private suspend fun refreshToken() : BearerTokens? {
        val  response : HttpResponse = authenticationClient.post( "${baseUrl}/v1/users/${userId}/login") {
            contentType(ContentType.Application.Json)
            setBody(localUser)
        }
        Log.d("Networking", "Refresh token response: ${response.status}")
        if (response.status != HttpStatusCode.OK) {
            return null
        }
        val rawBody = response.bodyAsText(Charsets.UTF_8)
//        Log.d("Networking", "Token: $rawBody")
        val parsedBody = Json.decodeFromString<Token>(rawBody)
        return BearerTokens(parsedBody.token, parsedBody.token)
    }
}