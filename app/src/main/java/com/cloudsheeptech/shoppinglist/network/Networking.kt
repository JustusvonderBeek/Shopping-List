package com.cloudsheeptech.shoppinglist.network

import android.util.Log
import com.auth0.android.jwt.JWT
import com.cloudsheeptech.shoppinglist.user.AppUser
import com.cloudsheeptech.shoppinglist.data.DatabaseUser
import com.cloudsheeptech.shoppinglist.data.User
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.database.UserDao
//import com.cloudsheeptech.shoppinglist.network.AuthenticationInterceptor
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Duration
import java.util.Calendar
import java.util.Date

object Networking {

    private val baseUrl = "https://shop.cloudsheeptech.com:46152/"
//    private val baseUrl = "https://10.0.2.2:46152/"
    private lateinit var applicationDir : String
    private lateinit var database : ShoppingListDatabase
    private lateinit var userDao : UserDao
    private var token = ""
    private val calendar = Calendar.getInstance()
    private var tokenValid : Date? = null

    private lateinit var client : HttpClient
    private var tokenInterceptor = JwtTokenInterceptor()
    private var init = false

    @Serializable
    data class Token(
        var token : String
    )

    fun registerApplicationDir(dir : String, db: ShoppingListDatabase) {
        applicationDir = dir
        database = db
        userDao = db.userDao()
    }

    private fun loginRequired() : Boolean {
        return token == "" || tokenValid == null || tokenValid!!.before(Calendar.getInstance().time)
    }

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

    suspend fun POST(requestUrlPath: String, data : String, responseHandler: suspend (HttpResponse) -> Unit) {
        POST(requestUrlPath, data, responseHandler, null)
    }

    // The content updater is meant for the case where the user was newly created online
    // and the createdBy ID must be updated now
    suspend fun POST(requestUrlPath: String, data : String, responseHandler: suspend (HttpResponse) -> Unit, contentUpdater: (suspend (String) -> String)?) {
        withContext(Dispatchers.IO) {
            if (!init) {
                init()
            }
            if (loginRequired()) {
                login()
            }
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
            if (!init) {
                init()
            }
            if (loginRequired()) {
                login()
            }
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
            tokenValid = jwtToken.expiresAt
            tokenInterceptor.updateToken(token)
//            Log.d("Networking", "Updated token to: $token")
            Log.d("Networking", "Token valid until: $tokenValid")
        } catch (ex : Exception) {
            Log.w("Networking", "Failed to update JWT token! $ex")
        }
    }

    fun resetToken() {
        // This is required if we delete the user but want to make a request without restarting the app
        this.token = ""
        this.tokenValid = null
    }

    // One of these functions does have side-effects because the username and ID get reset
    private suspend fun login() {
        Log.d("Networking", "Performing login")
        val decodedToken = withContext(Dispatchers.IO) {
            try {
                // Should only happen when opened for the first time
                if (AppUser.isPushingUser())
                    return@withContext null
                var user = AppUser.getUser()
                if (user.ID == 0L && !AppUser.isPushingUser()) {
                    Log.i("Networking", "User was not pushed to server yet")
                    AppUser.PostUserOnlineAsync(null)
                    user = AppUser.getUser()
                    // The following operation still might fail because of an incorrect userId
                    // Therefore, update all the items in the list and try again
                }
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
            }
        }
        withContext(Dispatchers.Main) {
            init = true
        }
    }
}