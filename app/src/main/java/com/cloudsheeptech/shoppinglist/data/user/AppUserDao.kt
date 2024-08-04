package com.cloudsheeptech.shoppinglist.data.user

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AppUserDao {

    // To ensure that we only ever keep a single user
    // Ensure that the primary key of the class is fixed
    // and cannot be changed in the application

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(appUser : AppUser)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateUser(appUser : AppUser)

    @Delete
    fun deleteUser(appUser : AppUser)

    @Query("DELETE FROM user")
    fun resetAllUsers()

    // -----------------------------------------------
    // Expecting only one user in total for the methods below to work!
    // -----------------------------------------------
    @Query("SELECT * FROM user LIMIT 1")
    fun getUser() : AppUser?

    @Query("SELECT * FROM user LIMIT 1")
    fun getUserLive() : LiveData<AppUser>

    // IMPORTANT: This function is only meant for debugging purposes, not for
    // production
    @Query("SELECT * FROM user")
    fun debugGetAllUserEntries() : List<AppUser>
}