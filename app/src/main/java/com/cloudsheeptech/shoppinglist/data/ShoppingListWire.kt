package com.cloudsheeptech.shoppinglist.data

import kotlinx.serialization.Serializable

@Serializable
data class ShoppingListWire(
    var ListId : Long,
    var Name : String,
    var CreatedBy : Long,
    var LastEdited : String,
    var Items : MutableList<ItemWire>
)
