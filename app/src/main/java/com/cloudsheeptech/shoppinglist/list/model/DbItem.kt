package com.cloudsheeptech.shoppinglist.list.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class DbItem(
//    @PrimaryKey(autoGenerate = true)
//    var id: Long,
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(collate = ColumnInfo.NOCASE, index = true)
    var name: String,
    // Can be null or "" in case no icon is specified
    @ColumnInfo(defaultValue = "")
    var icon: String,
) : Comparable<DbItem> {
    override fun equals(other: Any?): Boolean {
        if (other is DbItem) {
            // We don't care about the ID, we only care if the same sequence
            // of characters is found in both
            return this.name.trim().lowercase() == other.name.trim().lowercase()
        }
        return false
    }

    override fun compareTo(other: DbItem): Int = other.name.compareTo(this.name, ignoreCase = true)

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}
