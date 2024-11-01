package com.cloudsheeptech.shoppinglist.data.itemPrice

import android.util.Log
import com.cloudsheeptech.shoppinglist.data.items.AppItem
import com.cloudsheeptech.shoppinglist.data.typeConverter.BigDecimalSerializer
import com.cloudsheeptech.shoppinglist.data.typeConverter.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.network.UrlProviderEnum
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.math.BigDecimal
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemPriceRemoteDataSource
    @Inject
    constructor(
        private val networking: Networking,
    ) {
        private val json =
            Json {
                encodeDefaults = false
                ignoreUnknownKeys = false
                serializersModule =
                    SerializersModule {
                        contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
                        contextual(BigDecimal::class, BigDecimalSerializer())
                    }
            }

        suspend fun get(itemList: List<AppItem>): List<ItemPrice> {
            val itemPriceList = mutableListOf<ItemPrice>()
            withContext(Dispatchers.IO) {
                networking.GET(UrlProviderEnum.ITEM_PRICE.url) { response ->
                    if (response.status != HttpStatusCode.OK) {
                        Log.d("ItemPriceRemoteDataSource", "Failed to get item prices from remote")
                        return@GET
                    }
                    val rawBody = response.bodyAsText(Charsets.UTF_8)
                    val convertedItemList = json.decodeFromString<List<ItemPrice>>(rawBody)
                    itemPriceList.addAll(convertedItemList)
                }
            }
            return itemPriceList
        }

        // Other functionality not needed here
    }
