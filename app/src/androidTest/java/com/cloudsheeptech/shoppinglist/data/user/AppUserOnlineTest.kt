package com.cloudsheeptech.shoppinglist.data.user

import android.app.Application
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudsheeptech.shoppinglist.ShoppingListApplication
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.network.TokenProvider
import com.cloudsheeptech.shoppinglist.testUtil.TestUtil
import kotlinx.coroutines.test.runTest
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
        val remoteApi = Networking(tokenProvider)
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

            val success =
                TestUtil.shoppingListApplication.appUserRemoteDataSource.update(newOnlineUser)
            assert(success)
            Thread.sleep(10)
            val updatedLocalUser = TestUtil.shoppingListApplication.appUserLocalDataSource.getUser()
            Assert.assertNotNull(updatedLocalUser)
            assert(0L != updatedLocalUser!!.OnlineID)
            Log.d(
                "AppUserOnlineTest",
                "Now: ${OffsetDateTime.now()} / Created: ${updatedLocalUser.Created}",
            )
            // TODO: Fix the server and the UTC offset
//        assert(now.isEqual(remoteUser.Created!!) || now.isAfter(remoteUser.Created!!))
        }

    // This functionality is not used right now, but might be in the future
    @Test(expected = NotImplementedError::class)
    fun testReadUser() =
        runTest {
            TestUtil.initialize()
            val remoteAppUserDataSource = TestUtil.shoppingListApplication.appUserRemoteDataSource
            remoteAppUserDataSource.read()
        }

    @Test
    fun testUpdateUser() =
        runTest {
            TestUtil.initialize()

            val username = "test user update"
            val appUserOnlineDataSource = TestUtil.shoppingListApplication.appUserRemoteDataSource
            val appUserOfflineDataSource = TestUtil.shoppingListApplication.appUserLocalDataSource
            appUserOfflineDataSource.create(username)
            Thread.sleep(10)
            val appUser = appUserOfflineDataSource.getUser()
            Assert.assertNotNull(appUser)
            var success = appUserOnlineDataSource.update(appUser!!)
            assert(success)

            val updatedUsername = "test user updated name for test"
            appUser.Username = updatedUsername
            success = appUserOnlineDataSource.update(appUser)
            assert(success)
        }

    @Test
    fun testDeleteUser() =
        runTest {
            TestUtil.initialize()

            val username = "test user update"
            val appUserOnlineDataSource = TestUtil.shoppingListApplication.appUserRemoteDataSource
            val appUserOfflineDataSource = TestUtil.shoppingListApplication.appUserLocalDataSource
            appUserOfflineDataSource.create(username)
            appUserOfflineDataSource.store()
            Thread.sleep(10)
            val appUser = appUserOfflineDataSource.getUser()
            Assert.assertNotNull(appUser)
            var success = appUserOnlineDataSource.update(appUser!!)
            assert(success)

            success = appUserOnlineDataSource.delete(appUser)
            assert(success)
        }
}
