package com.cloudsheeptech.shoppinglist.data.items

import kotlinx.serialization.Serializable

/**
 * This class is meant to signal the item to the other remote endpoint
 * in a compact and combined format so that no additional information
 * is required besides this class.
 */
@Serializable
data class ApiItem(
    var name: String,
    var icon: String,
    var quantity : Long,
    var checked : Boolean,
    var addedBy : Long,
)