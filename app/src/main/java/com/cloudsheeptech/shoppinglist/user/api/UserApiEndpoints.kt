package com.cloudsheeptech.shoppinglist.user.api

object UserApiEndpoints {
    private const val PROTOCOL = "http"

    //    const val PROTOCOL = "https"
    private const val PORT = ":46152"

    //    const val BASE_ENDPOINT = "$PROTOCOL://shop.cloudsheeptech.com$PORT"
    const val BASE_ENDPOINT = "$PROTOCOL://10.0.2.2$PORT"

    // Unauthenticated

    const val ENDPOINT_PING = "/v1/ping"

    const val ENDPOINT_LOGIN_USER = "/v1/users/login/{onlineId}"

    const val ENDPOINT_CREATE_USER = "/v1/users"

    // Authenticated

    const val ENDPOINT_DELETE_USER = "/v1/users/{onlineId}"

    const val ENDPOINT_UPDATE_USER = "/v1/users/{onlineId}"

    const val ENDPOINT_LOGOUT = "/v1/users/logout"
}
