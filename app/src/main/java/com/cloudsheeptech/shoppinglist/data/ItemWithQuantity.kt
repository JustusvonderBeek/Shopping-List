package com.cloudsheeptech.shoppinglist.data

data class ItemWithQuantity(
    var ID: Long,
    var Name: String,
    var ImagePath: String,
    var Quantity : Long,
    var Checked : Boolean,
)
