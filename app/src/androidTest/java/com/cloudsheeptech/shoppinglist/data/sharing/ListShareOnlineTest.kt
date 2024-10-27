package com.cloudsheeptech.shoppinglist.data.sharing

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.cloudsheeptech.shoppinglist.data.typeConverter.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.data.user.ApiUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.OffsetDateTime

@RunWith(JUnit4::class)
class ListShareOnlineTest {
    private val json =
        Json {
            serializersModule =
                SerializersModule {
                    contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
                }
            ignoreUnknownKeys = false
            encodeDefaults = true
        }

//    private suspend fun createRemoteDataSource() : Triple<ListShareRemoteDataSource, AppUserRepository, ShoppingListRepository> {
//        val application = ApplicationProvider.getApplicationContext<Application>()
//        val database = ShoppingListDatabase.getInstance(application)
//        val localUserDs = AppUserLocalDataSource(database)
//        val networking = Networking(application.filesDir.path + "/token.txt")
//        val remoteUserDs = AppUserRemoteDataSource(networking)
//        val userRepository = AppUserRepository(localUserDs, remoteUserDs)
//        userRepository.create("test user")
//        val remoteDs = ListShareRemoteDataSource(networking, userRepository)
//        val localItemDs = ItemLocalDataSource(database)
//        val itemRepo = ItemRepository(localItemDs)
//        val localItemToListDs = ItemToListLocalDataSource(database)
//        val itemToListRepository = ItemToListRepository(localItemToListDs)
//        val localDataSource = ShoppingListLocalDataSource(database, userRepository, itemRepo, itemToListRepository)
//        val remoteDataSource = ShoppingListRemoteDataSource(networking)
//        val slRepo = ShoppingListRepository(localDataSource, remoteDataSource, userRepository)
//        return Triple(remoteDs, userRepository, slRepo)
//    }

    private suspend fun createNewRemoteUser(): Long {
        var newId = 0L
        withContext(Dispatchers.IO) {
            val newUser = ApiUser(0L, "new user", "ignore", OffsetDateTime.now(), OffsetDateTime.now())
            val encodedUser = json.encodeToString(newUser)
            val application = ApplicationProvider.getApplicationContext<Application>()
//            val networking = Networking(application.filesDir.path + "/token.txt")
//            networking.POST("/v1/users", encodedUser) { resp ->
//                // Authentication already handled by the networking object
//                if (resp.status != HttpStatusCode.Created) {
//                    Log.w("AppUserRemoteDataSource", "Failed to create the user online!")
//                    throw IllegalArgumentException("bad request")
//                }
//                val rawBody = resp.bodyAsText(Charsets.UTF_8)
//                val parsedApiUser = json.decodeFromString<ApiUser>(rawBody)
//                newId = parsedApiUser.onlineId
//            }
        }
        return newId
    }

    @Test
    fun testCreateSharing() =
        runTest {
//            val (remoteShare, userRepo, slRepo) = createRemoteDataSource()
//
//            val remoteList = slRepo.create("new list")
//            // Creating a new user online before proceeding
//            val newUserId = createNewRemoteUser()
//            val success = remoteShare.create(remoteList.listId, newUserId)
//            assert(success)
        }

    @Test(expected = NotImplementedError::class)
    fun testGetSharing() =
        runTest {
            throw NotImplementedError("not used by this app")
        }

    @Test
    fun testUpdateSharing() =
        runTest {
//            val (remoteShare, userRepo, slRepo) = createRemoteDataSource()
//
//            val remoteList = slRepo.create("new list")
//            // Creating a new user online before proceeding
//            val newUserId = createNewRemoteUser()
//            val success = remoteShare.create(remoteList.listId, newUserId)
//            assert(success)
//
//            val secondNewUserId = createNewRemoteUser()
//            val updateSuccess = remoteShare.update(remoteList.listId, listOf(secondNewUserId))
//            assert(updateSuccess)
        }

    @Test
    fun testDeleteSharing() =
        runTest {
//            val (remoteShare, userRepo, slRepo) = createRemoteDataSource()
//
//            val remoteList = slRepo.create("new list")
//            val newUserId = createNewRemoteUser()
//            val success = remoteShare.create(remoteList.listId, newUserId)
//            assert(success)
//
//            val deleteSuccess = remoteShare.delete(remoteList.listId)
//            assert(deleteSuccess)
        }
}
