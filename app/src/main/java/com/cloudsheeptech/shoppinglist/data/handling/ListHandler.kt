package com.cloudsheeptech.shoppinglist.data.handling

import com.cloudsheeptech.shoppinglist.data.ShoppingList
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

abstract class ListHandler(val database: ShoppingListDatabase) {

    // Common for both list handlers to execute background tasks
    protected val job = Job()
    protected val backgroundCoroutine = CoroutineScope(Dispatchers.Main + job)

    abstract suspend fun storeShoppingList(list: ShoppingList) : Long
    abstract suspend fun retrieveShoppingList(listId: Long, createdBy: Long): ShoppingList?
    abstract suspend fun updateLastEditedNow(listId: Long, createdBy: Long): ShoppingList?
    abstract suspend fun deleteShoppingList(listId: Long, createdBy: Long)

}