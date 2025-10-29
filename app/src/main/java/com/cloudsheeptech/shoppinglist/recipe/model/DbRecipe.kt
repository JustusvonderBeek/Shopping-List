package com.cloudsheeptech.shoppinglist.recipe.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.time.OffsetDateTime

@Entity(tableName = "recipes", primaryKeys = ["id", "createdBy"])
data class DbRecipe(
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
