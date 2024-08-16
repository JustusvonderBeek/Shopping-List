package com.cloudsheeptech.shoppinglist.data.list

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.cloudsheeptech.shoppinglist.data.ListCreator
import com.cloudsheeptech.shoppinglist.data.items.ApiItem
import com.cloudsheeptech.shoppinglist.data.typeConverter.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.data.user.ApiUser
import com.cloudsheeptech.shoppinglist.data.user.AppUser
import com.cloudsheeptech.shoppinglist.data.user.AppUserRemoteDataSource
import com.cloudsheeptech.shoppinglist.network.Networking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import java.time.OffsetDateTime

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.DEFAULT)
class ShoppingListOnlineTest {

    private fun AppUser.toApiUser() : ApiUser {
        return ApiUser(
            onlineId = this.OnlineID,
            username = this.Username,
            password = this.Password,
            created = this.Created,
            lastLogin = this.Created,
        )
    }

    private suspend fun createRemoteSLDataSource() : Pair<ShoppingListRemoteDataSource, AppUser> {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val networking = Networking(application.filesDir.path + "/token.txt")
        val remoteDataSource = ShoppingListRemoteDataSource(networking)
        val remoteUserDs = AppUserRemoteDataSource(networking)
        val appUser = AppUser(OnlineID = 0L, Created = OffsetDateTime.now(), ID = 1L, Password = "secure", Username = "username")
        val remoteAppUser = remoteUserDs.create(appUser)
        val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = false
            serializersModule = SerializersModule {
                contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
            }
        }
        val encoded = json.encodeToString(remoteAppUser!!.toApiUser())
        networking.resetSerializedUser(encoded, remoteAppUser!!.OnlineID)
        return Pair(remoteDataSource, remoteAppUser)
    }

    @Test
    fun testCreateList() = runTest {
        val (remoteDataSource, appUser) = createRemoteSLDataSource()

        val newListWithoutItems = ApiShoppingList(0L, "list without titles", ListCreator(appUser.OnlineID, appUser.Username), OffsetDateTime.now(), OffsetDateTime.now(), mutableListOf())
        var success = remoteDataSource.create(newListWithoutItems)
        assert(success)

        val listWithItems = ApiShoppingList(12L, "list with items", ListCreator(appUser.OnlineID, appUser.Username), OffsetDateTime.now(), OffsetDateTime.now(), mutableListOf())
        for (num in 1..3) {
            val item = ApiItem("item $num", "empty icon", quantity = num.toLong(), checked = num % 2 == 0, appUser.OnlineID)
            listWithItems.items.add(item)
        }
        success = remoteDataSource.create(listWithItems)
        assert(success)
    }

    @Test
    fun testGetList() = runTest {
        val (remoteDataSource, appUser) = createRemoteSLDataSource()

        val listWithItems = ApiShoppingList(12L, "list with items", ListCreator(appUser.OnlineID, appUser.Username), OffsetDateTime.now(), OffsetDateTime.now(), mutableListOf())
        for (num in 1..3) {
            val item = ApiItem("item $num", "empty icon", quantity = num.toLong(), checked = num % 2 == 0, appUser.OnlineID)
            listWithItems.items.add(item)
        }
        val success = remoteDataSource.create(listWithItems)
        assert(success)

        // TODO: Seems like the online lists contains the number of elements
        val remoteList = remoteDataSource.read(listWithItems.listId, listWithItems.createdBy.onlineId)
        Assert.assertNotNull(remoteList)
        Assert.assertEquals(listWithItems.title, remoteList!!.title)
        Assert.assertEquals(listWithItems, remoteList)
    }

    @Test
    fun testGetAllLists() = runTest {
        val (remoteDataSource, appUser) = createRemoteSLDataSource()

        val listWithItems = ApiShoppingList(12L, "list with items", ListCreator(appUser.OnlineID, appUser.Username), OffsetDateTime.now(), OffsetDateTime.now(), mutableListOf())
        for (num in 1..3) {
            val item = ApiItem("item $num", "empty icon", quantity = num.toLong(), checked = num % 2 == 0, appUser.OnlineID)
            listWithItems.items.add(item)
        }
        var success = remoteDataSource.create(listWithItems)
        assert(success)

        // Create a second list, for now that should be sufficient
        val secondListWithItems = listWithItems.copy()
        secondListWithItems.title = "second list with items"
        secondListWithItems.listId = 4L
        secondListWithItems.items = mutableListOf()
        for (num in 1..2) {
            val item = ApiItem("item $num", "empty icon", quantity = num.toLong(), checked = num % 2 == 0, appUser.OnlineID)
            secondListWithItems.items.add(item)
        }
        success = remoteDataSource.create(secondListWithItems)
        assert(success)

        val onlineLists = remoteDataSource.readAll()
        assert(onlineLists.isNotEmpty())
        Assert.assertEquals(2, onlineLists.size)
        if (onlineLists[0].listId == listWithItems.listId) {
            Assert.assertEquals(listWithItems, onlineLists[0])
            Assert.assertEquals(secondListWithItems, onlineLists[1])
        } else {
            Assert.assertEquals(listWithItems, onlineLists[1])
            Assert.assertEquals(secondListWithItems, onlineLists[0])
        }
    }

    @Test
    fun testUpdateList() = runTest {
        val (remoteDataSource, appUser) = createRemoteSLDataSource()

        val listWithItems = ApiShoppingList(12L, "list with items", ListCreator(appUser.OnlineID, appUser.Username), OffsetDateTime.now(), OffsetDateTime.now(), mutableListOf())
        for (num in 1..3) {
            val item = ApiItem("item $num", "empty icon", quantity = num.toLong(), checked = num % 2 == 0, appUser.OnlineID)
            listWithItems.items.add(item)
        }
        var success = remoteDataSource.create(listWithItems)
        assert(success)

        listWithItems.title = "update list with items"
        listWithItems.items.removeAt(2)
        success = remoteDataSource.update(listWithItems)
        assert(success)

        val remoteLists = remoteDataSource.readAll()
        assert(remoteLists.isNotEmpty())
        Assert.assertEquals(1, remoteLists.size)
        Assert.assertEquals(listWithItems, remoteLists[0])
    }

    @Test(expected = NotImplementedError::class)
    fun testUpdateSharedList() = runTest {
        throw NotImplementedError("Not yet implemented")
    }

    @Test
    fun testDeleteList() = runTest {
        val (remoteDataSource, appUser) = createRemoteSLDataSource()

        val listWithItems = ApiShoppingList(12L, "list with items", ListCreator(appUser.OnlineID, appUser.Username), OffsetDateTime.now(), OffsetDateTime.now(), mutableListOf())
        for (num in 1..3) {
            val item = ApiItem("item $num", "empty icon", quantity = num.toLong(), checked = num % 2 == 0, appUser.OnlineID)
            listWithItems.items.add(item)
        }
        var success = remoteDataSource.create(listWithItems)
        assert(success)

        success = remoteDataSource.deleteShoppingList(listWithItems.listId)
        assert(success)

        val remoteList = remoteDataSource.read(listWithItems.listId, listWithItems.createdBy.onlineId)
        Assert.assertNull(remoteList)
    }

}