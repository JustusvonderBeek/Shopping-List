package com.cloudsheeptech.shoppinglist.data.sharing

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SharedDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertShared(shared: ListShareDatabase)

    @Update
    fun updateShared(shared: ListShareDatabase)

    @Delete
    fun deleteShared(shared: ListShareDatabase)

    @Query("DELETE FROM shared_table WHERE ListId = :listId AND CreatedBy = :createdBy AND SharedWith = :sharedWith")
    fun delete(
        listId: Long,
        createdBy: Long,
        sharedWith: Long,
    )

    @Query("DELETE FROM shared_table WHERE ListId = :listId AND CreatedBy = :createdBy")
    fun deleteAllFromList(
        listId: Long,
        createdBy: Long,
    )

    @Query("DELETE FROM shared_table WHERE SharedWith = :userId AND ListId = :listId")
    fun deleteForUser(
        userId: Long,
        listId: Long,
    )

    @Query("SELECT * FROM shared_table WHERE ListId = :listId AND CreatedBy = :createdBy")
    fun getListSharedWith(
        listId: Long,
        createdBy: Long,
    ): List<ListShareDatabase>

    @Query("SELECT * FROM shared_table WHERE ListId = :listId AND CreatedBy = :createdBy")
    fun getListSharedWithLive(
        listId: Long,
        createdBy: Long,
    ): LiveData<List<ListShareDatabase>>

    @Query(
        "SELECT u.onlineId as UserId, u.username as Name, 1 as Shared FROM shared_table st JOIN online_user u ON st.SharedWith = u.onlineId WHERE st.ListId = :listId AND st.CreatedBy = :createdBy",
    )
    fun getListPreviewSharedWith(
        listId: Long,
        createdBy: Long,
    ): LiveData<List<ShareUserPreview>>
}
