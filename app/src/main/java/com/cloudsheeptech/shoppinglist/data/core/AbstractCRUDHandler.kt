package com.cloudsheeptech.shoppinglist.data.core

abstract class AbstractCRUDHandler<T : EntityIdentifier<ID>, ID : Any> : CRUDHandler<T, ID> {
    abstract val dataSource: DataSource<T, ID>

    override suspend fun create(entity: T): T = dataSource.create(entity)

    override suspend fun read(identifier: EntityIdentifier<ID>): T? = dataSource.read(identifier.getId())

    override suspend fun readAll(identifiers: List<EntityIdentifier<ID>>): List<T> = dataSource.readAll()

    override suspend fun update(updateEntity: T): T? = dataSource.update(updateEntity)

    override suspend fun delete(identifier: EntityIdentifier<ID>) = dataSource.delete(identifier.getId())
}
