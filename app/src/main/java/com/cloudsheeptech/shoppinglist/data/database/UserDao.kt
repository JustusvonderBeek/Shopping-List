package com.cloudsheeptech.shoppinglist.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cloudsheeptech.shoppinglist.data.DatabaseUser

@Dao
interface UserDao {

    // To ensure that we only ever keep a single user
    // Ensure that the primary key of the class is fixed
    // and cannot be changed in the application

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user : DatabaseUser)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateUser(user : DatabaseUser)

    @Delete
    fun deleteUser(user : DatabaseUser)

    @Query("DELETE FROM user")
    fun resetUser()

    // -----------------------------------------------
    // Expecting only one user in total for the methods below to work!
    // -----------------------------------------------
    @Query("SELECT * FROM user LIMIT 1")
    fun getUser() : DatabaseUser?

    @Query("SELECT * FROM user LIMIT 1")
    fun getUserLive() : LiveData<DatabaseUser>
}