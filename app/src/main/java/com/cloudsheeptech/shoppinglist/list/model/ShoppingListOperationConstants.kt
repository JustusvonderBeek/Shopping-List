package com.cloudsheeptech.shoppinglist.list.model

enum class ShoppingListOperationConstants(
    val op: String,
) {
    CREATE("CRT"),
    ADD_ITEM("ADD"),
    REMOVE_ITEM("RMV"),
    CHANGE_QUANTITY_ITEM("QTY"),
    TOGGLE_ITEM("TGL"),
    RENAME_LIST("RNM"),
    DELETE_LIST("DEL"),
}
