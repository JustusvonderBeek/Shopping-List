package com.cloudsheeptech.shoppinglist.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthenticationInterceptor(private val token : String) : Interceptor {

//    private val credentials = Credentials.basic(user, password)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val authenticationRequest = request.newBuilder().header("Authorization", token).build()
        return chain.proceed(authenticationRequest)
    }
}