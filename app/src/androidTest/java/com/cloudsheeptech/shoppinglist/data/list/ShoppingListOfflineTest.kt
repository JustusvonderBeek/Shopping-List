package com.cloudsheeptech.shoppinglist.data.list

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.cloudsheeptech.shoppinglist.data.ListCreator
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ItemToListLocalDataSource
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ItemToListRepository
import com.cloudsheeptech.shoppinglist.data.items.ItemLocalDataSource
import com.cloudsheeptech.shoppinglist.data.items.ItemRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUserLocalDataSource
import com.cloudsheeptech.shoppinglist.data.user.AppUserRemoteDataSource
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.network.Networking
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.OffsetDateTime

@RunWith(JUnit4::class)
class ShoppingListOfflineTest {

    @Test
    fun testCreateList() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()

        val database = ShoppingListDatabase.getInstance(application)
        val localUserDs = AppUserLocalDataSource(database)
        val networking = Networking(application.filesDir.path + "/token.txt")
        val remoteUserDs = AppUserRemoteDataSource(networking)
        val userRepository = AppUserRepository(localUserDs, remoteUserDs)
        val localItemDs = ItemLocalDataSource(database)
        val itemRepo = ItemRepository(localItemDs)
        val localItemToListDs = ItemToListLocalDataSource(database)
        val itemToListRepository = ItemToListRepository(localItemToListDs)
        val localDataSource = ShoppingListLocalDataSource(database, userRepository, itemRepo, itemToListRepository)
        val newList = ApiShoppingList(0L, "title", ListCreator(1234L, "creator"), OffsetDateTime.now(), OffsetDateTime.now(), mutableListOf())
        val insertedId = localDataSource.create(newList)
        Assert.assertEquals(1L, insertedId)

        val secondList = ApiShoppingList(0L, "second title", ListCreator(1234L, "creator"), OffsetDateTime.now(), OffsetDateTime.now(), mutableListOf())
        val secondInsertedId = localDataSource.create(secondList)
        Assert.assertEquals(2L, secondInsertedId)

        secondList.listId = secondInsertedId
        var exception = false
        try {
            val retryInsertion = localDataSource.create(secondList)
        } catch (ex: IllegalArgumentException) {
            exception = true
        }
        assert(exception)
    }

    @Test
    fun testGetList() = runTest {

    }

    @Test
    fun testUpdateList() = runTest {

    }

    @Test
    fun testDeleteList() = runTest {

    }

}