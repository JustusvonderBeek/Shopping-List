package com.cloudsheeptech.shoppinglist.handling

import android.app.Application
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudsheeptech.shoppinglist.data.ShoppingList
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.handling.DatabaseListHandler
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.time.OffsetDateTime

@RunWith(AndroidJUnit4::class)
class DatabaseListHandlerTest {

    private fun createDefaultList() : ShoppingList {
        val list = ShoppingList(
            ID = 0,
            Name = "Default List",
            CreatedByID = 1234,
            CreatedByName = "Default User",
            LastEdited = OffsetDateTime.now(),
        )
        return list
    }

//    private fun compareLists() {
//        val timeVal = OffsetDateTime.now()
//        val list1 = ShoppingList(
//            ID = 0,
//            Name = "Default List",
//            CreatedBy = 1234,
//            CreatedByName = "Default User",
//            LastEdited = timeVal,
//        )
//        val list2 = ShoppingList(
//            ID = 0,
//            Name = "Default List",
//            CreatedBy = 1234,
//            CreatedByName = "Default User",
//            LastEdited = timeVal,
//        )
//        Log.d("DatabaseListHandlerTest", "Lists equal: ${list1 == list2}")
//    }

    private fun checkDbContains(shoppingLists: List<ShoppingList>) : Boolean {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val slDao = database.shoppingListDao()
        val compareLists = slDao.getShoppingLists()
        if (compareLists.size != shoppingLists.size)
            return false
        for (list in shoppingLists) {
            if (!compareLists.contains(list))
                return false
        }
        return true
    }

    @Test
    fun testListInsert() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val dbHandler = DatabaseListHandler(database)

        val list = createDefaultList()
        val insertId = dbHandler.storeShoppingList(list)
//        Log.d("DatabaseListHandlerTest", "List: $list")

        assert(insertId == 1L)
        assert(list.ID == 0L)
        val list2 = list.copy()
        list.ID = insertId
        assert(checkDbContains(listOf(list)))

//        Log.d("DatabaseListHandlerTest", "Value: $insertId")
        // Checks that the list is not modified inside the function!

        val insertId2 = dbHandler.storeShoppingList(list2)
        assert(insertId2 == 2L)
        assert(list2.ID == 0L)
        list2.ID = insertId
        assert(checkDbContains(listOf(list, list2)))
    }

    @Test
    fun testListRetrieve() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val dbHandler = DatabaseListHandler(database)

        val list = createDefaultList()
        val insertId = dbHandler.storeShoppingList(list)

        val containedLists = mutableListOf(list)
        assert(insertId == 1L)
        list.ID = insertId
        assert(checkDbContains(containedLists))

        val list2 = createDefaultList()
        list2.Name = "Default List 2"
        val insertedId2 = dbHandler.storeShoppingList(list2)
        list2.ID = insertedId2
        containedLists.add(list2)

        assert(insertedId2 == 2L)
        assert(checkDbContains(containedLists))

        val retrievedList = dbHandler.retrieveShoppingList(insertId, list.CreatedByID)
        Assert.assertNotNull(retrievedList)
        // Fix the insertion ID
//        Log.e("DatabaseListHandlerTest", "List: $list, Inserted: $retrievedList")
        assert(list == retrievedList)
        val retrievedList2 = dbHandler.retrieveShoppingList(insertedId2, list2.CreatedByID)
//        Log.e("DatabaseListHandlerTest", "List: $list2, Inserted: $retrievedList2")
        Assert.assertNotNull(retrievedList2)
        assert(list2 == retrievedList2)
    }

    @Test
    fun testListUpdate() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val dbHandler = DatabaseListHandler(database)

        val list = createDefaultList()
        val insertId = dbHandler.storeShoppingList(list)
        list.ID = insertId

        val containedLists = mutableListOf(list)
        assert(insertId == 1L)
        assert(checkDbContains(containedLists))

        list.Name = "New list name"
        // Reference: Yes!
        Log.i("DatabaseListHandlerTest", "Updated list: $containedLists")

        val insertId2 = dbHandler.storeShoppingList(list)
        assert(insertId2 == list.ID)
        assert(checkDbContains(containedLists))
    }

    @Test
    fun testUpdateLastEdited() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val dbHandler = DatabaseListHandler(database)

        val list = createDefaultList()
        val insertId = dbHandler.storeShoppingList(list)
        list.ID = insertId

        val containedLists = mutableListOf(list)
        assert(insertId == 1L)
        assert(checkDbContains(containedLists))

        val updatedList = dbHandler.updateLastEditedNow(insertId, list.CreatedByID)
        Assert.assertNotNull(updatedList)
        assert(updatedList!!.ID == insertId)
        containedLists.clear()
        containedLists.add(updatedList)
        assert(checkDbContains(containedLists))

        // Test if list cannot be found
        val updatedList2 = dbHandler.updateLastEditedNow(insertId, list.CreatedByID + 10)
        Assert.assertNull(updatedList2)
    }

    @Test
    fun testDeleteList() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val dbHandler = DatabaseListHandler(database)

        val list = createDefaultList()
        val insertId = dbHandler.storeShoppingList(list)
        list.ID = insertId

        val containedLists = mutableListOf(list)
        assert(insertId == 1L)
        assert(checkDbContains(containedLists))

        val updatedList = dbHandler.deleteShoppingList(insertId, list.CreatedByID)

        assert(checkDbContains(emptyList()))
    }

}