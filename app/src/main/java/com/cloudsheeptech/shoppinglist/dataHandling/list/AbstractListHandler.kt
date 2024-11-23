package com.cloudsheeptech.shoppinglist.dataHandling.list

abstract class AbstractListHandler {

    abstract fun mergeTwoLists()
    abstract fun addItemToList(): Unit
    abstract fun addItemsToList(): Unit
    abstract fun removeItemFromList(): Unit

}