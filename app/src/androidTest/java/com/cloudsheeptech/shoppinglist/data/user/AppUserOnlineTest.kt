package com.cloudsheeptech.shoppinglist.data.user

import android.app.Application
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudsheeptech.shoppinglist.data.typeConverter.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.network.Networking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.net.ConnectException
import java.time.OffsetDateTime

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.DEFAULT)
class AppUserOnlineTest {

    // Requires the server running in the background



    @Test
    fun testCreateUser() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)

        val tokenFile = application.filesDir.path + "/token.txt"
        val remoteApi = Networking(tokenFile)
        val appUserRemoteDataSource = AppUserRemoteDataSource(remoteApi)
        val now = OffsetDateTime.now()
        val username = "Online user"
        val newOnlineUser = AppUser(ID = 1L, OnlineID = 0L, username, "secure", now)

        val remoteUser = appUserRemoteDataSource.create(newOnlineUser)
        Assert.assertNotNull(remoteUser)
        assert(0L != remoteUser!!.OnlineID)
        assert(remoteUser.Username == username)
        assert(remoteUser.Password == newOnlineUser.Password)
        Assert.assertNotNull(remoteUser.Created)
        Log.d("AppUserOnlineTest", "Now: $now / Created: ${remoteUser.Created}")
        // TODO: Fix the server and the UTC offset
//        assert(now.isEqual(remoteUser.Created!!) || now.isAfter(remoteUser.Created!!))
    }

    // This functionality is not used right now, but might be in the future
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
        val username = "Online user"
        val now = OffsetDateTime.now()
        val appUser = AppUser(1L, 0L, username, "secure", now)
        val apiUser = ApiUser(appUser.OnlineID, appUser.Username, appUser.Password, appUser.Created, appUser.Created)
        val encodedUser = json.encodeToString(apiUser)
        val tokenFile = application.filesDir.path + "/token.txt"
        val remoteApi = Networking(tokenFile)
        remoteApi.resetSerializedUser(encodedUser, apiUser.onlineId)
        val appUserRemoteDataSource = AppUserRemoteDataSource(remoteApi)

        val remoteUser = appUserRemoteDataSource.create(appUser)
        Assert.assertNotNull(remoteUser)

        // TODO: Update user information and send this requst to the remote
        val updatedUsername = "New Online User"
        appUser.OnlineID = remoteUser!!.OnlineID
        appUser.Username = updatedUsername
        appUserRemoteDataSource.update(appUser)
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
        val username = "Online user"
        val now = OffsetDateTime.now()
        val appUser = AppUser(1L, 0L, username, "secure", now)
        val apiUser = ApiUser(appUser.OnlineID, appUser.Username, appUser.Password, appUser.Created, appUser.Created)
        val encodedUser = json.encodeToString(apiUser)
        val tokenFile = application.filesDir.path + "token.txt"
        val remoteApi = Networking(tokenFile)
        remoteApi.resetSerializedUser(encodedUser, apiUser.onlineId)
        val appUserRemoteDataSource = AppUserRemoteDataSource(remoteApi)

        // TODO: Check how bad requests or different connection states influence the result
        var remoteUser : AppUser? = null
        try {
            remoteUser = appUserRemoteDataSource.create(appUser)
            Assert.assertNotNull(remoteUser)
        } catch (ex: ConnectException) {
            Assert.fail("Failed because server was not available")
        }
        appUserRemoteDataSource.delete(remoteUser!!)
    }

}