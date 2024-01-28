package com.cloudsheeptech.shoppinglist.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "list_table")
data class ShoppingList(
    @PrimaryKey(autoGenerate = true)
    var ID : Long,
    var Name : String,
    var CreatedBy : ListCreator,
    var LastEdited : String
)