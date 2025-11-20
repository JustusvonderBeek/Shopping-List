package com.cloudsheeptech.shoppinglist.list.api

object ShoppingListEndpoints {
    const val PROTOCOL = "http"

    //    const val PROTOCOL = "https"
    const val PORT = ":46152"

    //    const val BASE_ENDPOINT = "$PROTOCOL://shop.cloudsheeptech.com$PORT"
    const val BASE_ENDPOINT = "$PROTOCOL://10.0.2.2$PORT"

    // Endpoints for modification
    const val ENDPOINT_PERFORM_OPERATION = "/v1/lists/{listId}"
    const val ENDPOINT_CREATE_LIST = "/v1/lists"
    const val ENDPOINT_UPDATE_TITLE = "/list/{listId}/title"
    const val ENDPOINT_ADD_ITEM = "/list/{listId}/addItem"

    // Endpoints for reading data
    const val ENDPOINT_READ_ALL = "/lists"
}
