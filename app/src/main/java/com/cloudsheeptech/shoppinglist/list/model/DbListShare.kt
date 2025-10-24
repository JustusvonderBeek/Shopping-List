package com.cloudsheeptech.shoppinglist.list.model

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "shared_table", primaryKeys = ["ListId", "CreatedBy", "SharedWith"])
data class DbListShare(
    var ListId: Long,
    var CreatedBy: Long,
    var SharedWith: Long,
)
