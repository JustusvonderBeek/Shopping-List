package com.cloudsheeptech.shoppinglist.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "online_user")
data class UserWire(
    @PrimaryKey(autoGenerate = false)
    var ID : Long,
    var Username : String,
)
