package com.cloudsheeptech.shoppinglist.data.typeConverter

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = OffsetDateTime::class)
class OffsetDateTimeSerializer : KSerializer<OffsetDateTime> {

    private val formatter : DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    override fun serialize(encoder: Encoder, value: OffsetDateTime) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): OffsetDateTime {
        return OffsetDateTime.parse(decoder.decodeString(), formatter)
    }

}