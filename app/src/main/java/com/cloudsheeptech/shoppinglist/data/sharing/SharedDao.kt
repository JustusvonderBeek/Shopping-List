package com.cloudsheeptech.shoppinglist.data.sharing

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SharedDao {

    @Insert
    fun insertShared(shared : ListShareDatabase)

    @Update
    fun updateShared(shared: ListShareDatabase)

    @Delete
    fun deleteShared(shared: ListShareDatabase)

    @Query("DELETE FROM shared_table WHERE ID = :sharedId")
    fun deleteShared(sharedId : Long)

    @Query("DELETE FROM shared_table WHERE ListId = :listId")
    fun deleteAllFromList(listId : Long)

    @Query("DELETE FROM shared_table WHERE SharedWith = :userId AND ListId = :listId")
    fun deleteForUser(userId : Long, listId: Long)

    @Query("SELECT * FROM shared_table WHERE ListId = :listId")
    fun getListSharedWith(listId : Long) : List<ListShareDatabase>

    @Query("SELECT * FROM shared_table WHERE ListId = :listId")
    fun getListSharedWithLive(listId : Long) : LiveData<List<ListShareDatabase>>

}