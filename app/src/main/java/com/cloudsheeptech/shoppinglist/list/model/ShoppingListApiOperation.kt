package com.cloudsheeptech.shoppinglist.list.model

import kotlinx.serialization.Serializable

@Serializable
sealed class ShoppingListApiOperation {
    abstract val op: String

    @Serializable
    data class Create(
        val title: String,
        val creator: ListCreator,
        override val op: String = ShoppingListOperationConstants.CREATE.op,
    ) : ShoppingListApiOperation()

    @Serializable
    data class RenameList(
        val listPk: ShoppingListPK,
        val newTitle: String,
        override val op: String = ShoppingListOperationConstants.RENAME_LIST.op,
    ) : ShoppingListApiOperation()

    @Serializable
    data class DeleteList(
        val listPk: ShoppingListPK,
        override val op: String = ShoppingListOperationConstants.DELETE_LIST.op,
    ) : ShoppingListApiOperation()

    @Serializable
    data class AddItem(
        val listPk: ShoppingListPK,
        val item: AppItem,
        override val op: String = ShoppingListOperationConstants.ADD_ITEM.op,
    ) : ShoppingListApiOperation()

    @Serializable
    data class RemoveItem(
        val listPk: ShoppingListPK,
        val itemName: String,
        override val op: String = ShoppingListOperationConstants.REMOVE_ITEM.op,
    ) : ShoppingListApiOperation()

    @Serializable
    data class ChangeQuantityItem(
        val listPk: ShoppingListPK,
        val itemName: String,
        override val op: String = ShoppingListOperationConstants.CHANGE_QUANTITY_ITEM.op,
    ) : ShoppingListApiOperation()

    @Serializable
    data class ToggleItem(
        val listPk: ShoppingListPK,
        val itemName: String,
        override val op: String = ShoppingListOperationConstants.TOGGLE_ITEM.op,
    ) : ShoppingListApiOperation()
}
