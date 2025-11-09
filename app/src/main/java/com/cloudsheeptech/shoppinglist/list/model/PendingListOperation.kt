package com.cloudsheeptech.shoppinglist.list.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_list_operation")
data class PendingListOperation(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val opType: String,
    val serializedOp: String,
)
