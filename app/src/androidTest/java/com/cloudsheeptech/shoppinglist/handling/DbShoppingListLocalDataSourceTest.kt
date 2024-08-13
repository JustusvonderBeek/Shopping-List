package com.cloudsheeptech.shoppinglist.handling

import android.app.Application
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudsheeptech.shoppinglist.data.DbShoppingList
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListLocalDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.time.OffsetDateTime

@RunWith(AndroidJUnit4::class)
class DbShoppingListLocalDataSourceTest {

    private fun createDefaultList() : DbShoppingList {
        val list = DbShoppingList(
            listId = 0,
            title = "Default List",
            createdBy = 1234,
            createdByName = "Default User",
            lastUpdated = OffsetDateTime.now(),
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

    private fun checkDbContains(dbShoppingLists: List<DbShoppingList>) : Boolean {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val slDao = database.shoppingListDao()
        val compareLists = slDao.getShoppingLists()
        if (compareLists.size != dbShoppingLists.size)
            return false
        for (list in dbShoppingLists) {
            if (!compareLists.contains(list))
                return false
        }
        return true
    }

    @Test
    fun testListInsert() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val dbHandler = ShoppingListLocalDataSource(database)

        val list = createDefaultList()
        val insertId = dbHandler.create(list)
//        Log.d("DatabaseListHandlerTest", "List: $list")

        assert(insertId == 1L)
        assert(list.listId == 0L)
        val list2 = list.copy()
        list.listId = insertId
        assert(checkDbContains(listOf(list)))

//        Log.d("DatabaseListHandlerTest", "Value: $insertId")
        // Checks that the list is not modified inside the function!

        val insertId2 = dbHandler.create(list2)
        assert(insertId2 == 2L)
        assert(list2.listId == 0L)
        list2.listId = insertId
        assert(checkDbContains(listOf(list, list2)))
    }

    @Test
    fun testListRetrieve() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val dbHandler = ShoppingListLocalDataSource(database)

        val list = createDefaultList()
        val insertId = dbHandler.create(list)

        val containedLists = mutableListOf(list)
        assert(insertId == 1L)
        list.listId = insertId
        assert(checkDbContains(containedLists))

        val list2 = createDefaultList()
        list2.title = "Default List 2"
        val insertedId2 = dbHandler.create(list2)
        list2.listId = insertedId2
        containedLists.add(list2)

        assert(insertedId2 == 2L)
        assert(checkDbContains(containedLists))

        val retrievedList = dbHandler.read(insertId, list.createdBy)
        Assert.assertNotNull(retrievedList)
        // Fix the insertion ID
//        Log.e("DatabaseListHandlerTest", "List: $list, Inserted: $retrievedList")
        assert(list == retrievedList)
        val retrievedList2 = dbHandler.read(insertedId2, list2.createdBy)
//        Log.e("DatabaseListHandlerTest", "List: $list2, Inserted: $retrievedList2")
        Assert.assertNotNull(retrievedList2)
        assert(list2 == retrievedList2)
    }

    @Test
    fun testListUpdate() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val dbHandler = ShoppingListLocalDataSource(database)

        val list = createDefaultList()
        val insertId = dbHandler.create(list)
        list.listId = insertId

        val containedLists = mutableListOf(list)
        assert(insertId == 1L)
        assert(checkDbContains(containedLists))

        list.title = "New list name"
        // Reference: Yes!
        Log.i("DatabaseListHandlerTest", "Updated list: $containedLists")

        val insertId2 = dbHandler.create(list)
        assert(insertId2 == list.listId)
        assert(checkDbContains(containedLists))
    }

    @Test
    fun testUpdateLastEdited() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val dbHandler = ShoppingListLocalDataSource(database)

        val list = createDefaultList()
        val insertId = dbHandler.create(list)
        list.listId = insertId

        val containedLists = mutableListOf(list)
        assert(insertId == 1L)
        assert(checkDbContains(containedLists))

        val updatedList = dbHandler.update(insertId, list.createdBy)
        Assert.assertNotNull(updatedList)
        assert(updatedList!!.listId == insertId)
        containedLists.clear()
        containedLists.add(updatedList)
        assert(checkDbContains(containedLists))

        // Test if list cannot be found
        val updatedList2 = dbHandler.update(insertId, list.createdBy + 10)
        Assert.assertNull(updatedList2)
    }

    @Test
    fun testDeleteList() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val dbHandler = ShoppingListLocalDataSource(database)

        val list = createDefaultList()
        val insertId = dbHandler.create(list)
        list.listId = insertId

        val containedLists = mutableListOf(list)
        assert(insertId == 1L)
        assert(checkDbContains(containedLists))

        val updatedList = dbHandler.delete(insertId, list.createdBy)

        assert(checkDbContains(emptyList()))
    }

}