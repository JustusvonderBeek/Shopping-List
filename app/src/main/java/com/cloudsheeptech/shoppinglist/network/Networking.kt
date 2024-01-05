package com.cloudsheeptech.shoppinglist.network

import android.app.Application
import android.util.Log
import com.auth0.android.jwt.JWT
import com.cloudsheeptech.shoppinglist.data.AuthenticationInterceptor
import com.cloudsheeptech.shoppinglist.data.User
//import com.cloudsheeptech.shoppinglist.data.AuthenticationInterceptor
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.caseInsensitiveMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Request
import java.io.File
import java.util.Calendar
import java.util.Date
import kotlin.math.log

object Networking {

//    private val baseUrl = "https://shop.cloudsheeptech.com:46152/"
    private val baseUrl = "https://10.0.2.2:46152/"
    private lateinit var applicationDir : String
    private var token = ""
    private val calendar = Calendar.getInstance()
    private var tokenValid : Date? = null

    private lateinit var client : HttpClient
    private var tokenInterceptor = JwtTokenInterceptor()
    private var init = false
    private var login = false

    @Serializable
    data class Token(
        var token : String
    )

    fun registerApplicationDir(dir : String) {
        applicationDir = dir
    }

    private fun loginRequired() : Boolean {
        return token == "" || tokenValid == null || tokenValid!!.before(Calendar.getInstance().time)
    }

    // TODO: Automatically login / authorize and repeat the request
    suspend fun GET(requestUrlPath : String, responseHandler : suspend (response : HttpResponse) -> Unit) {
        withContext(Dispatchers.IO) {
            if (!init) {
                init()
            }
            if (loginRequired()) {
                login()
            }
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

    suspend fun POST(requestUrlPath: String, data : String, responseHandler: suspend (HttpResponse) -> Unit) : String {
        withContext(Dispatchers.IO) {
            if (!init) {
                init()
            }
            if (loginRequired()) {
                login()
            }
            try {
                val response : HttpResponse = client.post(baseUrl + requestUrlPath) {
                    setBody(data)
                }
                if (response.status == HttpStatusCode.Unauthorized) {
                    token = ""
                }
                responseHandler(response)
                response.bodyAsText()
            } catch (ex : Exception) {
                Log.w("Networking", "Failed to send POST request to $baseUrl$requestUrlPath: $ex")
            }
        }
        return "Error"
    }

    private fun updateToken(token : String) {
        if (token.isEmpty())
            return
        try {
            this.token = token
            val jwtToken = JWT(token)
            tokenValid = jwtToken.expiresAt
            tokenInterceptor.updateToken(token)
//            Log.d("Networking", "Updated token to: $token")
            Log.d("Networking", "Token valid until: $tokenValid")
        } catch (ex : Exception) {
            Log.w("Networking", "Failed to update JWT token! $ex")
        }
    }

    private suspend fun login() {
        Log.d("Networking", "Performing login")
        val decodedToken = withContext(Dispatchers.IO) {
            try {
                val file = File(applicationDir, "user.json")
                if (!file.exists()) {
                    Log.d("Networking", "User file does not exist!")
                    return@withContext null
                }
                val reader = file.reader(Charsets.UTF_8)
                val content = reader.readText()
//                Log.d("Networking", "Content of file '$content'")
                val user = Json.decodeFromString<User>(content)
                val response: HttpResponse = client.post(baseUrl + "auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(user)
                }
                if (response.status != HttpStatusCode.OK) {
                    Log.w("Networking", "Login failed!")
                    return@withContext null
                }
                // Extracting token and storing it locally
                val body = response.bodyAsText(Charsets.UTF_8)
//                Log.d("Networking", "Login got $body as response")
                return@withContext Json.decodeFromString<Token>(body)
            } catch (ex: Exception) {
                Log.w("Networking", "Failed to login: $ex")
            }
            return@withContext null
        }
        if (decodedToken == null) {
            Log.d("Networking", "Cannot login because token is nil")
            return
        }
        withContext(Dispatchers.Main) {
            updateToken(decodedToken.token)
            init = false
        }
    }

    private suspend fun init() {
        withContext(Dispatchers.IO) {
            client = HttpClient(OkHttp) {
                engine {
                    config {
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
//                install(Auth) {
//                    bearer {
//                        loadTokens {
//                            BearerTokens(token, token)
//                        }
//                        sendWithoutRequest { request ->
//                            request.url.host == "10.0.2.2" || request.url.host == "shop.cloudsheeptech.com"
//                        }
//                    }
//                }
            }
        }
        withContext(Dispatchers.Main) {
            init = true
        }
    }
}