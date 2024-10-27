package com.cloudsheeptech.shoppinglist.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.onlineUser.ListCreator
import com.cloudsheeptech.shoppinglist.data.typeConverter.OffsetDateTimeSerializer
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.After
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import java.time.OffsetDateTime

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.DEFAULT)
class OnlineUserRepositoryTest {
    private val json =
        Json {
            serializersModule =
                SerializersModule {
                    contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
                }
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private fun createDefaultListCreator(): ListCreator =
        ListCreator(
            onlineId = 12345L,
            username = "test creator",
        )

//    private suspend fun createOnlineUserRepo() : OnlineUserRepository {
//        val application = ApplicationProvider.getApplicationContext<Application>()
//        val database = ShoppingListDatabase.getInstance(application)
//        val localUserDs = AppUserLocalDataSource(database)
//        val networking = Networking(application.filesDir.path + "/token.txt")
//        val remoteUserDs = AppUserRemoteDataSource(networking)
//        val userRepository = AppUserRepository(localUserDs, remoteUserDs)
//        userRepository.create("test user")
//        val user = userRepository.read()!!
//        val apiUser = ApiUser(user.OnlineID, user.Username, user.Password, user.Created, user.Created)
//        val serializedUser = json.encodeToString(apiUser)
//        networking.resetSerializedUser(serializedUser, user.OnlineID)
//        val localOnlineUserDs = OnlineUserLocalDataSource(database)
//        val remoteOnlineUserDs = OnlineUserRemoteDataSource(networking)
//        val onlineUserRepo = OnlineUserRepository(localOnlineUserDs, remoteOnlineUserDs)
//        return onlineUserRepo
//    }

    private suspend fun createNewRemoteUser(): Pair<Long, String> {
        var newId = 0L
        var username = ""
//        withContext(Dispatchers.IO) {
//            val newUser = ApiUser(0L, "distinct", "ignore", OffsetDateTime.now(), OffsetDateTime.now())
//            val encodedUser = json.encodeToString(newUser)
//            val application = ApplicationProvider.getApplicationContext<Application>()
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
//                username = newUser.username
//            }
//        }
        return Pair(newId, username)
    }

    @After
    fun deleteAllEntries() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        database.clearAllTables()
    }

    @Test
    fun testCreateUser() =
        runTest {
//            val onlineUserRepo = createOnlineUserRepo()
//            val creator = createDefaultListCreator()
//
//            onlineUserRepo.create(creator)
//
//            val retrievedUser = onlineUserRepo.read(creator.onlineId)
//            Assert.assertNotNull(retrievedUser)
//            Assert.assertEquals(creator, retrievedUser)
        }

    @Test
    fun testReadUser() =
        runTest {
//            val onlineUserRepo = createOnlineUserRepo()
//            val creator = createDefaultListCreator()
//
//            onlineUserRepo.create(creator)
//
//            val retrievedUser = onlineUserRepo.read(creator.onlineId)
//            Assert.assertNotNull(retrievedUser)
//            Assert.assertEquals(creator, retrievedUser)
//
//            val (newId, newUsername) = createNewRemoteUser()
//            val users = onlineUserRepo.readOnline(newUsername.dropLast(1))
//            assert(users.isNotEmpty())
//            Assert.assertEquals(newUsername, users[0].username)
//        Assert.assertEquals(newId, users[0].onlineId)
        }

    @Test
    fun testUpdateUser() =
        runTest {
//            val onlineUserRepo = createOnlineUserRepo()
//            val creator = createDefaultListCreator()
//
//            onlineUserRepo.create(creator)
//
//            val retrievedUser = onlineUserRepo.read(creator.onlineId)
//            Assert.assertNotNull(retrievedUser)
//            Assert.assertEquals(creator, retrievedUser)
//
//            creator.username = "new totally different username"
//            onlineUserRepo.update(creator)
//
//            val updatedRetrievedUser = onlineUserRepo.read(creator.onlineId)
//            Assert.assertNotNull(updatedRetrievedUser)
//            Assert.assertEquals(creator, updatedRetrievedUser)
        }

    @Test
    fun testDeleteUser() =
        runTest {
//            val onlineUserRepo = createOnlineUserRepo()
//            val creator = createDefaultListCreator()
//
//            onlineUserRepo.create(creator)
//
//            val retrievedUser = onlineUserRepo.read(creator.onlineId)
//            Assert.assertNotNull(retrievedUser)
//            Assert.assertEquals(creator, retrievedUser)
//
//            onlineUserRepo.delete(creator.onlineId)
//
//            val nullUser = onlineUserRepo.read(creator.onlineId)
//            Assert.assertNull(nullUser)
        }
}
