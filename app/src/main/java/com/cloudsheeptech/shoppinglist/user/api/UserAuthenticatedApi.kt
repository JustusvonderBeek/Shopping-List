package com.cloudsheeptech.shoppinglist.user.api

import com.cloudsheeptech.shoppinglist.user.model.AppUser
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface UserAuthenticatedApi {
    @GET(UserApiEndpoints.ENDPOINT_PING)
    suspend fun ping()

    @PUT("todo")
    suspend fun changeUsername(
        @Body username: String,
    )

    @POST("todo")
    suspend fun logout()

    @DELETE(UserApiEndpoints.ENDPOINT_DELETE_USER)
    suspend fun delete(
        @Body user: AppUser,
    )
}
