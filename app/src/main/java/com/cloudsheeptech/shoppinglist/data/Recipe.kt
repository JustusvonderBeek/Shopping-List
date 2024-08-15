package com.cloudsheeptech.shoppinglist.data

import com.cloudsheeptech.shoppinglist.data.items.DbItem

data class Recipe(
    var ID : Long,
    var Name : String,
    var Description : String,
    var dbItems : List<DbItem>,
    var Quantities : List<Float>,
)
