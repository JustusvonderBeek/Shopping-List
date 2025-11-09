package com.cloudsheeptech.shoppinglist.list.model

import kotlinx.serialization.Serializable

sealed class ShoppingListApiOperation {
    abstract val listPk: ShoppingListPK
    abstract val op: String

    @Serializable
    data class Create(
        override val listPk: ShoppingListPK,
        val title: String,
        val creator: ListCreator,
        override val op: String = ShoppingListOperationConstants.CREATE.op,
    ) : ShoppingListApiOperation()

    @Serializable
    data class RenameList(
        override val listPk: ShoppingListPK,
        val newTitle: String,
        override val op: String = ShoppingListOperationConstants.RENAME_LIST.op,
    ) : ShoppingListApiOperation()

    @Serializable
    data class DeleteList(
        override val listPk: ShoppingListPK,
        override val op: String = ShoppingListOperationConstants.DELETE_LIST.op,
    ) : ShoppingListApiOperation()

    @Serializable
    data class AddItem(
        override val listPk: ShoppingListPK,
        val item: AppItem,
        override val op: String = ShoppingListOperationConstants.ADD_ITEM.op,
    ) : ShoppingListApiOperation()

    @Serializable
    data class RemoveItem(
        override val listPk: ShoppingListPK,
        val itemName: String,
        override val op: String = ShoppingListOperationConstants.REMOVE_ITEM.op,
    ) : ShoppingListApiOperation()

    @Serializable
    data class ChangeQuantityItem(
        override val listPk: ShoppingListPK,
        val itemName: String,
        override val op: String = ShoppingListOperationConstants.CHANGE_QUANTITY_ITEM.op,
    ) : ShoppingListApiOperation()

    @Serializable
    data class ToggleItem(
        override val listPk: ShoppingListPK,
        val itemName: String,
        override val op: String = ShoppingListOperationConstants.TOGGLE_ITEM.op,
    ) : ShoppingListApiOperation()
}
