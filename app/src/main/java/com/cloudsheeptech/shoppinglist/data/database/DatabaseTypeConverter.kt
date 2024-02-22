package com.cloudsheeptech.shoppinglist.data.database

import androidx.room.TypeConverter
import com.cloudsheeptech.shoppinglist.data.ListCreator
import com.cloudsheeptech.shoppinglist.data.DatabaseUser
import com.cloudsheeptech.shoppinglist.data.Serializer.OffsetDateTimeSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64.Encoder

class DatabaseTypeConverter {

    @TypeConverter
    fun offsetDateToString(date : OffsetDateTime) : String {
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        return date.format(formatter)
    }

    @TypeConverter
    fun stringToOffsetDate(date : String) : OffsetDateTime {
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        return OffsetDateTime.parse(date, formatter)
    }

    @TypeConverter
    fun userToString(user : DatabaseUser) : String {
        return Json.encodeToString(user)
    }

    @TypeConverter
    fun stringToUser(string : String) : DatabaseUser {
        return Json.decodeFromString<DatabaseUser>(string)
    }

    @TypeConverter
    fun listCreatorToString(listCreator: ListCreator) : String {
        return Json.encodeToString(listCreator)
    }

    @TypeConverter
    fun stringToListCreator(string: String) : ListCreator {
        return Json.decodeFromString(string)
    }

}