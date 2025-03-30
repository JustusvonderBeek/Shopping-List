package com.cloudsheeptech.shoppinglist.network

import com.cloudsheeptech.shoppinglist.data.list.ApiShoppingList
import com.cloudsheeptech.shoppinglist.data.recipe.ApiRecipe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ShoppingListAPI {
    @GET("/v1/list/{id}")
    suspend fun get(id: Long): ApiShoppingList

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    @Multipart
    @POST("v1/recipe")
    suspend fun createRecipe(
        @Part("object") recipe: ApiRecipe,
        @Part("content") images: Array<Byte>,
    )
}
