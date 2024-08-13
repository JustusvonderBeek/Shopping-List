package com.cloudsheeptech.shoppinglist.data.user

import android.app.Application
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudsheeptech.shoppinglist.data.Serializer.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.network.Networking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.net.ConnectException
import java.time.OffsetDateTime

@RunWith(AndroidJUnit4::class)
class AppUserOnlineTest {

    // Requires the server running in the background

    @Test
    fun testCreateUser() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)

        val tokenFile = application.filesDir.path + "/token.txt"
        val remoteApi = Networking(tokenFile)
        val appUserRemoteDataSource = AppUserRemoteDataSource(remoteApi)
        val appUserLocalDataSource = AppUserLocalDataSource(database)
        val username = "Online user"
        appUserLocalDataSource.create(username)
        val localUser = appUserLocalDataSource.getUser()
        Assert.assertNotNull(localUser)

        val remoteUser = appUserRemoteDataSource.create(localUser!!)
        Assert.assertNotNull(remoteUser)
        assert(0L != remoteUser!!.OnlineID)
        assert(remoteUser.Username == username)
        assert(remoteUser.Password == localUser.Password)
        Assert.assertNotNull(remoteUser.Created)
        val now = OffsetDateTime.now()
        Log.d("AppUserOnlineTest", "Now: $now / Created: ${remoteUser.Created}")
        // TODO: Fix the server and the UTC offset
//        assert(now.isEqual(remoteUser.Created!!) || now.isAfter(remoteUser.Created!!))
    }

    @Test(expected = NotImplementedError::class)
    fun testReadUser() = runTest {
        val remoteApi = Networking("")
        val appUserRemoteDataSource = AppUserRemoteDataSource(remoteApi)
        appUserRemoteDataSource.read()
    }

    @Test
    fun testUpdateUser() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val json = Json {
            serializersModule = SerializersModule {
                contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
            }
        }
        val appUserLocalDataSource = AppUserLocalDataSource(database)
        val username = "Online user"
        appUserLocalDataSource.create(username)
        val localUser = appUserLocalDataSource.getUser()
        Assert.assertNotNull(localUser)
        val apiUser = ApiUser(localUser!!.OnlineID, localUser.Username, localUser.Password, localUser.Created, null)
        val encodedUser = json.encodeToString(apiUser)
        val tokenFile = application.filesDir.path + "/token.txt"
        val remoteApi = Networking(tokenFile)
        remoteApi.resetSerializedUser(encodedUser, apiUser.onlineId)
        val appUserRemoteDataSource = AppUserRemoteDataSource(remoteApi)

        val remoteUser = appUserRemoteDataSource.create(localUser!!)
        Assert.assertNotNull(remoteUser)
        appUserLocalDataSource.resetOnlineId(remoteUser!!.OnlineID)

        // TODO: Update user information and send this requst to the remote
        val updatedUsername = "New Online User"
        appUserLocalDataSource.resetUsername(updatedUsername)
        val updatedLocalUser = appUserLocalDataSource.getUser()
        Assert.assertNotNull(updatedLocalUser)
        appUserRemoteDataSource.update(updatedLocalUser!!)

    }

    @Test
    fun testDeleteUser() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val json = Json {
            serializersModule = SerializersModule {
                contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
            }
        }
        val appUserLocalDataSource = AppUserLocalDataSource(database)
        val username = "Online user"
        appUserLocalDataSource.create(username)
        val localUser = appUserLocalDataSource.getUser()
        Assert.assertNotNull(localUser)
        val apiUser = ApiUser(localUser!!.OnlineID, localUser.Username, localUser.Password, localUser.Created, null)
        val encodedUser = json.encodeToString(apiUser)
        val tokenFile = application.filesDir.path + "token.txt"
        val remoteApi = Networking(tokenFile)
        remoteApi.resetSerializedUser(encodedUser, apiUser.onlineId)
        val appUserRemoteDataSource = AppUserRemoteDataSource(remoteApi)

        // TODO: Check how bad requests or different connection states influence the result
        try {
            val remoteUser = appUserRemoteDataSource.create(localUser!!)
            Assert.assertNotNull(remoteUser)
            appUserLocalDataSource.resetOnlineId(remoteUser!!.OnlineID)
        } catch (ex: ConnectException) {
            Assert.fail("Failed because server was not available")
        }
        val updatedLocalUser = appUserLocalDataSource.getUser()
        Assert.assertNotNull(updatedLocalUser)

        appUserRemoteDataSource.delete(updatedLocalUser!!)
    }

}