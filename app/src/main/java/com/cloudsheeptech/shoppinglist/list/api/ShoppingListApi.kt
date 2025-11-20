package com.cloudsheeptech.shoppinglist.list.api

import com.cloudsheeptech.shoppinglist.list.model.AppItem
import com.cloudsheeptech.shoppinglist.list.model.QuantityType
import com.cloudsheeptech.shoppinglist.list.model.ShoppingList
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListApiOperation
import io.ktor.client.statement.HttpResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ShoppingListApi {
    // --------------- List modifications --------------------
    @POST(ShoppingListEndpoints.ENDPOINT_CREATE_LIST)
    suspend fun create(
        @Body list: ShoppingList,
    ): Response<Unit>

    @PUT(ShoppingListEndpoints.ENDPOINT_UPDATE_TITLE)
    suspend fun updateTitle(
        @Path("listId") listId: Long,
        @Body newTitle: String,
    ): HttpResponse

    @PUT(ShoppingListEndpoints.ENDPOINT_ADD_ITEM)
    suspend fun addItem(
        @Path("listId") listId: Long,
        @Body addedItem: AppItem,
    ): HttpResponse

    @PUT(ShoppingListEndpoints.ENDPOINT_ADD_ITEM)
    suspend fun changeQuantityItem(
        @Path("listId") listId: Long,
        @Body quantityType: QuantityType,
    ): HttpResponse

    // TODO: Adapt return type to list of delta operations which need to
    // be performed locally
    @POST(ShoppingListEndpoints.ENDPOINT_PERFORM_OPERATION)
    suspend fun performOperations(
        @Body operations: List<@JvmSuppressWildcards ShoppingListApiOperation>,
    ): Response<Unit>

    // --------------- List retrieval --------------------
    @GET(ShoppingListEndpoints.ENDPOINT_READ_ALL)
    suspend fun readAllRemote(): List<ShoppingList>
}
