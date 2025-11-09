package com.cloudsheeptech.shoppinglist.list.model

import kotlinx.serialization.Serializable

@Serializable
data class ShoppingListPK(
    val listId: Long,
    val createdBy: Long,
)
