package com.cloudsheeptech.shoppinglist.list.api

import com.cloudsheeptech.shoppinglist.list.model.ApiResult
import com.cloudsheeptech.shoppinglist.list.model.ShoppingList
import io.ktor.client.statement.HttpResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ShoppingListApi {
    // --------------- List modifications --------------------
    @POST(ShoppingListEndpoints.ENDPOINT_CREATE_LIST)
    suspend fun create(list: ShoppingList): ApiResult

    @PUT(ShoppingListEndpoints.ENDPOINT_UPDATE_TITLE)
    suspend fun updateTitle(
        @Path("listId") listId: Long,
        newTitle: String,
    ): HttpResponse

    // --------------- List retrieval --------------------
    @GET(ShoppingListEndpoints.ENDPOINT_READ_ALL)
    suspend fun readAllRemote(): List<ShoppingList>
}
