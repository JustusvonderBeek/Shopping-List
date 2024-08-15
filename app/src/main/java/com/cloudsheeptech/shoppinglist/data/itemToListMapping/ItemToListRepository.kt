package com.cloudsheeptech.shoppinglist.data.itemToListMapping

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemToListRepository @Inject constructor(private val localDataSource: ItemToListLocalDataSource) {

    suspend fun create(mapping: ListMapping) : Long {
        return localDataSource.create(mapping)
    }

    suspend fun read(mappingId : Long) : ListMapping? {
        return localDataSource.read(mappingId)
    }

    suspend fun read(listId: Long, createdBy: Long) : List<ListMapping> {
        return localDataSource.read(listId, createdBy)
    }

    suspend fun update(mapping: ListMapping) : Long {
        return localDataSource.update(mapping)
    }

    suspend fun delete(mapping: ListMapping) {
        return localDataSource.delete(mapping)
    }

    suspend fun delete(itemId: Long, listId: Long, createdBy: Long) {
        return localDataSource.delete(itemId, listId, createdBy)
    }

    suspend fun deleteAllMappingsForList(listId: Long, createdBy: Long) {
        return localDataSource.deleteAllMappingsForList(listId, createdBy)
    }
}