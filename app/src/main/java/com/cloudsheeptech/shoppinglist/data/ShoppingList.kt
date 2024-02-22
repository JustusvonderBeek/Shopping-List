package com.cloudsheeptech.shoppinglist.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Entity(tableName = "list_table", primaryKeys = ["ID", "CreatedBy"])
data class ShoppingList(
//    @PrimaryKey(autoGenerate = true)
    var ID : Long,
    var Name : String,
    // Flatten list creator to allow direct ID access
    var CreatedBy : Long,
    var CreatedByName : String,
    var LastEdited : OffsetDateTime
)