package com.cloudsheeptech.shoppinglist.data.uiPreference

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ui_preferences")
data class UIPreference(
    @PrimaryKey(autoGenerate = true)
    var ID: Long,
    var ListId: Long,
    var Ordering: Ordering,
)
