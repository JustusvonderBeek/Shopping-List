package com.cloudsheeptech.shoppinglist.data

import androidx.room.PrimaryKey

data class ItemWithQuantity(
    var ID: Long,
    var Name: String,
    var ImagePath: String,
    var Quantity : Long,
)
