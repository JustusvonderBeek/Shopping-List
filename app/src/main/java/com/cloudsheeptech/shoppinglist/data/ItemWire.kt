package com.cloudsheeptech.shoppinglist.data

import kotlinx.serialization.Serializable

@Serializable
data class ItemWire(
    var Name: String,
    var Icon: String,
    var Quantity : Long,
    var Checked : Boolean,
    var AddedBy : Long,
)
