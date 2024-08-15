package com.cloudsheeptech.shoppinglist.data.typeConverter

import androidx.room.TypeConverter
import com.cloudsheeptech.shoppinglist.data.ListCreator
import com.cloudsheeptech.shoppinglist.data.user.AppUser
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

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
    fun userToString(user : AppUser) : String {
        return Json.encodeToString(user)
    }

    @TypeConverter
    fun stringToUser(string : String) : AppUser {
        return Json.decodeFromString<AppUser>(string)
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