package com.cloudsheeptech.shoppinglist.data.recipe

import android.util.Log
import com.cloudsheeptech.shoppinglist.data.typeConverter.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.network.ShoppingListAPI
import com.cloudsheeptech.shoppinglist.network.UrlProviderEnum
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

@Singleton
class RecipeRemoteDataSource
    @Inject
    constructor(
        private val networking: Networking,
        private val certificate: InputStream,
    ) {
        private val json =
            Json {
                encodeDefaults = true
                ignoreUnknownKeys = false
                serializersModule =
                    SerializersModule {
                        contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
                    }
            }

        suspend fun create(receipt: ApiRecipe): Boolean {
            var success = false
            val certFactory = CertificateFactory.getInstance("X509")
            // Load system resource under 'raw' directory into input stream
            val expectedCert = certFactory.generateCertificate(certificate)
            certificate.close()
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null)
            keyStore.setCertificateEntry("self-signed", expectedCert)

            val trustManager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManager.init(keyStore)
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustManager.trustManagers, null)

            val okhttp =
                OkHttpClient
                    .Builder()
                    .sslSocketFactory(
                        sslContext.socketFactory,
                        trustManager.trustManagers[0] as X509TrustManager,
                    ).build()
            val retrofit =
                Retrofit
                    .Builder()
                    .baseUrl(UrlProviderEnum.BASE_URL.url)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okhttp)
                    .build()
            val shoppingListAPI = retrofit.create(ShoppingListAPI::class.java)
            shoppingListAPI.createRecipe(receipt, emptyArray())
//            val encodedReceipt = json.encodeToString(receipt)
//            networking.POST("/v1/recipe", encodedReceipt) { response ->
//                if (response.status != HttpStatusCode.Created) {
//                    Log.e("ReceiptRemoteDataSource", "Failed to create remote receipt")
//                    return@POST
//                }
//                success = true
//            }
            return success
        }

        suspend fun read(
            receiptId: Long,
            createdBy: Long,
        ): ApiRecipe? {
            var onlineReceipt: ApiRecipe? = null
            networking.GET("/v1/recipe/$receiptId?createdBy=$createdBy") { response ->
                if (response.status != HttpStatusCode.OK) {
                    Log.e(
                        "ReceiptRemoteDataSource",
                        "Failed to get receipt $receiptId from $createdBy online",
                    )
                    return@GET
                }
                val rawBody = response.bodyAsText(Charsets.UTF_8)
                if (rawBody.isEmpty() || rawBody == "null") {
                    Log.w("ReceiptRemoteDataSource", "List $receiptId from $createdBy not found online")
                    return@GET
                }
                val decoded = json.decodeFromString<ApiRecipe>(rawBody)
                onlineReceipt = decoded
                Log.d(
                    "ReceiptRemoteDataSource",
                    "Found receipt $receiptId with ${onlineReceipt?.ingredients?.size} online",
                )
            }
            return onlineReceipt
        }

        suspend fun update(receipt: ApiRecipe): Boolean {
            var success = false
            val encodedReceipt = json.encodeToString(receipt)
            networking.PUT(
                "/v1/recipe/${receipt.onlineId}?createdBy=${receipt.createdBy.onlineId}",
                encodedReceipt,
            ) { response ->
                if (response.status != HttpStatusCode.OK) {
                    Log.e(
                        "ReceiptRemoteDataSource",
                        "Failed to update receipt ${receipt.onlineId} online",
                    )
                    return@PUT
                }
                success = true
            }
            return success
        }

        suspend fun delete(
            receiptId: Long,
            createdBy: Long,
        ): Boolean {
            var success = false
            networking.DELETE("/v1/recipe/$receiptId?createdBy=$createdBy") { response ->
                if (response.status != HttpStatusCode.OK) {
                    Log.e("ReceiptRemoteDataSource", "Failed to delete receipt $receiptId online")
                    return@DELETE
                }
                success = true
            }
            return success
        }
    }
