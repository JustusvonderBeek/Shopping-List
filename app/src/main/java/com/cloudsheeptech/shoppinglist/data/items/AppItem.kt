package com.cloudsheeptech.shoppinglist.data.items

data class AppItem(
    // We need this ID locally, because of actions like toggle the checkbox etc.
    var id: Long, // But do we really? Shouldn't this be possible with just the name?
    var name: String,
    var icon: String,
    var quantity: Long,
    var checked: Boolean,
    var addedBy: Long,
)
