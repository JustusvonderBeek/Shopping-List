package com.cloudsheeptech.shoppinglist.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "list_table")
data class ShoppingList(
    @PrimaryKey(autoGenerate = true)
    var ID : Long,
    var Title : String,
    var Description : String,
    var Image : String,
    var Creator : User
)
