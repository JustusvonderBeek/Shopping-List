package com.cloudsheeptech.shoppinglist.data.core

interface CRUDHandler<T : EntityIdentifier<ID>, ID : Any> {
    suspend fun create(entity: T): T

    suspend fun read(identifier: EntityIdentifier<ID>): T?

    suspend fun readAll(identifiers: List<EntityIdentifier<ID>>): List<T>

    suspend fun update(newObject: T): T?

    suspend fun delete(identifier: EntityIdentifier<ID>): Boolean
}
