package com.cloudsheeptech.shoppinglist.data.list

import com.cloudsheeptech.shoppinglist.data.items.ApiItem
import com.cloudsheeptech.shoppinglist.data.items.AppItem
import com.cloudsheeptech.shoppinglist.data.items.ItemToList
import com.cloudsheeptech.shoppinglist.data.onlineUser.ListCreator

class ShoppingListConversionHelper {
    companion object {
        fun AppItem.toApiItem(): ApiItem =
            ApiItem(
                name = this.name,
                icon = this.icon,
                quantity = this.quantity,
                checked = this.checked,
                addedBy = this.addedBy,
            )

        fun ApiItem.toDbItem(): DbItem {
            val dbItem =
                DbItem(
                    id = 0L, // Auto generated
                    name = this.name,
                    icon = this.icon,
                )
            return dbItem
        }

        fun DbItem.toAppItem(onlineId: Long): AppItem =
            AppItem(
                id = this.id,
                name = this.name,
                icon = this.icon,
                quantity = 1L,
                checked = false,
                addedBy = onlineId,
            )

        fun ApiItem.toListMapping(
            itemId: Long,
            listId: Long,
            createdBy: Long,
        ): ItemToList {
            val itemToList =
                ItemToList(
                    id = 0L, // Auto generated
                    itemId = itemId,
                    listId = listId,
                    createdBy = createdBy,
                    quantity = this.quantity,
                    checked = this.checked,
                    addedBy = this.addedBy,
                )
            return itemToList
        }

        fun ShoppingList.toDbList(): Pair<DbShoppingList, List<DbItem>> {
            val dbList =
                DbShoppingList(
                    listId = this.listId,
                    title = this.title,
                    createdBy = this.createdBy.onlineId,
                    createdByName = this.createdBy.username,
                    lastSynchronized = this.synchronized,
                    version = this.version,
                )
            val dbItems = this.items.map { item -> item.toDbItem() }
            return Pair(dbList, dbItems)
        }

        // Ignore for now
        fun DbShoppingList.toApiList(
            listCreator: ListCreator,
            items: MutableList<ApiItem> = mutableListOf(),
        ): ShoppingList {
            val apiList =
                ShoppingList(
                    listId = this.listId,
                    title = this.title,
                    createdBy = listCreator,
                    createdAt = this.lastSynchronized,
                    synchronized = this.lastSynchronized,
                    items = items,
                    version = this.version,
                )
            return apiList
        }

        fun ItemToList.toApiItem(): ApiItem {
            val apiItem =
                ApiItem(
                    name = "",
                    icon = "",
                    quantity = this.quantity,
                    checked = this.checked,
                    addedBy = this.addedBy,
                )
            return apiItem
        }
    }
}
