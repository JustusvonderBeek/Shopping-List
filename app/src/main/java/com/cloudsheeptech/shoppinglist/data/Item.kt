package com.cloudsheeptech.shoppinglist.data

import kotlinx.serialization.Serializable

@Serializable
data class Item(
    var ID: Long,
    var Name: String,
    var ImagePath: String
) : Comparable<Item> {
    override fun compareTo(other: Item): Int {
        if (other.ID == this.ID)
            return 0
        if (other.ID > this.ID)
            return -1
        return 1
    }
}
