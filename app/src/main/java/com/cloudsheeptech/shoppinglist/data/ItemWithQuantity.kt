package com.cloudsheeptech.shoppinglist.data

data class ItemWithQuantity(
    var ID: Long,
    var Name: String,
    var IconPath: String,
    var Quantity : Long,
    var Checked : Boolean,
    var AddedBy : Long
)
