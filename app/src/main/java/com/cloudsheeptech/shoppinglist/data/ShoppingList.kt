package com.cloudsheeptech.shoppinglist.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "list_table")
data class ShoppingList(
    @PrimaryKey(autoGenerate = false)
    var ID : Long,
    var Name : String,
    var CreatedBy : User,
    var LastEdited : String
)

@Serializable
data class ShoppingListWire(
    var ID : Long,
    var Name : String,
    var CreatedBy : Long,
    var LastEdited : String
)