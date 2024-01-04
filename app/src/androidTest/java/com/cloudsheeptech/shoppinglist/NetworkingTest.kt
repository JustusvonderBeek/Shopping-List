package com.cloudsheeptech.shoppinglist

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudsheeptech.shoppinglist.network.Networking
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

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
        assertEquals("com.cloudsheeptech.shop", appContext.packageName)
    }

    private suspend fun getTestPathUnauth() : Boolean {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        Networking.registerApplicationDir(appContext.filesDir.absolutePath)
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
        Networking.registerApplicationDir(appContext.filesDir.absolutePath)
        var success = false
        Networking.GET("v1/test/auth") { resp ->
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
    fun testFetchResourceAuth() = runTest {
        println("Testing if the network can automatically login and fetch a resource")
        val success = getTestPathAuth()
        assert(success)
    }

}