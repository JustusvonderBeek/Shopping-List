package com.cloudsheeptech.shoppinglist.data.database

import androidx.room.TypeConverter
import com.cloudsheeptech.shoppinglist.data.ListCreator
import com.cloudsheeptech.shoppinglist.data.DatabaseUser
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DatabaseTypeConverter {

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