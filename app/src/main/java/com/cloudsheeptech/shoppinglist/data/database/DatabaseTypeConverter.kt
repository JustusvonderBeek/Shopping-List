package com.cloudsheeptech.shoppinglist.data.database

import androidx.room.TypeConverter
import com.cloudsheeptech.shoppinglist.data.User
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DatabaseTypeConverter {

    @TypeConverter
    fun userToString(user : User) : String {
        return Json.encodeToString(user)
    }

    @TypeConverter
    fun stringToUser(string : String) : User {
        return Json.decodeFromString<User>(string)
    }

}