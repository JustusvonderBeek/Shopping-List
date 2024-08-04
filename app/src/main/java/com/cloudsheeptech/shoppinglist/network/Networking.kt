package com.cloudsheeptech.shoppinglist.network

import android.util.Log
import com.auth0.android.jwt.JWT
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.user.AppUserDao
//import com.cloudsheeptech.shoppinglist.network.AuthenticationInterceptor
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
import io.ktor.util.cio.writeChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Duration
import java.util.Calendar
import java.util.Date

/*
* This class captures the authentication logic of the application,
* allowing to reuse the same HTTP client for all networking requests
 */
class Networking(tokenFile: String) {

    @Serializable
    data class Token(
        var token : String
    )

//    private val baseUrl = "https://shop.cloudsheeptech.com:46152/"
//    private val baseUrl = "https://ec2-3-120-40-62.eu-central-1.compute.amazonaws.com:46152/"
    private val baseUrl = "https://10.0.2.2:46152/"
    private var tokenInterceptor = JwtTokenInterceptor()

    private var localUser = ""
    private lateinit var applicationDir : String
    private var token = ""

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
                hostnameVerifier {
                    // TODO: Include verification of the hostname
                        _, _ -> true
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


    /* We only store the latest token on disk */
    private fun readTokenFromDisk(tokenFile: String) : BearerTokens {
        var token = BearerTokens("", "")
        if (!File(tokenFile).exists()) {
            Log.d("Networking", "Token File does not exist")
            return token
        }
        val content = File(tokenFile).readText(Charsets.UTF_8)
        try {
            val decodedToken = Json.decodeFromString<Token>(content)
            token = BearerTokens(decodedToken.token, "")
        } catch (ex: SerializationException) {
            Log.d("Networking", "The type of the token file is in incorrect format!")
        }
        return token
    }

    private fun storeTokenToDisk(tokenFile: String, token: BearerTokens) {
        val tokenInFileformat = Token(token.accessToken)
        val encodedToken = Json.encodeToString(tokenInFileformat)
        // Overwriting the file in case it does exist
        File(tokenFile).writeText(encodedToken)
    }

    fun resetSerializedUser(user: String) {
        this.localUser = user
    }

//    fun registerApplicationDir(dir : String, db: ShoppingListDatabase) {
//        applicationDir = dir
//        database = db
//        appUserDao = db.userDao()
//    }

    suspend fun GET(requestUrlPath : String, responseHandler : suspend (response : HttpResponse) -> Unit) {
        withContext(Dispatchers.IO) {
//            if (!init) {
//                init(localUser)
//            }
//            if (loginRequired()) {
//                login()
//            }
            try {
                val response : HttpResponse = client.get(baseUrl + requestUrlPath)
                if (response.status == HttpStatusCode.Unauthorized) {
                    token = ""
                }
                responseHandler(response)
            } catch (ex : Exception) {
                Log.w("Networking", "Failed to send GET request to $baseUrl$requestUrlPath: $ex")
            }
        }
    }

    suspend fun POST(requestUrlPath: String, data : String, responseHandler: suspend (HttpResponse) -> Unit) {
        POST(requestUrlPath, data, responseHandler, null)
    }

    @Throws(IllegalAccessError::class)
    suspend fun POST(requestUrlPath: String, data : String, responseHandler: suspend (HttpResponse) -> Unit, contentUpdater: (suspend (String) -> String)?) {
        withContext(Dispatchers.IO) {
//            if (!init) {
//                init(localUser)
//            }
////            if (loginRequired()) {
//                login()
//            }
            try {
                var dataToPost = data
                if (contentUpdater != null) {
                    dataToPost = contentUpdater.invoke(data)
                }
                val response : HttpResponse = client.post(baseUrl + requestUrlPath) {
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

    @Throws(IllegalAccessError::class)
    suspend fun PUT(requestUrlPath: String, data : String, responseHandler: suspend (HttpResponse) -> Unit) {
        withContext(Dispatchers.IO) {
//            if (!init) {
//                init(localUser)
//            }
//            if (loginRequired()) {
//                login()
//            }
            try {
                var dataToPost = data
//                if (contentUpdater != null) {
//                    dataToPost = contentUpdater.invoke(data)
//                }
                val response : HttpResponse = client.put(baseUrl + requestUrlPath) {
                    setBody(dataToPost)
                }
                if (response.status == HttpStatusCode.Unauthorized) {
                    token = "token from put"
                    throw IllegalAccessError("user not authenticated online")
                }
                responseHandler(response)
                response.bodyAsText()
            } catch (ex : Exception) {
                Log.w("Networking", "Failed to send POST request to $baseUrl$requestUrlPath: $ex")
            }
        }
    }

    suspend fun DELETE(requestUrlPath: String, data: String, responseHandler: suspend (HttpResponse) -> Unit) {
        withContext(Dispatchers.IO) {
//            if (!init) {
//                init(localUser)
//            }
//            if (loginRequired()) {
//                login()
//            }
            try {
                val response : HttpResponse = client.delete(baseUrl + requestUrlPath) {
                    if (data != "") {
                        setBody(data)
                    }
                }
                if (response.status == HttpStatusCode.Unauthorized) {
                    token = ""
                }
                responseHandler(response)
                response.bodyAsText()
            } catch (ex : Exception) {
                Log.w("Networking", "Failed to send DELETE request to $baseUrl$requestUrlPath: $ex")
            }
        }
    }

    private fun updateToken(token : String) {
        if (token.isEmpty())
            return
        try {
            this.token = token
            val jwtToken = JWT(token)
//            tokenValid = jwtToken.expiresAt
            tokenInterceptor.updateToken(token)
//            Log.d("Networking", "Updated token to: $token")
//            Log.d("Networking", "Token valid until: $tokenValid")
        } catch (ex : Exception) {
            Log.w("Networking", "Failed to update JWT token! $ex")
        }
    }

    fun resetToken() {
        // This is required if we delete the user but want to make a request without restarting the app
//        this.token = ""
//        this.tokenValid = null
    }

    private suspend fun refreshToken() : BearerTokens? {
        val  response : HttpResponse = authenticationClient.post( "${baseUrl}auth/login") {
            contentType(ContentType.Application.Json)
            setBody(localUser)
        }
        Log.d("Networking", "Refresh token response: ${response.status}")
        if (response.status != HttpStatusCode.OK) {
            return null
        }
        val rawBody = response.bodyAsText(Charsets.UTF_8)
        val parsedBody = Json.decodeFromString<Token>(rawBody)
        return BearerTokens(parsedBody.token, "")
    }
}