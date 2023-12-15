package com.cloudsheeptech.shoppinglist.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true)
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