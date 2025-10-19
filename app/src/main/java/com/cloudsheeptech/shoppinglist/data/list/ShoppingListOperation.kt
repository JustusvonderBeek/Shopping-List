package com.cloudsheeptech.shoppinglist.data.list

import com.cloudsheeptech.shoppinglist.data.items.AppItem
import com.cloudsheeptech.shoppinglist.data.items.QuantityType
import com.cloudsheeptech.shoppinglist.data.onlineUser.ListCreator

sealed class ShoppingListOperation {
    data class Create(
        val title: String,
        val creator: ListCreator,
        val items: List<AppItem>,
    ) : ShoppingListOperation()

    data class AddItem(
        val item: AppItem,
        val listPk: ShoppingListPK,
    ) : ShoppingListOperation()

    data class AddItemById(
        val itemId: Long,
        val listPk: ShoppingListPK,
    ) : ShoppingListOperation()

    data class RemoveItemByName(
        val itemName: String,
        val listPk: ShoppingListPK,
    ) : ShoppingListOperation()

    data class RemoveItemById(
        val itemId: Long,
        val listPk: ShoppingListPK,
    ) : ShoppingListOperation()

    data class ChangeQuantityOfItem(
        val itemId: Long,
        val quantity: Long,
        val quantityType: QuantityType?,
        val listPk: ShoppingListPK,
    ) : ShoppingListOperation()

    data class SetItemCheckedStatus(
        val itemId: Long,
        val status: ItemToggleStatus = ItemToggleStatus.TOGGLE,
        val listPK: ShoppingListPK,
    ) : ShoppingListOperation()

    data class RenameList(
        val newName: String,
        val listPk: ShoppingListPK,
    ) : ShoppingListOperation()

    data class Delete(
        val listPk: ShoppingListPK,
    ) : ShoppingListOperation()
}
