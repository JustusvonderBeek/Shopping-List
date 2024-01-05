package com.cloudsheeptech.shoppinglist.network

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.http.HttpHeaders
import okhttp3.Interceptor
import okhttp3.Response

class JwtTokenInterceptor : Interceptor {

    private var token : String? = null

    fun updateToken(token : String) {
        this.token = token
    }

    override fun intercept(chain : Interceptor.Chain) : Response {
        // Proceed anyways (e.g. for the login) if no token is found
        if (token == null || token!!.isEmpty()) {
            return chain.proceed(chain.request())
        }
        val request = chain.request()
        // Get the current JWT token
        val bearerToken = "Bearer $token"
        val authenticatedRequest = request.newBuilder().header("Authorization", bearerToken).build()
        return chain.proceed(authenticatedRequest)
    }
}