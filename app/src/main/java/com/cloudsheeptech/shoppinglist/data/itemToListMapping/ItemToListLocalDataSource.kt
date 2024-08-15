package com.cloudsheeptech.shoppinglist.data.itemToListMapping

import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ItemToListLocalDataSource @Inject constructor(database: ShoppingListDatabase) {

    private val mappingDao = database.mappingDao()

    /**
     * Creates the mapping of item to list in the database
     * @throws IllegalArgumentException if the mapping has an id != 0L
     * @throws IllegalStateException if the mapping already exists
     * @return the id of the newly created mapping
     */
    suspend fun create(mapping: ListMapping) : Long {
        var mappingId = 0L
        if (mapping.ID != 0L) {
            throw IllegalArgumentException("mapping already exists")
        }
        withContext(Dispatchers.IO) {
            val existingMappings = mappingDao.getMappingForItemAndList(mapping.ItemID, mapping.ListID, mapping.CreatedBy)
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
    suspend fun read(mappingId: Long) : ListMapping? {
        var dbMapping : ListMapping? = null
        withContext(Dispatchers.IO) {
            dbMapping = mappingDao.getMapping(mappingId)
        }
        return dbMapping
    }

    /**
     * Searches for and reads all mappings found for a given list
     * @return the mappings for the queried list
     */
    suspend fun read(listId: Long, createdBy: Long) : List<ListMapping> {
        val foundMappings = mutableListOf<ListMapping>()
        withContext(Dispatchers.IO) {
            val dbMappings = mappingDao.getMappingsForList(listId, createdBy)
            foundMappings.addAll(dbMappings)
        }
        return foundMappings
    }

    /**
     * Updates an existing mappings if it exists
     * @throws IllegalStateException if the mapping does not exist
     * @return the id of the updated mapping
     */
    suspend fun update(mapping: ListMapping) : Long {
        var updateMappingId = 0L
        withContext(Dispatchers.IO) {
            val existingMappings = mappingDao.getMappingForItemAndList(mapping.ItemID, mapping.ListID, mapping.CreatedBy)
            if (existingMappings.isEmpty()) {
                throw IllegalStateException("mapping does not exists")
            }
            if (existingMappings.size > 1) {
                throw IllegalStateException("found more than a single mapping for the same item")
            }
            // Allows to update the mapping even in cases where the data was received
            // from remote and the id is not set
            mapping.ID = existingMappings[0].ID
            mappingDao.updateMapping(mapping)
            updateMappingId = mapping.ID
        }
        return updateMappingId
    }

    suspend fun delete(mapping: ListMapping) {
        withContext(Dispatchers.IO) {
            mappingDao.deleteMapping(mapping.ID)
        }
    }

    suspend fun delete(itemId: Long, listId: Long, createdBy: Long) {
        withContext(Dispatchers.IO) {
            mappingDao.deleteMappingItemListId(itemId, listId, createdBy)
        }
    }

    suspend fun deleteAllMappingsForList(listId: Long, createdBy: Long) {
        withContext(Dispatchers.IO) {
            mappingDao.deleteMappingsForListId(listId, createdBy)
        }
    }

}