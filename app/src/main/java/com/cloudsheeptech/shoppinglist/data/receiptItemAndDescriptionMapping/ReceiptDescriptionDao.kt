package com.cloudsheeptech.shoppinglist.data.receiptItemAndDescriptionMapping

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDescriptionDao {

    @Insert
    fun insert(description: ReceiptDescriptionMapping)

    @Update
    fun update(description: ReceiptDescriptionMapping)

    @Query("SELECT EXISTS(SELECT * FROM receipt_to_description WHERE receiptId = :receiptId AND createdBy = :createdBy AND descriptionOrder = :order)")
    fun exists(receiptId: Long, createdBy: Long, order: Int) : Boolean

    @Query("SELECT * FROM receipt_to_description WHERE receiptId = :receiptId AND createdBy = :createdBy")
    fun read(receiptId: Long, createdBy: Long) :  List<ReceiptDescriptionMapping>

    @Query("SELECT * FROM receipt_to_description WHERE receiptId = :receiptId AND createdBy = :createdBy AND descriptionOrder = :order")
    fun read(receiptId: Long, createdBy: Long, order: Int) : ReceiptDescriptionMapping?

    @Query("SELECT * FROM receipt_to_description WHERE receiptId = :receiptId AND createdBy = :createdBy")
    fun readFlow(receiptId: Long, createdBy: Long) : Flow<List<ReceiptDescriptionMapping>>

    @Delete
    fun delete(description: ReceiptDescriptionMapping)

    @Query("DELETE FROM receipt_to_description WHERE receiptId = :receiptId AND createdBy = :createdBy")
    fun deleteAllForReceipt(receiptId: Long, createdBy: Long)

    @Query("DELETE FROM receipt_to_description WHERE receiptId = :receiptId AND createdBy = :createdBy AND descriptionOrder = :order")
    fun delete(receiptId: Long, createdBy: Long, order: Int)
}
