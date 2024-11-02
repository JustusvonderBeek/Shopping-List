package com.cloudsheeptech.shoppinglist.data.list

import android.app.Application
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ItemToListLocalDataSource
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ItemToListRepository
import com.cloudsheeptech.shoppinglist.data.items.ItemLocalDataSource
import com.cloudsheeptech.shoppinglist.data.items.ItemRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUserLocalDataSource
import com.cloudsheeptech.shoppinglist.data.user.AppUserRemoteDataSource
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.data.user.UserCreationDataProvider
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.network.TokenProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.time.OffsetDateTime

@RunWith(AndroidJUnit4::class)
class DbShoppingListLocalDataSourceTest {
    private fun ApiShoppingList.toDbList(): DbShoppingList {
        val dbList =
            DbShoppingList(
                listId = this.listId,
                title = this.title,
                createdBy = this.createdBy.onlineId,
                createdByName = this.createdBy.username,
                lastUpdated = this.lastUpdated,
            )
//        val dbItems = this.items.map { item -> item.toDbItem() }
        return dbList
    }

    private fun createDefaultList(): ApiShoppingList {
        val list =
            ApiShoppingList(
                listId = 0L,
                title = "Default List",
                createdBy = ApiListCreator(1234L, "creator"),
                createdAt = OffsetDateTime.now(),
                lastUpdated = OffsetDateTime.now(),
                items = mutableListOf(),
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

    private fun checkDbContains(dbShoppingLists: List<ApiShoppingList>): Boolean {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)

        val slDao = database.shoppingListDao()
        val compareLists = slDao.getShoppingLists()
        if (compareLists.size != dbShoppingLists.size) {
            return false
        }
        for (list in dbShoppingLists) {
            if (!compareLists.contains(list.toDbList())) {
                return false
            }
        }
        return true
    }

    @Test
    fun testListInsert() =
        runTest {
            val application = ApplicationProvider.getApplicationContext<Application>()
            val database = ShoppingListDatabase.getInstance(application)
            val localUserDs = AppUserLocalDataSource(database)
            val payloadProvider = UserCreationDataProvider(localUserDs)
            val tokenProvider = TokenProvider(payloadProvider)
            val networking = Networking(application.filesDir.path + "/token.txt", tokenProvider)
            val remoteUserDs = AppUserRemoteDataSource(networking)
            val userRepository = AppUserRepository(localUserDs, remoteUserDs)
            val localItemDs = ItemLocalDataSource(database)
            val itemRepo = ItemRepository(localItemDs)
            val localItemToListDs = ItemToListLocalDataSource(database)
            val itemToListRepository = ItemToListRepository(localItemToListDs)
            val localDataSource =
                ShoppingListLocalDataSource(
                    database,
                    userRepository,
                    itemRepo,
                    itemToListRepository,
                )

            val list = createDefaultList()
            val insertId = localDataSource.create(list)
//        Log.d("DatabaseListHandlerTest", "List: $list")

            assert(insertId == 1L)
            assert(list.listId == 0L)
            val list2 = list.copy()
            list.listId = insertId
            assert(checkDbContains(listOf(list)))

//        Log.d("DatabaseListHandlerTest", "Value: $insertId")
            // Checks that the list is not modified inside the function!

            val insertId2 = localDataSource.create(list2)
            assert(insertId2 == 2L)
            assert(list2.listId == 0L)
            list2.listId = insertId
            assert(checkDbContains(listOf(list, list2)))
        }

    @Test
    fun testListRetrieve() =
        runTest {
            val application = ApplicationProvider.getApplicationContext<Application>()
            val database = ShoppingListDatabase.getInstance(application)
            val localUserDs = AppUserLocalDataSource(database)
            val payloadProvider = UserCreationDataProvider(localUserDs)
            val tokenProvider = TokenProvider(payloadProvider)
            val networking = Networking(application.filesDir.path + "/token.txt", tokenProvider)
            val remoteUserDs = AppUserRemoteDataSource(networking)
            val userRepository = AppUserRepository(localUserDs, remoteUserDs)
            val localItemDs = ItemLocalDataSource(database)
            val itemRepo = ItemRepository(localItemDs)
            val localItemToListDs = ItemToListLocalDataSource(database)
            val itemToListRepository = ItemToListRepository(localItemToListDs)
            val dbHandler =
                ShoppingListLocalDataSource(
                    database,
                    userRepository,
                    itemRepo,
                    itemToListRepository,
                )

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

            val retrievedList = dbHandler.read(insertId, list.createdBy.onlineId)
            Assert.assertNotNull(retrievedList)
            // Fix the insertion ID
//        Log.e("DatabaseListHandlerTest", "List: $list, Inserted: $retrievedList")
            assert(list == retrievedList)
            val retrievedList2 = dbHandler.read(insertedId2, list2.createdBy.onlineId)
//        Log.e("DatabaseListHandlerTest", "List: $list2, Inserted: $retrievedList2")
            Assert.assertNotNull(retrievedList2)
            assert(list2 == retrievedList2)
        }

    @Test
    fun testListUpdate() =
        runTest {
            val application = ApplicationProvider.getApplicationContext<Application>()
            val database = ShoppingListDatabase.getInstance(application)
            val localUserDs = AppUserLocalDataSource(database)
            val payloadProvider = UserCreationDataProvider(localUserDs)
            val tokenProvider = TokenProvider(payloadProvider)
            val networking = Networking(application.filesDir.path + "/token.txt", tokenProvider)
            val remoteUserDs = AppUserRemoteDataSource(networking)
            val userRepository = AppUserRepository(localUserDs, remoteUserDs)
            val localItemDs = ItemLocalDataSource(database)
            val itemRepo = ItemRepository(localItemDs)
            val localItemToListDs = ItemToListLocalDataSource(database)
            val itemToListRepository = ItemToListRepository(localItemToListDs)
            val dbHandler =
                ShoppingListLocalDataSource(
                    database,
                    userRepository,
                    itemRepo,
                    itemToListRepository,
                )

            val list = createDefaultList()
            val insertId = dbHandler.create(list)
            list.listId = insertId

            val containedLists = mutableListOf(list)
            assert(insertId == 1L)
            assert(checkDbContains(containedLists))

            list.title = "New list name"
            // Reference: Yes!
            Log.i("DatabaseListHandlerTest", "Updated list: $containedLists")

            dbHandler.update(list)
            assert(checkDbContains(containedLists))
        }

    @Test
    fun testUpdateLastEdited() =
        runTest {
            val application = ApplicationProvider.getApplicationContext<Application>()
            val database = ShoppingListDatabase.getInstance(application)
            val localUserDs = AppUserLocalDataSource(database)
            val payloadProvider = UserCreationDataProvider(localUserDs)
            val tokenProvider = TokenProvider(payloadProvider)
            val networking = Networking(application.filesDir.path + "/token.txt", tokenProvider)
            val remoteUserDs = AppUserRemoteDataSource(networking)
            val userRepository = AppUserRepository(localUserDs, remoteUserDs)
            val localItemDs = ItemLocalDataSource(database)
            val itemRepo = ItemRepository(localItemDs)
            val localItemToListDs = ItemToListLocalDataSource(database)
            val itemToListRepository = ItemToListRepository(localItemToListDs)
            val dbHandler =
                ShoppingListLocalDataSource(
                    database,
                    userRepository,
                    itemRepo,
                    itemToListRepository,
                )

            val list = createDefaultList()
            val insertId = dbHandler.create(list)
            list.listId = insertId

            val containedLists = mutableListOf(list)
            assert(insertId == 1L)
            assert(checkDbContains(containedLists))

            dbHandler.update(list)
            val updatedList = dbHandler.read(list.listId, list.createdBy.onlineId)
            Assert.assertNotNull(updatedList)
            assert(updatedList!!.listId == insertId)
            containedLists.clear()
            containedLists.add(updatedList)
            assert(checkDbContains(containedLists))

            // Test if list cannot be found
//        val updatedList2 = dbHandler.update(insertId, list.createdBy + 10)
//        Assert.assertNull(updatedList2)
        }

    @Test
    fun testDeleteList() =
        runTest {
            val application = ApplicationProvider.getApplicationContext<Application>()
            val database = ShoppingListDatabase.getInstance(application)
            val localUserDs = AppUserLocalDataSource(database)
            val payloadProvider = UserCreationDataProvider(localUserDs)
            val tokenProvider = TokenProvider(payloadProvider)
            val networking = Networking(application.filesDir.path + "/token.txt", tokenProvider)
            val remoteUserDs = AppUserRemoteDataSource(networking)
            val userRepository = AppUserRepository(localUserDs, remoteUserDs)
            val localItemDs = ItemLocalDataSource(database)
            val itemRepo = ItemRepository(localItemDs)
            val localItemToListDs = ItemToListLocalDataSource(database)
            val itemToListRepository = ItemToListRepository(localItemToListDs)
            val dbHandler =
                ShoppingListLocalDataSource(
                    database,
                    userRepository,
                    itemRepo,
                    itemToListRepository,
                )

            val list = createDefaultList()
            val insertId = dbHandler.create(list)
            list.listId = insertId

            val containedLists = mutableListOf(list)
            assert(insertId == 1L)
            assert(checkDbContains(containedLists))

            dbHandler.delete(list.listId, list.createdBy.onlineId)

            assert(checkDbContains(emptyList()))
        }
}
