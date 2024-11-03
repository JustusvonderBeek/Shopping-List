package com.cloudsheeptech.shoppinglist.data.onlineUser

import android.app.Application
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.typeConverter.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.data.user.ApiUser
import com.cloudsheeptech.shoppinglist.data.user.AppUserLocalDataSource
import com.cloudsheeptech.shoppinglist.data.user.AppUserRemoteDataSource
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.data.user.UserCreationDataProvider
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.network.TokenProvider
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
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
class OnlineUserOnlineTest {
    private val json =
        Json {
            serializersModule =
                SerializersModule {
                    contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
                }
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private suspend fun createRemoteDataSource(): OnlineUserRemoteDataSource {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val localUserDs = AppUserLocalDataSource(database)
        val payloadProvider = UserCreationDataProvider(localUserDs)
        val tokenProvider = TokenProvider(payloadProvider)
        val networking = Networking(tokenProvider)
        val remoteUserDs = AppUserRemoteDataSource(networking)
        val userRepository = AppUserRepository(localUserDs, remoteUserDs)
        userRepository.create("test user")
        val user = userRepository.read()!!
        return OnlineUserRemoteDataSource(networking)
    }

    private suspend fun createNewRemoteUser(): Pair<Long, String> {
        var newId = 0L
        var username = ""
        withContext(Dispatchers.IO) {
            val newUser =
                ApiUser(0L, "distinct", "ignore", OffsetDateTime.now(), OffsetDateTime.now())
            val encodedUser = json.encodeToString(newUser)
            val application = ApplicationProvider.getApplicationContext<Application>()
            val database = ShoppingListDatabase.getInstance(application)
            val localUserDs = AppUserLocalDataSource(database)
            val payloadProvider = UserCreationDataProvider(localUserDs)
            val tokenProvider = TokenProvider(payloadProvider)
            val networking = Networking(tokenProvider)
            networking.POST("/v1/users", encodedUser) { resp ->
                // Authentication already handled by the networking object
                if (resp.status != HttpStatusCode.Created) {
                    Log.w("AppUserRemoteDataSource", "Failed to create the user online!")
                    throw IllegalArgumentException("bad request")
                }
                val rawBody = resp.bodyAsText(Charsets.UTF_8)
                val parsedApiUser = json.decodeFromString<ApiUser>(rawBody)
                newId = parsedApiUser.onlineId
                username = newUser.username
            }
        }
        return Pair(newId, username)
    }

    @Test
    fun testReadOnlineUser() =
        runTest {
            val remoteDS = createRemoteDataSource()
            var username = ""
            val emptyList = remoteDS.read(username)
            assert(emptyList.isEmpty())

            val (_, remoteUsername) = createNewRemoteUser()

            username = remoteUsername.dropLast(2)
            val users = remoteDS.read(username)
            assert(users.isNotEmpty())

            Assert.assertEquals("distinct", users[0].username)
        }
}
