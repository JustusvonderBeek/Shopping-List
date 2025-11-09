package com.cloudsheeptech.shoppinglist.list.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.cloudsheeptech.shoppinglist.list.model.PendingListOperation

@Dao
interface PendingListOperationDao {
    @Insert
    fun insert(operation: PendingListOperation): Long

    @Query("SELECT * FROM pending_list_operation")
    fun getAllOperations(): List<PendingListOperation>

    @Query("DELETE FROM pending_list_operation")
    fun deleteAllOperations()
}
