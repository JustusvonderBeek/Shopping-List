package com.cloudsheeptech.shoppinglist.user.api

object UserApiEndpoints {
    private const val PROTOCOL = "http"

    //    const val PROTOCOL = "https"
    private const val PORT = ":46152"

    //    const val BASE_ENDPOINT = "$PROTOCOL://shop.cloudsheeptech.com$PORT"
    const val BASE_ENDPOINT = "$PROTOCOL://10.0.2.2$PORT"

    const val ENDPOINT_CREATE_USER = "/create"

    const val ENDPOINT_LOGIN_USER = "/login"

    const val ENDPOINT_DELETE_USER = "/delete"
}
