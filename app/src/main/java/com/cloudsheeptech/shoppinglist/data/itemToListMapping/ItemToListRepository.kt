package com.cloudsheeptech.shoppinglist.data.itemToListMapping

import com.cloudsheeptech.shoppinglist.data.items.ItemToList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemToListRepository
    @Inject
    constructor(
        private val localDataSource: ItemToListLocalDataSource,
    ) {
        suspend fun create(mapping: ItemToList): Long = localDataSource.create(mapping)

        suspend fun read(mappingId: Long): ItemToList? = localDataSource.read(mappingId)

        suspend fun read(
            listId: Long,
            createdBy: Long,
        ): List<ItemToList> = localDataSource.read(listId, createdBy)

        suspend fun update(mapping: ItemToList): Long = localDataSource.update(mapping)

        suspend fun setCreatorIdForAllItems(createdBy: Long) = localDataSource.setCreatorIdForAllItems(createdBy)

        suspend fun setAddedByForAllItems(addedBy: Long) = localDataSource.setAddedByForAllItems(addedBy)

        suspend fun resetCreatorIdForAllItemsInOwnLists(createdBy: Long) = localDataSource.resetCreatorIdForAllItemsInOwnLists(createdBy)

        suspend fun resetAddedByForOwnLists(addedBy: Long) = localDataSource.resetAddedByForOwnLists(addedBy)

        suspend fun delete(mapping: ItemToList) = localDataSource.delete(mapping)

        suspend fun delete(
            itemId: Long,
            listId: Long,
            createdBy: Long,
        ) = localDataSource.delete(itemId, listId, createdBy)

        suspend fun deleteAllMappingsForList(
            listId: Long,
            createdBy: Long,
        ) = localDataSource.deleteAllMappingsForList(listId, createdBy)

        suspend fun deleteAllCheckedMappingsForList(
            listId: Long,
            createdBy: Long,
        ) = localDataSource.deleteAllCheckedMappingsForList(listId, createdBy)

        suspend fun deleteAllMappings() = localDataSource.deleteAllMappings()
    }
