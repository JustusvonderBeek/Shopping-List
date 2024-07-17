package com.cloudsheeptech.shoppinglist.data

import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.user.AppUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Entity(tableName = "list_table", primaryKeys = ["ID", "CreatedBy"])
data class ShoppingList(
    var ID : Long,
    var Name : String,
    // Flatten list creator to allow direct ID access
    var CreatedBy : Long,
    var CreatedByName : String,
    var LastEdited : OffsetDateTime
) {

    suspend fun toShoppingListWire(database: ShoppingListDatabase): ShoppingListWire {
        var userId = 0L
        val listId = this.ID
        val createdBy = this.CreatedBy
        val items = mutableListOf<ItemWire>()
        withContext(Dispatchers.IO) {
            if (AppUser.UserId == 0L) {
                AppUser.PostUserOnlineAsync(null)
                if (AppUser.UserId == 0L) {
                    Log.i("ShoppingListHandler", "Failed to create user online: Cannot complete request")
                    return@withContext
                }
            }
            userId = AppUser.UserId
            val itemsMapped = database.mappingDao().getMappingsForList(listId, createdBy)
            for (item in itemsMapped) {
                val convertedItem = ItemWire(Name="", Icon="", Quantity = 1L, Checked = false, AddedBy = item.AddedBy)
                val databaseItem = database.itemDao().getItem(item.ItemID) ?: continue
                convertedItem.Name = databaseItem.Name
                convertedItem.Icon = databaseItem.Icon
                convertedItem.Quantity = item.Quantity
                convertedItem.Checked = item.Checked
                convertedItem.AddedBy = item.AddedBy
                items.add(convertedItem)
            }
        }
        val wireList = ShoppingListWire(
            ListId = this.ID,
            Name = this.Name,
            CreatedBy = ListCreator(userId, AppUser.Username),
            Created = this.LastEdited,
            LastEdited = this.LastEdited,
            Items = items,
        )
        return wireList
    }

}