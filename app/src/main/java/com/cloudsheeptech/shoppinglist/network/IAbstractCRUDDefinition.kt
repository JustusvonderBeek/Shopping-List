package com.cloudsheeptech.shoppinglist.network

interface IAbstractCRUDDefinition<T : Any?> {

    suspend fun create(data: T, responseHandler: IHttpResponseHandler): Boolean

    suspend fun read(primaryKey: IPrimaryKey): T?

    suspend fun readAll(primaryKey: IPrimaryKey): List<T>

    suspend fun update(data: T): Boolean

    suspend fun delete(primaryKey: IPrimaryKey): Boolean
}