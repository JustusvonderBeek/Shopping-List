package com.cloudsheeptech.shoppinglist.util

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = OffsetDateTime::class)
class OffsetDateTimeFormatHandler :
    TypeAdapter<OffsetDateTime>(),
    KSerializer<OffsetDateTime> {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    override fun serialize(
        encoder: Encoder,
        value: OffsetDateTime,
    ) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): OffsetDateTime = OffsetDateTime.parse(decoder.decodeString(), formatter)

    override fun write(
        out: JsonWriter?,
        value: OffsetDateTime?,
    ) {
        val formattedTime = value?.format(formatter) ?: ""
        out?.value(formattedTime)
    }

    override fun read(`in`: JsonReader?): OffsetDateTime? = OffsetDateTime.parse(`in`?.toString(), formatter)
}
