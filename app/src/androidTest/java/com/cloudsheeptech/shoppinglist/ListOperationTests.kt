package com.cloudsheeptech.shoppinglist

import android.app.Application
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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

    private suspend fun createList() : Boolean {
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
        val itemDao = database.itemListDao()
        val allItems = itemDao.getAllItems()
        Assert.assertEquals(0, allItems.size)
        val listDao = database.shoppingListDao()
        val allLists = listDao.getShoppingLists()
        Assert.assertEquals(2, allLists.size)
        return true
    }

    @Test
    fun testCreateList() = runTest {
        Log.i("ListOperationTest", "Testing creating new list")
        val success = createList()
        assert(success)
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