package com.cloudsheeptech.shoppinglist.data.list

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.cloudsheeptech.shoppinglist.data.core.EntityIdentifier
import java.time.OffsetDateTime

@Entity(tableName = "list_table", primaryKeys = ["listId", "createdBy"])
data class DbShoppingList(
    var listId: Long,
    var title: String,
    // Flatten list creator to allow direct ID access
    var createdBy: Long,
    // FIXME: This column is deprecated and should be removed / updated to createdAt
    var createdByName: String,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    var lastUpdated: OffsetDateTime,
    var version: Long, // Replaces the lastUpdated timestamp, allows for easier version comparison
) : EntityIdentifier<Pair<Long, Long>> {
    override fun getId(): Pair<Long, Long> = Pair(listId, createdBy)
}
