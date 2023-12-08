package com.cloudsheeptech.shoppinglist.network

import android.util.Log
import com.cloudsheeptech.shoppinglist.data.AuthenticationInterceptor
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Networking {

//    private val baseUrl = "https://vocabulary.cloudsheeptech.com:41308/"
    private val baseUrl = "https://10.0.2.2:41308/"

    private lateinit var client : HttpClient
    private var init = false

    suspend fun GET(requestUrlPath : String, responseHandler : (response : HttpResponse) -> Unit) {
        withContext(Dispatchers.IO) {
            if (!init) {
                init()
            }
            try {
                val response : HttpResponse = client.get(baseUrl + requestUrlPath)
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
            try {
                val response : HttpResponse = client.post(baseUrl + requestUrlPath) {
                    setBody(data)
                }
                responseHandler(response)
                 response.bodyAsText()
            } catch (ex : Exception) {
                Log.w("Networking", "Failed to send POST request to $baseUrl$requestUrlPath: $ex")
            }
        }
        return "Error"
    }

    private suspend fun init() {
        withContext(Dispatchers.IO) {
            client = HttpClient(OkHttp) {
                engine {
                 addInterceptor(AuthenticationInterceptor(token = "todo"))
                    config {
                        hostnameVerifier {
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