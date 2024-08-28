package com.cloudsheeptech.shoppinglist.data.receipt

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.OffsetDateTime

@Entity(tableName = "receipts")
data class DbReceipt(
    @PrimaryKey(autoGenerate = true)
    var id : Long,
    var name: String,
    var createdBy: Long,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    var createdAt: OffsetDateTime,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    var lastUpdated: OffsetDateTime,
)
