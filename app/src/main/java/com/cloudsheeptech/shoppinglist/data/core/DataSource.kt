package com.cloudsheeptech.shoppinglist.data.core

interface DataSource<T, ID> {
    suspend fun create(entity: T): T

    suspend fun read(identifier: ID): T

    suspend fun readAll(): List<T>

    suspend fun update(entity: T): T

    suspend fun delete(identifier: ID): Boolean
}
