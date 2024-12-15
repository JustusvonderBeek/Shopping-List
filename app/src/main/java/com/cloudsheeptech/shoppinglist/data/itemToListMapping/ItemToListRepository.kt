package com.cloudsheeptech.shoppinglist.data.itemToListMapping

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemToListRepository
@Inject
constructor(
    private val localDataSource: ItemToListLocalDataSource,
) {
    suspend fun create(mapping: ListMapping): Long = localDataSource.create(mapping)

    suspend fun read(mappingId: Long): ListMapping? = localDataSource.read(mappingId)

    suspend fun read(
        listId: Long,
        createdBy: Long,
    ): List<ListMapping> = localDataSource.read(listId, createdBy)

    suspend fun update(mapping: ListMapping): Long = localDataSource.update(mapping)

    suspend fun setCreatorIdForAllItems(createdBy: Long) =
        localDataSource.setCreatorIdForAllItems(createdBy)

    suspend fun setAddedByForAllItems(addedBy: Long) =
        localDataSource.setAddedByForAllItems(addedBy)

    suspend fun resetCreatorIdForAllItemsInOwnLists(createdBy: Long) =
        localDataSource.resetCreatorIdForAllItemsInOwnLists(createdBy)

    suspend fun resetAddedByForOwnLists(addedBy: Long) =
        localDataSource.resetAddedByForOwnLists(addedBy)

    suspend fun delete(mapping: ListMapping) = localDataSource.delete(mapping)

    suspend fun delete(
        itemId: Long,
        listId: Long,
        createdBy: Long,
    ) = localDataSource.delete(itemId, listId, createdBy)

    suspend fun deleteAllMappingsForList(
        listId: Long,
        createdBy: Long,
    ) = localDataSource.deleteAllMappingsForList(listId, createdBy)

    suspend fun deleteAllMappings() = localDataSource.deleteAllMappings()
}
