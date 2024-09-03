package com.cloudsheeptech.shoppinglist.data.receiptItemAndDescriptionMapping

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptItemDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(itemMapping: ReceiptItemMapping)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(itemMapping: ReceiptItemMapping)

    @Query("SELECT * FROM receipt_to_item WHERE id = :mappingId")
    fun read(mappingId : Long) : ReceiptItemMapping?

    @Query("SELECT * FROM  receipt_to_item WHERE receiptId = :receiptId AND createdBy = :createdBy")
    fun readAllForReceipt(receiptId: Long, createdBy: Long) : List<ReceiptItemMapping>

    @Query("SELECT * FROM receipt_to_item JOIN items ON itemId = items.id")
    fun readJoined() : List<ReceiptItemMapping>

    @Query("SELECT * FROM receipt_to_item WHERE receiptId = :receiptId AND createdBy = :createdBy")
    fun readFlow(receiptId: Long, createdBy: Long) : Flow<List<ReceiptItemMapping>>

    @Delete
    fun delete(itemMapping: ReceiptItemMapping)

    @Query("DELETE FROM receipt_to_item WHERE receiptId = :receiptId AND createdBy = :createdBy")
    fun deleteAllForReceipt(receiptId: Long, createdBy: Long)

}