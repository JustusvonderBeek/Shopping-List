package com.cloudsheeptech.shoppinglist.data.itemToListMapping

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "item_to_list_mapping")
data class ListMapping(
    @PrimaryKey(autoGenerate = true)
    var ID : Long,
    var ItemID : Long,
    var ListID : Long,
    var CreatedBy : Long,
    var Quantity : Long,
    var Checked : Boolean,
    var AddedBy : Long
)