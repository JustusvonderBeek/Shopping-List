package com.cloudsheeptech.shoppinglist.data.list

import com.cloudsheeptech.shoppinglist.data.items.AppItem
import com.cloudsheeptech.shoppinglist.data.onlineUser.ListCreator

sealed class ShoppingListOperation {
    data class Create(
        val name: String,
        val creator: ListCreator,
        val items: List<AppItem>,
    ) : ShoppingListOperation()

    data class Add(
        val item: AppItem,
    ) : ShoppingListOperation()

    data class RemoveByName(
        val itemName: String,
    ) : ShoppingListOperation()

    data class RemoveById(
        val id: Long,
    ) : ShoppingListOperation()

    data class Rename(
        val newName: String,
    ) : ShoppingListOperation()

    data class ChangeQuantity(
        val id: Long,
        val quantity: Long,
    ) : ShoppingListOperation()

    data class Delete(
        val listId: Long,
    ) : ShoppingListOperation()
}
