package com.cloudsheeptech.shoppinglist.network

import io.ktor.client.statement.HttpResponse

interface IHttpResponseHandler {

    suspend fun handle(response: HttpResponse): Boolean

}