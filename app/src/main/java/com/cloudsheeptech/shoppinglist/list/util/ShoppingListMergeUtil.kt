package com.cloudsheeptech.shoppinglist.list.util

import android.util.Log
import com.cloudsheeptech.shoppinglist.list.model.ShoppingList
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListOperation
import io.ktor.util.toLowerCasePreservingASCIIRules

class ShoppingListMergeUtil {
    companion object {
        @Throws(IllegalArgumentException::class)
        fun getListDelta(
            existingList: ShoppingList,
            newList: ShoppingList,
        ): Map<ShoppingListOperation, List<ApiItem>> {
            val addedItems = mutableListOf<ApiItem>()
            val removeItems = mutableListOf<ApiItem>()
            val changedQuantityItems = mutableListOf<ApiItem>()
            val operationsPerformed = HashMap<ShoppingListOperation, List<ApiItem>>()

            if (existingList.listId != newList.listId) {
                throw IllegalArgumentException(
                    "list ${existingList.title}: ${existingList.listId} is different from ${newList.title}: ${newList.listId}",
                )
            }
            if (existingList.createdBy.onlineId != newList.createdBy.onlineId) {
                Log.w(
                    "ShoppingListMergeHelper",
                    "existing list ${existingList.title}: ${existingList.listId} has different creator ${existingList.createdBy.onlineId} than new list ${newList.createdBy.onlineId}",
                )
                operationsPerformed.put(ShoppingListOperation.CHANGE_CREATOR, emptyList())
            }

            if (existingList.title != newList.title) {
                operationsPerformed.put(ShoppingListOperation.RENAME_LIST, emptyList())
            }

            val newItems = HashMap<String, ApiItem>()
            for (newItem in newList.items) {
                newItems.put(newItem.name.toLowerCasePreservingASCIIRules(), newItem)
            }
            for (item in existingList.items) {
                if (!newItems.contains(item.name.toLowerCasePreservingASCIIRules())) {
                    removeItems.add(item)
                } else {
                    val changedItem = newItems.remove(item.name.toLowerCasePreservingASCIIRules())
                    if (changedItem != null && item.quantity != changedItem.quantity) {
                        changedQuantityItems.add(changedItem)
                    }
                }
            }

            addedItems.addAll(newItems.values)
            if (addedItems.isNotEmpty()) {
                operationsPerformed.put(ShoppingListOperation.ADD_ITEM, addedItems)
            }
            if (removeItems.isNotEmpty()) {
                operationsPerformed.put(ShoppingListOperation.REMOVE_ITEM, removeItems)
            }
            if (changedQuantityItems.isNotEmpty()) {
                operationsPerformed.put(
                    ShoppingListOperation.CHANGE_QUANTITY,
                    changedQuantityItems,
                )
            }

            return operationsPerformed
        }
    }
}
