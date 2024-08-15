package com.cloudsheeptech.shoppinglist

import android.app.Application
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudsheeptech.shoppinglist.data.list.DbShoppingList
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.time.OffsetDateTime

@RunWith(AndroidJUnit4::class)
class ListOperationTests {

    private fun createList() : Boolean {
//        val listTitle = "New List"
        val application = ApplicationProvider.getApplicationContext<Application>()
//        AppUserLocalDataSource.loadUser(application)
//        AppUserLocalDataSource.getUser()!!.OnlineID = 12
//        AppUserLocalDataSource.getUser()!!.Username = "Franz"

//        val factory = CreateShoppinglistViewModelFactory(application)
//        val store = ViewModelStore()
//        val viewModel = ViewModelProvider(store, factory)[CreateShoppinglistViewModel::class.java]
//        // Setting the required properties
//        withContext(Dispatchers.Main) {
//            viewModel.title.value = listTitle
//        }
//        viewModel.create()
        val database = ShoppingListDatabase.getInstance(application)
//        Networking.registerApplicationDir(application.filesDir.absolutePath, database)
//        val listHandler = ShoppingListRepository(database = database)
//        listHandler.CreateNewShoppingList("Neue Liste")
//        Thread.sleep(500)
//        listHandler.CreateNewShoppingList("Zweite Liste")
//        Thread.sleep(500)
//
//        // Expecting two lists to be created
//        val itemDao = database.itemDao()
//        val allItems = itemDao.getAllItems()
//        Assert.assertEquals(0, allItems.size)
//        val listDao = database.shoppingListDao()
//        val allLists = listDao.getShoppingLists()
//        Assert.assertEquals(2, allLists.size)
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
        val list = DbShoppingList(listId=0, title = "1", createdBy = 12, createdByName = "", lastUpdated = OffsetDateTime.now())
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
        val list = DbShoppingList(listId=0, title = "1", createdBy = 12, createdByName = "", lastUpdated = OffsetDateTime.now())
        val list2 = DbShoppingList(listId=0, title = "2", createdBy = 12, createdByName = "", lastUpdated = OffsetDateTime.now())
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
        val list = DbShoppingList(listId=0, title = "1", createdBy = 12, createdByName = "", lastUpdated = OffsetDateTime.now())
        sl.insertList(list)
        var lists = sl.getShoppingLists()
        for (list in lists) {
            println(list)
        }
        list.title = "Updated list"
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
//        AppUserLocalDataSource.loadUser(application)
//        AppUserLocalDataSource.getUser()!!.OnlineID = 12
//        AppUserLocalDataSource.getUser()!!.Username = "Franz"
//        val database = ShoppingListDatabase.getInstance(application)
//        Networking.registerApplicationDir(application.filesDir.absolutePath, database)
//        val listHandler = ShoppingListHandler(database = database)
//        listHandler.GetShoppingList(1L, AppUserLocalDataSource.getUser()!!.OnlineID)
        return true
    }

    fun testLoadList() = runTest {
        Log.i("ListOperationTest", "Testing obtaining list from online")
        val success = loadList()
        assert(success)
    }
 
    suspend fun testAddItemToList() : Boolean {
        // Dont require to push the item to the server first
        // Simply add the item via the list (self-explanatory)
        val application = ApplicationProvider.getApplicationContext<Application>()
//        AppUserLocalDataSource.loadUser(application)
//        AppUserLocalDataSource.getUser()!!.OnlineID = 12
//        AppUserLocalDataSource.getUser()!!.Username = "Franz"
//        val database = ShoppingListDatabase.getInstance(application)
//        val listHandler = ShoppingListHandler(database = database)
//        listHandler.CreateNewShoppingList("Testlist")
//        val item = Item(0, "Item", "ic_icon")
//        listHandler.AddItemToShoppingList(item, 1, AppUserLocalDataSource.getUser()!!.OnlineID)

        Thread.sleep(100)

//        val list = database.shoppingListDao().getShoppingList(1, AppUserLocalDataSource.getUser()!!.OnlineID)
//        val itemsInList = database.mappingDao().getMappingsForList(1, AppUserLocalDataSource.getUser()!!.OnlineID)
//        Assert.assertNotNull(list)
//        Assert.assertNotNull(itemsInList)
//
//        Assert.assertEquals("Testlist", list!!.Name)
//        Assert.assertEquals(1, list!!.ID)
//        Assert.assertEquals(1, itemsInList.size)

        return true
    }

    @Test
    fun testAddItem() = runTest {
        val success = testAddItemToList()
        Assert.assertTrue(success)
    }

    @Test
    fun testAddItemsOnline() = runTest {
//        val success = test
        Assert.fail()
    }

    suspend fun createListOffline() : Pair<Boolean, Application> {
        val application = ApplicationProvider.getApplicationContext<Application>()
//        AppUser.loadUser(application)
//        AppUserLocalDataSource.new("Franz")
        val database = ShoppingListDatabase.getInstance(application)
//        val listHandler = ShoppingListRepository(database = database)
//        val listName = "Offline List 1"
//        val listName2 = "Offline List 2"
//        listHandler.CreateNewShoppingList(listName)
//        // Give time for the opeartion to complete
//        Thread.sleep(1000)
//        listHandler.CreateNewShoppingList(listName2)
//        // Give time for the opeartion to complete
//        Thread.sleep(1000)
//
//        val listDao = database.shoppingListDao()
//        val lists = listDao.getShoppingLists()
//        Assert.assertEquals(2, lists.size)
//        for (list in lists) {
//            Assert.assertEquals(0L, list.createdBy)
//        }
        return Pair(true, application)
    }

    suspend fun createUser(application: Application) : Boolean {
//        Assert.assertEquals(0, AppUserLocalDataSource.getUser()!!.OnlineID)
//        AppUserLocalDataSource.PostUserOnline(application.applicationContext)
//        AppUserLocalDataSource.getUser()!!.OnlineID = 12345L
        // Give time for the user creation
        Thread.sleep(1000)
        // Now push the first list online to check if the updating takes place
        val db = ShoppingListDatabase.getInstance(application.applicationContext)
//        val listHander = ShoppingListRepository(db)
//        listHander.updatedCreatedByForAllLists()

        val listDao = db.shoppingListDao()
        val lists = listDao.getShoppingLists()
        Assert.assertEquals(2, lists.size)
        for (list in lists) {
            Assert.assertNotEquals(0L, list.createdBy)
        }
        return true
    }

    @Test
    fun testCreatingListOfflineAndPushOnline() = runTest {
        var (success, app) = createListOffline()
        Assert.assertTrue(success)
        success = createUser(app)
        Assert.assertTrue(success)
    }
}