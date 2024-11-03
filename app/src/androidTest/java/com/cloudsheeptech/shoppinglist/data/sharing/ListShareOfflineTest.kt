package com.cloudsheeptech.shoppinglist.data.sharing

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ItemToListLocalDataSource
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ItemToListRepository
import com.cloudsheeptech.shoppinglist.data.items.ItemLocalDataSource
import com.cloudsheeptech.shoppinglist.data.items.ItemRepository
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListLocalDataSource
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListRemoteDataSource
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListRepository
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
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ListShareOfflineTest {
    private suspend fun createListShare(): Pair<ListShareLocalDataSource, ShoppingListRepository> {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val localUserDs = AppUserLocalDataSource(database)
        val payloadProvider = UserCreationDataProvider(localUserDs)
        val tokenProvider = TokenProvider(payloadProvider)
        val networking = Networking(tokenProvider)
        val remoteUserDs = AppUserRemoteDataSource(networking)
        val userRepository = AppUserRepository(localUserDs, remoteUserDs)
        // Creating the user for all following tests
        userRepository.create("test user")
        val localItemDs = ItemLocalDataSource(database)
        val itemRepo = ItemRepository(localItemDs)
        val localItemToListDs = ItemToListLocalDataSource(database)
        val itemToListRepository = ItemToListRepository(localItemToListDs)
        val localDataSource =
            ShoppingListLocalDataSource(database, userRepository, itemRepo, itemToListRepository)
        val remoteDataSource = ShoppingListRemoteDataSource(networking, userRepository)
        val slRepo = ShoppingListRepository(localDataSource, remoteDataSource, userRepository)
        val listShareDS = ListShareLocalDataSource(database, userRepository, slRepo)
        return Pair(listShareDS, slRepo)
    }

    @Test
    fun testCreateSharing() =
        runTest {
            val (listShare, slRepo) = createListShare()

            val list = slRepo.create("new list")
            Assert.assertNotNull(list)

            listShare.create(list.listId, 1234L)
            val application = ApplicationProvider.getApplicationContext<Application>()
            val database = ShoppingListDatabase.getInstance(application)
            val shareDao = database.sharedDao()
            val allShared = shareDao.getListSharedWith(list.listId)
            assert(allShared.isNotEmpty())
            Assert.assertEquals(1, allShared.size)
            Assert.assertEquals(list.listId, allShared[0].ListId)
            Assert.assertEquals(1234L, allShared[0].SharedWith)

            var exception = false
            try {
                listShare.create(1224L, 1234L)
            } catch (ex: IllegalArgumentException) {
                exception = true
            }
            assert(exception)
        }

    @Test
    fun testGetSharing() =
        runTest {
            val (listShare, slRepo) = createListShare()

            val list = slRepo.create("new list")
            Assert.assertNotNull(list)

            listShare.create(list.listId, 1234L)
            listShare.create(list.listId, 1235L)

            val shared = listShare.read(list.listId)
            Assert.assertEquals(2, shared.size)
            Assert.assertEquals(1234L, shared[0])
            Assert.assertEquals(1235L, shared[1])
        }

    @Test
    fun testUpdateSharing() =
        runTest {
            val (listShare, slRepo) = createListShare()

            val list = slRepo.create("new list")
            Assert.assertNotNull(list)

            listShare.create(list.listId, 1234L)
            listShare.create(list.listId, 1235L)

            val shared = listShare.read(list.listId)
            Assert.assertEquals(2, shared.size)
            val sharedRemoveList = shared.dropLast(1)
            listShare.update(list.listId, sharedRemoveList)

            val sharedAfterRemove = listShare.read(list.listId)
            Assert.assertEquals(1, sharedAfterRemove.size)
        }

    @Test
    fun testDeleteSharing() =
        runTest {
            val (listShare, slRepo) = createListShare()

            val list = slRepo.create("new list")
            Assert.assertNotNull(list)

            listShare.create(list.listId, 1234L)
            listShare.create(list.listId, 1235L)

            val shared = listShare.read(list.listId)
            Assert.assertEquals(2, shared.size)

            listShare.delete(list.listId)
            val sharedAfterRemove = listShare.read(list.listId)
            Assert.assertEquals(0, sharedAfterRemove.size)
        }
}
