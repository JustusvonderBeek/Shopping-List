package com.cloudsheeptech.shoppinglist.data.user

import android.app.Application
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudsheeptech.shoppinglist.ShoppingListApplication
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.typeConverter.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.network.TokenProvider
import com.cloudsheeptech.shoppinglist.testUtil.TestUtil
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.time.OffsetDateTime

/** Testing the creation and handling of the user online
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.DEFAULT)
class AppUserOnlineTest {
    // Requires the server running in the background

    private fun setupRemoteAppUserDataSource(): Triple<AppUserLocalDataSource, AppUserRemoteDataSource, Networking> {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)

        val tokenFile = application.filesDir.path + "/token.txt"
        val appUserLocalDataSource = AppUserLocalDataSource(database)
        val userCreationPayloadProvider = UserCreationDataProvider(appUserLocalDataSource)
        val tokenProvider = TokenProvider(userCreationPayloadProvider)
        val remoteApi = Networking(tokenFile, tokenProvider)
        val appUserRemoteDataSource = AppUserRemoteDataSource(remoteApi)
        appUserLocalDataSource.create("test user")
        return Triple(appUserLocalDataSource, appUserRemoteDataSource, remoteApi)
    }

    private fun createLocalUser(
        shoppingListApplication: ShoppingListApplication,
        name: String,
    ) {
        shoppingListApplication.appUserLocalDataSource.create(name)
    }

    @Test
    fun testCreateUser() =
        runTest {
            TestUtil.initialize()

            val username = "Online user"
            createLocalUser(TestUtil.shoppingListApplication, username)
            val newOnlineUser = TestUtil.shoppingListApplication.appUserLocalDataSource.getUser()!!
            newOnlineUser.Username += " changed for test"

            val success = TestUtil.shoppingListApplication.appUserRemoteDataSource.update(newOnlineUser)
            assert(success)
            Thread.sleep(10)
            val updatedLocalUser = TestUtil.shoppingListApplication.appUserLocalDataSource.getUser()
            Assert.assertNotNull(updatedLocalUser)
            assert(0L != updatedLocalUser!!.OnlineID)
            Log.d("AppUserOnlineTest", "Now: ${OffsetDateTime.now()} / Created: ${updatedLocalUser.Created}")
            // TODO: Fix the server and the UTC offset
//        assert(now.isEqual(remoteUser.Created!!) || now.isAfter(remoteUser.Created!!))
        }

    // This functionality is not used right now, but might be in the future
    @Test(expected = NotImplementedError::class)
    fun testReadUser() =
        runTest {
            val (localSource, remoteSource, api) = setupRemoteAppUserDataSource()
            remoteSource.read()
        }

    @Test
    fun testUpdateUser() =
        runTest {
            val application = ApplicationProvider.getApplicationContext<Application>()
            val database = ShoppingListDatabase.getInstance(application)
            val json =
                Json {
                    serializersModule =
                        SerializersModule {
                            contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
                        }
                }
            val username = "Online user"
            val now = OffsetDateTime.now()
            val appUser = AppUser(1L, 0L, username, "secure", now)
            val apiUser = ApiUser(appUser.OnlineID, appUser.Username, appUser.Password, appUser.Created, appUser.Created)
            val encodedUser = json.encodeToString(apiUser)
            val tokenFile = application.filesDir.path + "/token.txt"
//            val remoteApi = Networking(tokenFile)
//            remoteApi.resetSerializedUser(encodedUser, apiUser.onlineId)
//            val appUserRemoteDataSource = AppUserRemoteDataSource(remoteApi)
//
//            val remoteUser = appUserRemoteDataSource.create(appUser)
//            Assert.assertNotNull(remoteUser)
//
//            // TODO: Update user information and send this requst to the remote
//            val updatedUsername = "New Online User"
//            appUser.OnlineID = remoteUser.OnlineID
//            appUser.Username = updatedUsername
//            appUserRemoteDataSource.update(appUser)
        }

    @Test
    fun testDeleteUser() =
        runTest {
            val application = ApplicationProvider.getApplicationContext<Application>()
            val database = ShoppingListDatabase.getInstance(application)
            val json =
                Json {
                    serializersModule =
                        SerializersModule {
                            contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
                        }
                }
            val username = "Online user"
            val now = OffsetDateTime.now()
            val appUser = AppUser(1L, 0L, username, "secure", now)
            val apiUser = ApiUser(appUser.OnlineID, appUser.Username, appUser.Password, appUser.Created, appUser.Created)
            val encodedUser = json.encodeToString(apiUser)
            val tokenFile = application.filesDir.path + "token.txt"
//            val remoteApi = Networking(tokenFile)
//            remoteApi.resetSerializedUser(encodedUser, apiUser.onlineId)
//            val appUserRemoteDataSource = AppUserRemoteDataSource(remoteApi)
//
//            // TODO: Check how bad requests or different connection states influence the result
//            var remoteUser: AppUser? = null
//            try {
//                remoteUser = appUserRemoteDataSource.create(appUser)
//                Assert.assertNotNull(remoteUser)
//            } catch (ex: ConnectException) {
//                Assert.fail("Failed because server was not available")
//            }
//            appUserRemoteDataSource.delete(remoteUser!!)
        }
}
