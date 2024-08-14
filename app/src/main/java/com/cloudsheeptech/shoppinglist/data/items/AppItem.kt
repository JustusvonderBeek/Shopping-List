package com.cloudsheeptech.shoppinglist.data.items

data class AppItem(
    var id: Long,   // Do we need this ID? And what for? Otherwise, can be merged with the ApiItem
    var name: String,
    var icon: String,
    var quantity : Long,
    var checked : Boolean,
    var addedBy : Long
)