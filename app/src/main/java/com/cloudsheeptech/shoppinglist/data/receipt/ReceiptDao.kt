package com.cloudsheeptech.shoppinglist.data.receipt

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

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

    

}