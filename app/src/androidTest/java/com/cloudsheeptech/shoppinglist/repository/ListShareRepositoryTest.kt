package com.cloudsheeptech.shoppinglist.repository

import android.app.Application
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ItemToListLocalDataSource
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ItemToListRepository
import com.cloudsheeptech.shoppinglist.data.items.ItemLocalDataSource
import com.cloudsheeptech.shoppinglist.data.items.ItemRepository
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListLocalDataSource
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListRemoteDataSource
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListRepository
import com.cloudsheeptech.shoppinglist.data.sharing.ListShareLocalDataSource
import com.cloudsheeptech.shoppinglist.data.sharing.ListShareRemoteDataSource
import com.cloudsheeptech.shoppinglist.data.sharing.ListShareRepository
import com.cloudsheeptech.shoppinglist.data.typeConverter.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.data.user.ApiUser
import com.cloudsheeptech.shoppinglist.data.user.AppUserLocalDataSource
import com.cloudsheeptech.shoppinglist.data.user.AppUserRemoteDataSource
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.network.Networking
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.OffsetDateTime

@RunWith(JUnit4::class)
class ListShareRepositoryTest {

    private val json = Json {
        serializersModule = SerializersModule {
            contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
        }
        ignoreUnknownKeys = false
        encodeDefaults = true
    }

    private suspend fun createListShareRepo() : Triple<ListShareRepository, AppUserRepository, ShoppingListRepository> {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val localUserDs = AppUserLocalDataSource(database)
        val networking = Networking(application.filesDir.path + "/token.txt")
        val remoteUserDs = AppUserRemoteDataSource(networking)
        val userRepository = AppUserRepository(localUserDs, remoteUserDs)
        userRepository.create("test user")
        val remoteDs = ListShareRemoteDataSource(networking, userRepository)
        val localItemDs = ItemLocalDataSource(database)
        val itemRepo = ItemRepository(localItemDs)
        val localItemToListDs = ItemToListLocalDataSource(database)
        val itemToListRepository = ItemToListRepository(localItemToListDs)
        val localDataSource = ShoppingListLocalDataSource(database, userRepository, itemRepo, itemToListRepository)
        val remoteDataSource = ShoppingListRemoteDataSource(networking)
        val slRepo = ShoppingListRepository(localDataSource, remoteDataSource, userRepository)
        val localShareDs = ListShareLocalDataSource(database, userRepository, slRepo)
        val shareRepo = ListShareRepository(localShareDs, remoteDs)
        return Triple(shareRepo, userRepository, slRepo)
    }

    private suspend fun createNewRemoteUser() : Long {
        var newId = 0L
        withContext(Dispatchers.IO) {
            val newUser = ApiUser(0L,"new user", "ignore", OffsetDateTime.now(), OffsetDateTime.now())
            val encodedUser = json.encodeToString(newUser)
            val application = ApplicationProvider.getApplicationContext<Application>()
            val networking = Networking(application.filesDir.path + "/token.txt")
            networking.POST("/v1/users", encodedUser) { resp ->
                // Authentication already handled by the networking object
                if (resp.status != HttpStatusCode.Created) {
                    Log.w("AppUserRemoteDataSource", "Failed to create the user online!")
                    throw IllegalArgumentException("bad request")
                }
                val rawBody = resp.bodyAsText(Charsets.UTF_8)
                val parsedApiUser = json.decodeFromString<ApiUser>(rawBody)
                newId = parsedApiUser.onlineId
            }
        }
        return newId
    }

    @Test
    fun testCreateSharing() = runTest {
        val (listShareRepo, userRepo, listRepo) = createListShareRepo()

        val list = listRepo.create("new list")
        val newUserId = createNewRemoteUser()
        listShareRepo.create(list.listId, newUserId)

        val shared = listShareRepo.read(list.listId)
        Assert.assertEquals(1, shared.size)
        Assert.assertEquals(newUserId, shared[0])
    }

    @Test
    fun testGetSharing() = runTest {
        val (listShareRepo, userRepo, listRepo) = createListShareRepo()

        val list = listRepo.create("new list")
        val newUserId = createNewRemoteUser()
        listShareRepo.create(list.listId, newUserId)

        val shared = listShareRepo.read(list.listId)
        Assert.assertEquals(1, shared.size)
        Assert.assertEquals(newUserId, shared[0])
    }

    @Test
    fun testUpdateSharing() = runTest {
        val (listShareRepo, userRepo, listRepo) = createListShareRepo()

        val list = listRepo.create("new list")
        val newUserId = createNewRemoteUser()
        listShareRepo.create(list.listId, newUserId)

        val shared = listShareRepo.read(list.listId)
        Assert.assertEquals(1, shared.size)
        Assert.assertEquals(newUserId, shared[0])

        val secondNewUserId = createNewRemoteUser()
        listShareRepo.update(list.listId, listOf(newUserId, secondNewUserId))
        val updatedShare = listShareRepo.read(list.listId)
        Assert.assertEquals(2, updatedShare.size)
        Assert.assertEquals(newUserId, updatedShare[0])
        Assert.assertEquals(secondNewUserId, updatedShare[1])
    }

    @Test
    fun testDeleteSharing() = runTest {
        val (listShareRepo, userRepo, listRepo) = createListShareRepo()

        val list = listRepo.create("new list")
        val newUserId = createNewRemoteUser()
        listShareRepo.create(list.listId, newUserId)

        val shared = listShareRepo.read(list.listId)
        Assert.assertEquals(1, shared.size)
        Assert.assertEquals(newUserId, shared[0])

        listShareRepo.delete(list.listId)
        val deletedShare = listShareRepo.read(list.listId)
        Assert.assertEquals(0, deletedShare.size)
    }

}