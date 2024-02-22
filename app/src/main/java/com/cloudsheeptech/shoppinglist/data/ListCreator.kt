package com.cloudsheeptech.shoppinglist.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "online_user")
data class ListCreator(
    @PrimaryKey(autoGenerate = false)
    var ID : Long,
    var Name : String,
)
