package com.cloudsheeptech.shoppinglist.list.model

sealed class ShoppingListOperation {
    data class Create(
        val title: String,
        val creator: ListCreator,
        val items: List<AppItem>,
    ) : ShoppingListOperation()

    data class AddItem(
        val listPk: ShoppingListPK,
        val item: AppItem,
    ) : ShoppingListOperation()

    data class AddItemById(
        val listPk: ShoppingListPK,
        val itemId: Long,
    ) : ShoppingListOperation()

    data class RemoveItemByName(
        val listPk: ShoppingListPK,
        val itemName: String,
    ) : ShoppingListOperation()

    data class RemoveItemById(
        val listPk: ShoppingListPK,
        val itemId: Long,
    ) : ShoppingListOperation()

    data class ChangeQuantityOfItem(
        val listPk: ShoppingListPK,
        val itemId: Long,
        val quantity: Long,
        val quantityType: QuantityType?,
    ) : ShoppingListOperation()

    data class SetItemCheckedStatus(
        val listPK: ShoppingListPK,
        val itemId: Long,
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
