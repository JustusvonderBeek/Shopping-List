package com.cloudsheeptech.shoppinglist.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cloudsheeptech.shoppinglist.fragments.list.ShoppinglistViewModel

@Entity(tableName = "ui_preferences")
data class UIPreference(
    @PrimaryKey(autoGenerate = true)
    var ID : Long,
    var ListId : Long,
    var Ordering : ShoppinglistViewModel.ORDERING,
)
