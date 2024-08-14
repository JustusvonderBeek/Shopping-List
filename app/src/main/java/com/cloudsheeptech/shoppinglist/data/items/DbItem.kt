package com.cloudsheeptech.shoppinglist.data.items

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class DbItem(
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    var name: String,
    // Can be null or "" in case no icon is specified
    var icon: String
) : Comparable<DbItem> {
    override fun compareTo(other: DbItem): Int {
        if (other.id == this.id)
            return 0
        if (other.id > this.id)
            return -1
        return 1
    }
}