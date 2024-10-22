package com.cloudsheeptech.shoppinglist.network

enum class UrlProviderEnum(
    val value: String,
) {
    BASE_URL("https://10.0.2.2:46152"),
    USER_CREATION("/v1/users"),
    USER_LOGIN("/v1/users"),
}
