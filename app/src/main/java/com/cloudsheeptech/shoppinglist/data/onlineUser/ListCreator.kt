package com.cloudsheeptech.shoppinglist.data.onlineUser

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "online_user")
data class ListCreator(
    @PrimaryKey(autoGenerate = false)
    var onlineId : Long,
    var username : String,
)
