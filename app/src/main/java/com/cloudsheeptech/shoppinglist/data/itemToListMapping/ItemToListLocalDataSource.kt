package com.cloudsheeptech.shoppinglist.data.itemToListMapping

import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.items.ItemToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemToListLocalDataSource
    @Inject
    constructor(
        database: ShoppingListDatabase,
    ) {
        private val mappingDao = database.mappingDao()

        /**
         * Creates the mapping of item to list in the database
         * @throws IllegalArgumentException if the mapping has an id != 0L
         * @throws IllegalStateException if the mapping already exists
         * @return the id of the newly created mapping
         */
        suspend fun create(mapping: ItemToList): Long {
            var mappingId = 0L
            if (mapping.itemId != 0L) {
                throw IllegalArgumentException("mapping already exists")
            }
            withContext(Dispatchers.IO) {
                val existingMappings =
                    mappingDao.getMappingForItemAndList(
                        mapping.itemId,
                        mapping.listId,
                        mapping.createdBy,
                    )
                if (existingMappings.isNotEmpty()) {
                    throw IllegalStateException("mapping already exists")
                }
                mappingId = mappingDao.insertMapping(mapping)
            }
            return mappingId
        }

        /**
         * Searches for and returns a single mapping for the given ID
         * @return the mapping if found or null
         */
        suspend fun read(mappingId: Long): ItemToList? {
            var dbMapping: ItemToList? = null
            withContext(Dispatchers.IO) {
                dbMapping = mappingDao.getMapping(mappingId)
            }
            return dbMapping
        }

        /**
         * Searches for and reads all mappings found for a given list
         * @return the mappings for the queried list
         */
        suspend fun read(
            listId: Long,
            createdBy: Long,
        ): List<ItemToList> {
            val foundMappings = mutableListOf<ItemToList>()
            withContext(Dispatchers.IO) {
                val dbMappings = mappingDao.getMappingsForList(listId, createdBy)
//            Log.d("ItemToListLocalDataSource", "Found ${dbMappings.size} items for list $listId from $createdBy")
                foundMappings.addAll(dbMappings)
            }
            return foundMappings
        }

        /**
         * Updates an existing mappings if it exists
         * @throws IllegalStateException if the mapping does not exist
         * @return the id of the updated mapping
         */
        suspend fun update(mapping: ItemToList): Long {
            var updateMappingId = 0L
            withContext(Dispatchers.IO) {
                var existingMappings =
                    mappingDao.getMappingForItemAndList(
                        mapping.itemId,
                        mapping.listId,
                        mapping.createdBy,
                    )
                if (existingMappings.isEmpty()) {
//                throw IllegalStateException("mapping does not exists")
                    val existingMapping =
                        mappingDao.getMapping(mapping.itemId)
                            ?: throw IllegalStateException("mapping does not exist")
                    existingMappings = listOf(existingMapping)
                }
                if (existingMappings.size > 1) {
                    throw IllegalStateException("found more than a single mapping for the same item")
                }
                // Allows to update the mapping even in cases where the data was received
                // from remote and the id is not set
                mapping.itemId = existingMappings[0].itemId
                mappingDao.updateMapping(mapping)
                updateMappingId = mapping.itemId
            }
            return updateMappingId
        }

        suspend fun setCreatorIdForAllItems(createdBy: Long) {
            withContext(Dispatchers.IO) {
                mappingDao.setCreatorIdForAllItems(createdBy)
            }
        }

        suspend fun setAddedByForAllItems(addedBy: Long) {
            withContext(Dispatchers.IO) {
                mappingDao.setAddedByIdForItemsInOwnLists(addedBy)
            }
        }

        suspend fun resetCreatorIdForAllItemsInOwnLists(createdBy: Long) {
            withContext(Dispatchers.IO) {
                mappingDao.resetCreatorIdForItemsInOwnLists(createdBy)
            }
        }

        suspend fun resetAddedByForOwnLists(addedBy: Long) {
            withContext(Dispatchers.IO) {
                mappingDao.resetAddedByIdForItemsInOwnLists(addedBy)
            }
        }

        suspend fun delete(mapping: ItemToList) {
            withContext(Dispatchers.IO) {
                mappingDao.deleteMappingItemListId(mapping.itemId, mapping.listId, mapping.createdBy)
            }
        }

        suspend fun delete(
            itemId: Long,
            listId: Long,
            createdBy: Long,
        ) {
            withContext(Dispatchers.IO) {
                mappingDao.deleteMappingItemListId(itemId, listId, createdBy)
            }
        }

        suspend fun deleteAllMappingsForList(
            listId: Long,
            createdBy: Long,
        ) {
            withContext(Dispatchers.IO) {
                mappingDao.deleteMappingsForListId(listId, createdBy)
            }
        }

        suspend fun deleteAllCheckedMappingsForList(
            listId: Long,
            createdBy: Long,
        ) {
            withContext(Dispatchers.IO) {
                mappingDao.deleteCheckedMappingsForListId(listId, createdBy)
            }
        }

        suspend fun deleteAllMappings() {
            withContext(Dispatchers.IO) {
                mappingDao.deleteAllMappings()
            }
        }
    }
