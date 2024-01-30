package com.cloudsheeptech.shoppinglist.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cloudsheeptech.shoppinglist.data.User
import com.cloudsheeptech.shoppinglist.data.UserWire

@Dao
interface OnlineUserDao {

    @Insert
    fun insertUser(user: UserWire)

    @Delete
    fun deleteUser(user : UserWire)

    @Query("DELETE FROM online_user WHERE ID = :userId")
    fun deleteUser(userId : Long)

    @Update
    fun updateUser(user: UserWire)

    @Query("SELECT * FROM online_user WHERE ID = :id")
    fun getUser(id : Long) : UserWire?

    @Query("SELECT * FROM online_user")
    fun getAllOnlineUsers() : List<UserWire>

}