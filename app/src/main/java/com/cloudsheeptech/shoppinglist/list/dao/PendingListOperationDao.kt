package com.cloudsheeptech.shoppinglist.list.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.cloudsheeptech.shoppinglist.list.model.DbShoppingListOperation

@Dao
interface PendingListOperationDao {
    @Insert
    fun insert(operation: DbShoppingListOperation): Long

    @Query("UPDATE pending_list_operation SET serializedOp = :serializedOp WHERE id = :id")
    fun update(
        id: Long,
        serializedOp: String,
    )

    @Query("SELECT * FROM pending_list_operation")
    fun getAllOperations(): List<DbShoppingListOperation>

    @Query("DELETE FROM pending_list_operation")
    fun deleteAllOperations()
}
