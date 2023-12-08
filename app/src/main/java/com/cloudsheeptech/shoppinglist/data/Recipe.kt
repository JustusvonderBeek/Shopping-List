package com.cloudsheeptech.shoppinglist.data

data class Recipe(
    var ID : Long,
    var Name : String,
    var Description : String,
    var Items : List<Item>,
    var Quantities : List<Float>,
)
