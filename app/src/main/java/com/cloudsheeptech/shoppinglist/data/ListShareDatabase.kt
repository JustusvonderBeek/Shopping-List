package com.cloudsheeptech.shoppinglist.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "shared_table")
data class ListShareDatabase(
    @PrimaryKey(autoGenerate = true)
    var ID : Long,
    var ListId : Long,
    var SharedWith : Long,
)
