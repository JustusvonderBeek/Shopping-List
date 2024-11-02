package com.cloudsheeptech.shoppinglist.network

enum class UrlProviderEnum(
    val url: String,
) {
    BASE_URL("https://10.0.2.2:46152"),

    //    BASE_URL("https://cloudsheeptech.com:46152"),
//    BASE_URL("https://ec2-3-120-40-62.eu-central-1.compute.amazonaws.com:46152/"),
    BASE_USER_URL("/v1/users"),
    BASE_SHOPPING_LIST_URL("/v1/lists"),
    SHOPPING_LIST_READ("/v1/list"),
    ITEM_PRICE("/v1/item/prices"),
}
