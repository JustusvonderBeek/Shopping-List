package com.cloudsheeptech.shoppinglist.network

interface ICreateAccountHandler {

    suspend fun createAccount(username: String, password: String): Any {
        TODO("Not yet implemented")
    }

}