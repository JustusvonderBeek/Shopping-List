package com.cloudsheeptech.shoppinglist.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cloudsheeptech.shoppinglist.data.UIPreference

@Dao
interface UIPreferencesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPreference(pref : UIPreference)

    @Update
    fun updatePreference(pref : UIPreference)

    @Query("DELETE FROM ui_preferences WHERE ListId = :listId")
    fun deletePreferenceForList(listId : Long)

    @Query("DELETE FROM ui_preferences")
    fun clearPreferences()

    @Query("SELECT * FROM ui_preferences WHERE ListId = :listId")
    fun getPreferenceForList(listId: Long) : UIPreference?

    @Query("SELECT * FROM ui_preferences WHERE ListId = :listId")
    fun getPreferencesForListLive(listId: Long) : LiveData<UIPreference>
}