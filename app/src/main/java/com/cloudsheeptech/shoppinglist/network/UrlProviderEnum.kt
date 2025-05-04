package com.cloudsheeptech.shoppinglist.network

enum class UrlProviderEnum(
    var url: String,
) {
    //    BASE_URL("https://10.0.2.2:46152"),
    BASE_URL("http://10.0.2.2:46152"),

    //    BASE_URL("https://shop.cloudsheeptech.com:46152"),
    LOGIN("/v1/users/login"),

    BASE_USER_URL("/v1/users"),
    BASE_SHOPPING_LIST_URL("/v1/lists"),
    SHOPPING_LIST_SHARE("/v1/share"),
    ITEM_PRICE("/v1/item/prices"),
    RECIPE("/v1/recipe"),
    RECIPE_SHARE("/v1/recipe/share"),
    PING("/v1/ping"),
}
