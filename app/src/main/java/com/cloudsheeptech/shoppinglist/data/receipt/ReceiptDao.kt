package com.cloudsheeptech.shoppinglist.data.receipt

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(receipt : DbReceipt) : Long

    @Update
    fun update(receipt: DbReceipt)

    @Query("DELETE FROM receipts WHERE id = :key AND createdBy = :createdBy")
    fun delete(key : Long, createdBy : Long)

    @Query("DELETE FROM receipts")
    fun reset()

    @Query("SELECT * FROM receipts WHERE id = :key AND createdBy = :createdBy")
    fun get(key: Long, createdBy: Long) : DbReceipt?

    @Query("SELECT * FROM receipts WHERE id = :key AND createdBy = :createdBy")
    fun getLive(key: Long, createdBy: Long) : LiveData<DbReceipt>

    @Query("SELECT * FROM receipts WHERE id = :key AND createdBy = :createdBy")
    fun getFlow(key: Long, createdBy: Long) : Flow<DbReceipt>

    @Query("SELECT * FROM receipts")
    fun getAllLive() : LiveData<List<DbReceipt>>

}