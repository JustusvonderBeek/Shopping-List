package com.cloudsheeptech.shoppinglist.data.onlineUser

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface OnlineUserDao {
    @Insert(onConflict =  OnConflictStrategy.REPLACE)
    fun insertUser(user: ListCreator)

    @Delete
    fun deleteUser(user : ListCreator)

    @Query("DELETE FROM online_user WHERE onlineId = :userId")
    fun deleteUser(userId : Long)

    @Update
    fun updateUser(user: ListCreator)

    @Query("SELECT * FROM online_user WHERE onlineId = :id")
    fun getUser(id : Long) : ListCreator?

    @Query("SELECT * FROM online_user")
    fun getAllOnlineUsers() : List<ListCreator>

}