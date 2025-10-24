package com.cloudsheeptech.shoppinglist.list.repo

import com.cloudsheeptech.shoppinglist.list.model.ShoppingList
import retrofit2.http.GET

interface ShoppingListAPI {
    // TODO: Find out about authentication, headers etc

    @GET("/v1/list/{id}")
    suspend fun get(id: Long): ShoppingList
}
