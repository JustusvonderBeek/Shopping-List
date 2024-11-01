package com.cloudsheeptech.shoppinglist.data.typeConverter

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal

/**
 * Check if this class works
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BigDecimal::class)
class BigDecimalSerializer : KSerializer<BigDecimal> {
    override fun serialize(
        encoder: Encoder,
        value: BigDecimal,
    ) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal = BigDecimal(decoder.decodeString())
}
