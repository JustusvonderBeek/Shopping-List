package com.cloudsheeptech.shoppinglist.list.model

sealed class ShoppingListOperation {
    data class Create(
        val title: String,
        val creator: ListCreator,
    ) : ShoppingListOperation()

    data class AddItem(
        val listPk: ShoppingListPK,
        val item: AppItem,
    ) : ShoppingListOperation()

    data class AddItemByName(
        val listPk: ShoppingListPK,
        val itemName: String,
    ) : ShoppingListOperation()

    data class RemoveItemByName(
        val listPk: ShoppingListPK,
        val itemName: String,
    ) : ShoppingListOperation()

    data class ChangeQuantityOfItem(
        val listPk: ShoppingListPK,
        val itemName: String,
        val quantity: Long,
        val quantityType: QuantityType?,
    ) : ShoppingListOperation()

    data class SetItemCheckedStatus(
        val listPk: ShoppingListPK,
        val itemName: String,
        val status: ItemToggleStatus = ItemToggleStatus.TOGGLE,
    ) : ShoppingListOperation()

    data class RenameList(
        val listPk: ShoppingListPK,
        val newName: String,
    ) : ShoppingListOperation()

    data class Delete(
        val listPk: ShoppingListPK,
    ) : ShoppingListOperation()
}
