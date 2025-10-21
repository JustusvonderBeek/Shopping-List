package com.cloudsheeptech.shoppinglist.data.list

import androidx.room.ColumnInfo
import androidx.room.Entity
import kotlinx.serialization.Contextual
import java.time.OffsetDateTime

@Entity(tableName = "list_table", primaryKeys = ["listId", "createdBy"])
data class DbShoppingList(
    var listId: Long,
    var createdBy: Long,
    var title: String,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    @Contextual
    var lastSynchronized: OffsetDateTime,
)
