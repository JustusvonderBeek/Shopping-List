package com.cloudsheeptech.shoppinglist.user.api

import com.cloudsheeptech.shoppinglist.network.token.NetworkToken
import com.cloudsheeptech.shoppinglist.user.model.ApiUser
import com.cloudsheeptech.shoppinglist.user.model.AppUser
import retrofit2.http.Body
import retrofit2.http.POST

interface UserUnauthenticatedApi {
    @POST(UserApiEndpoints.ENDPOINT_CREATE_USER)
    suspend fun create(
        @Body user: AppUser,
    ): ApiUser?

    @POST(UserApiEndpoints.ENDPOINT_LOGIN_USER)
    suspend fun login(
        @Body user: ApiUser,
    ): NetworkToken?
}
