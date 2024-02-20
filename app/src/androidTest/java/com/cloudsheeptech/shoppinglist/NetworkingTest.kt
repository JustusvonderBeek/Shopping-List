package com.cloudsheeptech.shoppinglist

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.data.DatabaseUser
import com.cloudsheeptech.shoppinglist.data.User
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.network.Networking
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.io.File

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class NetworkingTest {

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.cloudsheeptech.shoppinglist", appContext.packageName)
    }

    private suspend fun createUserAccount(fileDirPath : String) : Boolean {
        var success = false
        if (File(fileDirPath, "user.json").exists()) {
            success = true
            return success
        }
        val user = User(0, "test user", "test password")
        val encoded = Json.encodeToString(user)
        Networking.POST("auth/create", encoded) { resp ->
            if (resp.status != HttpStatusCode.Created)
                return@POST
            val body = resp.bodyAsText(Charsets.UTF_8)
            if (body.isEmpty())
                return@POST
            // Decode user to replace password
            val decodedUser = Json.decodeFromString<DatabaseUser>(body)
            decodedUser.Password = user.Password
            val encodedUser = Json.encodeToString(decodedUser)
            // Store user to disk to allow the test making use of it
            val file = File(fileDirPath, "user.json")
            val writer = file.writer(Charsets.UTF_8)
            writer.write(encodedUser)
            writer.close()
            success = true
        }
        Log.d("NetworkingTest", "User created")
        return success
    }

    private suspend fun getTestPathUnauth() : Boolean {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val db = ShoppingListDatabase.getInstance(appContext)
        Networking.registerApplicationDir(appContext.filesDir.absolutePath, db)
        var success = false
        Networking.GET("test/unauth") { resp ->
            println("Got an answer: ${resp.bodyAsText(Charsets.UTF_8)}")
            if (resp.status != HttpStatusCode.OK)
                return@GET
            if (resp.bodyAsText(Charsets.UTF_8) == "")
                return@GET
            success = true
        }
        return success
    }

    @Test
    fun testFetchingPathWithoutLogin() = runTest {
        println("Testing if the network can fetch a resource")
        // This should show that the network can automatically determine the state and reconnect / login if required
        val success = getTestPathUnauth()
        assert(success)
    }

    private suspend fun getTestPathAuth() : Boolean {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val db = ShoppingListDatabase.getInstance(appContext)
        Networking.registerApplicationDir(appContext.filesDir.absolutePath, db)
        // For debugging, create a new user first
        var success = createUserAccount(appContext.filesDir.absolutePath)
        assert(success)

        success = false
        var response = ""
        Networking.GET("v1/test/auth") { resp ->
            println("Got an answer: ${resp.bodyAsText(Charsets.UTF_8)}")
            if (resp.status != HttpStatusCode.OK)
                return@GET
            if (resp.bodyAsText(Charsets.UTF_8) == "")
                return@GET
            Log.d("NetworkingTest", "Authenticated!")
            response = resp.bodyAsText(Charsets.UTF_8)
            success = true
        }
        assertEquals("""{"status":"testing-content"}""", response.filter { !it.isWhitespace() })
        return success
    }

    @Test
    fun testFetchResourceAuth() = runTest {
        println("Testing if the network can automatically login and fetch a resource")
        val success = getTestPathAuth()
        assert(success)
    }

    @Test
    fun testFetchResourceTimeout() = runTest {
        println("Testing if the network automatically logs in when token timeouts")
        var success = getTestPathAuth()
        assert(success)
        Thread.sleep(5000)
        success = getTestPathAuth()
        assert(success)
    }

    private suspend fun postTestPathAuth() : Boolean {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val db = ShoppingListDatabase.getInstance(appContext)
        Networking.registerApplicationDir(appContext.filesDir.absolutePath, db)
        // For debugging, create a new user first
        var success = createUserAccount(appContext.filesDir.absolutePath)
        assert(success)

        success = false
        var answer = ""
        val item = Item(123, "Test Item", "Empty")
        val encodedItem = Json.encodeToString(item)
        Networking.POST("v1/test/auth", encodedItem) { resp ->
            println("Got an answer: ${resp.bodyAsText(Charsets.UTF_8)}")
            if (resp.status != HttpStatusCode.OK)
                return@POST
            if (resp.bodyAsText(Charsets.UTF_8) == "")
                return@POST
            Log.d("NetworkingTest", "Post Authenticated!")
            answer = resp.bodyAsText(Charsets.UTF_8)
            success = true
        }
        assertEquals("""{"status":"post-successful"}""", answer.filter { !it.isWhitespace() })
        return success
    }

    @Test
    fun testPostResourceAuth() = runTest {
        println("Testing if we can post resources authenticated")
        val success = postTestPathAuth()
        assert(success)
    }

}