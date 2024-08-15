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

    override fun equals(other: Any?): Boolean {
        if (other is DbItem) {
            // We don't care about the ID, we only care if the same sequence
            // of characters is found in both
            return this.name.trim().lowercase() == other.name.trim().lowercase()
        }
        return false
    }

    override fun compareTo(other: DbItem): Int {
        if (other.id == this.id)
            return 0
        if (other.id > this.id)
            return -1
        return 1
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}