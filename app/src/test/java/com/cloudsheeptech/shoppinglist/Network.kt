package com.cloudsheeptech.shoppinglist

import com.cloudsheeptech.shoppinglist.network.IHttpResponseHandler
import com.cloudsheeptech.shoppinglist.network.ShoppingListNetworkHandler
import com.cloudsheeptech.shoppinglist.network.UrlProviderEnum
import com.cloudsheeptech.shoppinglist.network.token.ShoppingListAuthenticationTokenProvider
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.DisableOnDebug
import org.junit.rules.TestRule
import org.junit.rules.Timeout
import kotlin.time.Duration.Companion.seconds

class Network {
    @JvmField
    @Rule
    val testRule: TestRule = DisableOnDebug(Timeout.seconds(500))

    @Test
    fun testTokenProvider() =
        runTest {
        }

    @Test
    fun testSimpleGet() =
        runTest(timeout = 500.seconds) {
            val payloadProvider = TestUserPayloadProvider()
            val tokenProvider = ShoppingListAuthenticationTokenProvider(payloadProvider)
            val network = ShoppingListNetworkHandler(tokenProvider)

            UrlProviderEnum.BASE_URL.url = "http://127.0.0.1:46152"
//            UrlProviderEnum.BASE_URL.url = "http://192.168.0.200:46152"
            val success =
                network.get(
                    "/v1/lists",
                    object : IHttpResponseHandler {
                        override suspend fun handle(response: HttpResponse): Boolean =
                            response.status == HttpStatusCode.OK
                    },
                )
            assert(success)
        }
}
