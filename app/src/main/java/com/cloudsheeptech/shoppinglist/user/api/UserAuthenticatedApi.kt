package com.cloudsheeptech.shoppinglist.user.api

import com.cloudsheeptech.shoppinglist.user.model.ApiUser
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserAuthenticatedApi {
    @GET(UserApiEndpoints.ENDPOINT_PING)
    suspend fun ping(): Response<Unit>

    @PUT(UserApiEndpoints.ENDPOINT_UPDATE_USER)
    suspend fun updateUser(
        @Path("onlineId") onlineId: Long,
        @Body user: ApiUser,
    ): Response<Unit>

    @POST(UserApiEndpoints.ENDPOINT_LOGOUT)
    suspend fun logout(): Response<Unit>

    @DELETE(UserApiEndpoints.ENDPOINT_DELETE_USER)
    suspend fun delete(
        @Path("onlineId") onlineId: Long,
    ): Response<Unit>
}
