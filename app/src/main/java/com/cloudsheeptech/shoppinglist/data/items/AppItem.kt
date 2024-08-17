package com.cloudsheeptech.shoppinglist.data.items

data class AppItem(
    var id: Long, // We need this ID locally, because of actions like toggle the checkbox etc.
    var name: String,
    var icon: String,
    var quantity : Long,
    var checked : Boolean,
    var addedBy : Long
)