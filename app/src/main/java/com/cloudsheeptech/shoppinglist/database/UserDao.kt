package com.cloudsheeptech.shoppinglist.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cloudsheeptech.shoppinglist.data.User

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user : User)

    @Update
    fun updateUser(user : User)

    @Delete
    fun deleteUser(user : User)

    @Query("DELETE FROM user_table")
    fun resetUser()

    // -----------------------------------------------
    // Expecting only one user in total for the methods below to work!
    // -----------------------------------------------
    @Query("SELECT * FROM user_table LIMIT 1")
    fun getUser() : User?

    @Query("SELECT * FROM user_table LIMIT 1")
    fun getUserLive() : LiveData<User>
}