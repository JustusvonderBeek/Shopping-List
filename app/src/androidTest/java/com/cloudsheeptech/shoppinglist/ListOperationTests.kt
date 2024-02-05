package com.cloudsheeptech.shoppinglist

import android.app.Application
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudsheeptech.shoppinglist.data.ListCreator
import com.cloudsheeptech.shoppinglist.data.ShoppingList
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.handling.ShoppingListHandler
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.user.AppUser
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListOperationTests {

    private fun createList() : Boolean {
//        val listTitle = "New List"
        val application = ApplicationProvider.getApplicationContext<Application>()
        AppUser.loadUser(application)
        AppUser.ID = 12
        AppUser.Username = "Franz"

//        val factory = CreateShoppinglistViewModelFactory(application)
//        val store = ViewModelStore()
//        val viewModel = ViewModelProvider(store, factory)[CreateShoppinglistViewModel::class.java]
//        // Setting the required properties
//        withContext(Dispatchers.Main) {
//            viewModel.title.value = listTitle
//        }
//        viewModel.create()
        val database = ShoppingListDatabase.getInstance(application)
        Networking.registerApplicationDir(application.filesDir.absolutePath, database)
        val listHandler = ShoppingListHandler(database = database)
        listHandler.CreateNewShoppingList("Neue Liste")
        Thread.sleep(500)
        listHandler.CreateNewShoppingList("Zweite Liste")
        Thread.sleep(500)

        // Expecting two lists to be created
        val itemDao = database.itemDao()
        val allItems = itemDao.getAllItems()
        Assert.assertEquals(0, allItems.size)
        val listDao = database.shoppingListDao()
        val allLists = listDao.getShoppingLists()
        Assert.assertEquals(2, allLists.size)
        return true
    }

    // This test should:
    // - Check if creating a new list works
    // - Check if the list is correctly stored in the database
    // - Check if the request online is made correctly
    @Test
    fun testCreateList() = runTest {
        Log.i("ListOperationTest", "Testing creating new list")
        val success = createList()
        assert(success)
    }


    private fun updateNonExistingList() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val sl = database.shoppingListDao()
        val list = ShoppingList(ID=0, Name = "1", CreatedBy = ListCreator(12, ""), LastEdited = "")
        sl.updateList(list)
        val lists = sl.getShoppingLists()
        for (list in lists) {
            println(list)
        }
    }

    private fun createEntryInDb() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val sl = database.shoppingListDao()
        val list = ShoppingList(ID=0, Name = "1", CreatedBy = ListCreator(12, ""), LastEdited = "")
        val list2 = ShoppingList(ID=0, Name = "2", CreatedBy = ListCreator(12, ""), LastEdited = "")
        sl.insertList(list)
        sl.insertList(list)
        val testId = sl.insertList(list2)
        Assert.assertEquals(3L, testId)
        val lists = sl.getShoppingLists()
        for (list in lists) {
            println(list)
        }
    }

    private fun insertExistingList() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val sl = database.shoppingListDao()
        val list = ShoppingList(ID=0, Name = "1", CreatedBy = ListCreator(12, ""), LastEdited = "")
        sl.insertList(list)
        var lists = sl.getShoppingLists()
        for (list in lists) {
            println(list)
        }
        list.Name = "Updated list"
        sl.insertList(list)
        lists = sl.getShoppingLists()
        for (list in lists) {
            println(list)
        }
    }

    private fun updateExistingListWithItems() {
        // TODO:
        // test adding new items
        // test removing old items
    }

    @Test
    fun AuxiliaryTest() = runTest {
//        createEntryInDb()
//        updateNonExistingList()
//        insertExistingList()
        Assert.fail("Not implemented")
    }

    fun testShareList() {

    }

    fun testRemoveList() {

    }

    private suspend fun loadList() : Boolean {
        val application = ApplicationProvider.getApplicationContext<Application>()
        AppUser.loadUser(application)
        AppUser.ID = 12
        AppUser.Username = "Franz"
        val database = ShoppingListDatabase.getInstance(application)
        Networking.registerApplicationDir(application.filesDir.absolutePath, database)
        val listHandler = ShoppingListHandler(database = database)
        listHandler.GetShoppingList(12L)
        return true
    }

    fun testLoadList() = runTest {
        Log.i("ListOperationTest", "Testing obtaining list from online")
        val success = loadList()
        assert(success)
    }

    fun testAddItemToList() {
        // Dont require to push the item to the server first
        // Simply add the item via the list (self-explanatory)

    }

}