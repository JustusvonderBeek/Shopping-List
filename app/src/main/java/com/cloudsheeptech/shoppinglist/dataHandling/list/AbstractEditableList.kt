package com.cloudsheeptech.shoppinglist.dataHandling.list

data class AbstractEditableList(
    var version: Long,
    var title: String,
    var items: List<AbstractEditableItems>,
)
