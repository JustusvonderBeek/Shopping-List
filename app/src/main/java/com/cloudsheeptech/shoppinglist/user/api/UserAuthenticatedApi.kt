package com.cloudsheeptech.shoppinglist.user.api

import com.cloudsheeptech.shoppinglist.user.model.ApiUser
import io.ktor.client.statement.HttpResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserAuthenticatedApi {
    @GET(UserApiEndpoints.ENDPOINT_PING)
    suspend fun ping(): HttpResponse

    @PUT(UserApiEndpoints.ENDPOINT_UPDATE_USER)
    suspend fun updateUser(
        @Body user: ApiUser,
    ): HttpResponse

    @POST("todo")
    suspend fun logout()

    @DELETE(UserApiEndpoints.ENDPOINT_DELETE_USER)
    suspend fun delete(
        @Path("onlineId") onlineId: Long,
    ): HttpResponse
}
