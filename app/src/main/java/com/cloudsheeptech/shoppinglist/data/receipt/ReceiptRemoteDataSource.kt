package com.cloudsheeptech.shoppinglist.data.receipt

import android.util.Log
import com.cloudsheeptech.shoppinglist.data.typeConverter.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.network.Networking
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptRemoteDataSource @Inject constructor(
    private val networking: Networking,
) {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
        serializersModule = SerializersModule {
            contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
        }
    }

    suspend fun create(receipt: ApiReceipt) : Boolean {
        var success = false
        val encodedReceipt = json.encodeToString(receipt)
        networking.POST("/v1/receipts", encodedReceipt) { response ->
            if (response.status != HttpStatusCode.OK) {
                Log.e("ReceiptRemoteDataSource", "Failed to create remote receipt")
                return@POST
            }
            success = true
        }
        return success
    }

    suspend fun read(receiptId: Long, createdBy: Long) : ApiReceipt? {
        var onlineReceipt : ApiReceipt? = null
        networking.GET("/v1/receipts/$receiptId?createdBy=$createdBy") { response ->
            if (response.status != HttpStatusCode.OK) {
                Log.e("ReceiptRemoteDataSource", "Failed to get receipt $receiptId from $createdBy online")
                return@GET
            }
            val rawBody = response.bodyAsText(Charsets.UTF_8)
            if (rawBody.isEmpty() || rawBody == "null") {
                Log.w("ReceiptRemoteDataSource", "List $receiptId from $createdBy not found online")
                return@GET
            }
            val decoded = json.decodeFromString<ApiReceipt>(rawBody)
            onlineReceipt = decoded
            Log.d("ReceiptRemoteDataSource", "Found receipt $receiptId with ${onlineReceipt?.ingredients?.size} online")
        }
        return onlineReceipt
    }

    suspend fun update(receipt: ApiReceipt) : Boolean {
        var success = false
        val encodedReceipt = json.encodeToString(receipt)
        networking.PUT("/v1/receipts/${receipt.onlineId}?createdBy=${receipt.createdBy}", encodedReceipt) { response ->
            if (response.status != HttpStatusCode.OK) {
                Log.e("ReceiptRemoteDataSource", "Failed to update receipt ${receipt.onlineId} online")
                return@PUT
            }
            success = true
        }
        return success
    }

    suspend fun delete(receiptId: Long, createdBy: Long) : Boolean {
        var success = false
        networking.DELETE("/v1/receipts/$receiptId?createdBy=$createdBy") { response ->
            if (response.status != HttpStatusCode.OK) {
                Log.e("ReceiptRemoteDataSource", "Failed to delete receipt $receiptId online")
                return@DELETE
            }
            success = true
        }
        return success
    }

}