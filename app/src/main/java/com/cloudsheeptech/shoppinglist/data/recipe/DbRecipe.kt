package com.cloudsheeptech.shoppinglist.data.recipe

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.OffsetDateTime

@Entity(tableName = "recipes")
data class DbRecipe(
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    var name: String,
    var createdBy: Long,
    var createdByName: String,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    var createdAt: OffsetDateTime,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    var lastUpdated: OffsetDateTime,
    var version: Int,
    var defaultPortion: Int,
)
