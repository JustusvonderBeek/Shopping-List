package com.cloudsheeptech.shoppinglist.data.sharing

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "shared_table", primaryKeys = ["ListId", "CreatedBy", "SharedWith"])
data class ListShareDatabase(
    var ListId: Long,
    var CreatedBy: Long,
    var SharedWith: Long,
)
