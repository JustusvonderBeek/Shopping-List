package com.cloudsheeptech.shoppinglist.data

import kotlinx.serialization.Serializable

@Serializable
data class ShopListWire(
    var ListID : Long,
    var Name : String,
    var CreatedBy : Long,
    var LastEdited : String,
    var Items : List<Item>
)
