package com.cloudsheeptech.shoppinglist.user.api

import com.cloudsheeptech.shoppinglist.user.model.ApiUser
import com.cloudsheeptech.shoppinglist.user.model.AppUser
import io.ktor.client.plugins.auth.providers.BearerTokens
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

interface UserUnauthenticatedApi {
    @POST(UserApiEndpoints.ENDPOINT_CREATE_USER)
    fun create(
        @Body user: AppUser,
    ): ApiUser

    // TODO: Lookup correct way to perform this operation
    @POST(UserApiEndpoints.ENDPOINT_LOGIN_USER)
    suspend fun login(
        @Body token: BearerTokens,
    )

    // TODO: Move into auth api
    @DELETE(UserApiEndpoints.ENDPOINT_DELETE_USER)
    suspend fun delete(
        @Body user: AppUser,
    )
}
