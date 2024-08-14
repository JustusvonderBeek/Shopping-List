package com.cloudsheeptech.shoppinglist.data.items

class ItemRepository(private val localDataSource: ItemLocalDataSource) {

    /**
     * Searches for the item stored under this id
     * @return the item if found or null
     */
    suspend fun read(itemId: Long) : DbItem? {
        val itemList = readByIds(listOf(itemId))
        if (itemList.isEmpty())
            return null
        return itemList[0]
    }

    /**
     * Returns all items queried for that can be found.
     * @return a list of all items that can be found or an empty list
     */
    suspend fun readByIds(itemIds: List<Long>) : List<DbItem> {
        if (itemIds.isEmpty())
            return emptyList()
        val itemsFoundForIds = localDataSource.readByIds(itemIds)
        return itemsFoundForIds
    }

    /**
     * Searches for all items that contain the queried name.
     * Requires at least one character to start the query
     * @return a list of all matching items (containing the search pattern)
     */
    suspend fun readByName(itemName: String) : List<DbItem> {
        if (itemName.isEmpty())
            return emptyList()
        val itemsFoundForName = localDataSource.readByName(itemName)
        return itemsFoundForName
    }

    /**
     * Stores the new item but only
     * item does not exist
     * @throws IllegalArgumentException if the item did exist
     * @return the newly created item with the inserted id
     */
    suspend fun create(newItem: DbItem) : DbItem {
        val createdItem = localDataSource.create(newItem)
        return createdItem
    }

    /**
     * Updates an item if it already exists.
     * @throws IllegalArgumentException if the item did not exist
     */
    suspend fun update(item: DbItem) {
        localDataSource.update(item)
    }

    /**
     * Tries to delete the item if it exists.
     * If the item does not exist, nothing happens
     */
    suspend fun delete(item: DbItem) {
        delete(item.id)
    }

    suspend fun delete(itemId: Long) {
        localDataSource.delete(itemId)
    }

}