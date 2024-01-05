package com.cloudsheeptech.shoppinglist.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "user_table")
data class User(
    @PrimaryKey(autoGenerate = false)
    var ID : Long = 0L,
    var Username : String = "",
    var Password : String = ""
)
