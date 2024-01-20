package com.cloudsheeptech.shoppinglist.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "list_table")
data class ShoppingList(
    @PrimaryKey(autoGenerate = true)
    var ID : Long,
    var Name : String,
    var CreatedBy : User,
    var LastEdited : String
)

@Serializable
data class ShoppingListWire(
    var ListId : Long,
    var Name : String,
    var CreatedBy : Long,
    var LastEdited : String,
    var Items : MutableList<ItemWire>
)