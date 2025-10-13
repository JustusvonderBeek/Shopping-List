package com.cloudsheeptech.shoppinglist.data.list

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.cloudsheeptech.shoppinglist.data.core.EntityIdentifier
import java.time.OffsetDateTime

@Entity(tableName = "list_table", primaryKeys = ["listId", "createdBy"])
data class DbShoppingList(
    var listId: Long,
    var createdBy: Long,
    var title: String,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    var synchronized: OffsetDateTime,
) : EntityIdentifier<Pair<Long, Long>> {
    override fun getId(): Pair<Long, Long> = Pair(listId, createdBy)
}
