package com.cloudsheeptech.shoppinglist.data.list

import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ListMapping
import com.cloudsheeptech.shoppinglist.data.items.ApiItem
import com.cloudsheeptech.shoppinglist.data.items.AppItem
import com.cloudsheeptech.shoppinglist.data.items.DbItem
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
        ): ListMapping {
            val listMapping =
                ListMapping(
                    ID = 0L, // Auto generated
                    ItemID = itemId,
                    ListID = listId,
                    CreatedBy = createdBy,
                    Quantity = this.quantity,
                    Checked = this.checked,
                    AddedBy = this.addedBy,
                )
            return listMapping
        }

        fun ApiShoppingList.toDbList(): Pair<DbShoppingList, List<DbItem>> {
            val dbList =
                DbShoppingList(
                    listId = this.listId,
                    title = this.title,
                    createdBy = this.createdBy.onlineId,
                    createdByName = this.createdBy.username,
                    lastUpdated = this.lastUpdated,
                    version = this.version,
                )
            val dbItems = this.items.map { item -> item.toDbItem() }
            return Pair(dbList, dbItems)
        }

        // Ignore for now
        fun DbShoppingList.toApiList(listCreator: ListCreator): ApiShoppingList {
            val apiList =
                ApiShoppingList(
                    listId = this.listId,
                    title = this.title,
                    createdBy = listCreator,
                    createdAt = this.lastUpdated,
                    lastUpdated = this.lastUpdated,
                    items = mutableListOf(),
                    version = this.version,
                )
            return apiList
        }

        fun ListMapping.toApiItem(): ApiItem {
            val apiItem =
                ApiItem(
                    name = "",
                    icon = "",
                    quantity = this.Quantity,
                    checked = this.Checked,
                    addedBy = this.AddedBy,
                )
            return apiItem
        }
    }
}
