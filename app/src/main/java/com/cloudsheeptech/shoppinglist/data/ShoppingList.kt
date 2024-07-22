package com.cloudsheeptech.shoppinglist.data

import android.util.Log
import androidx.room.Entity
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.user.AppUserHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime

@Entity(tableName = "list_table", primaryKeys = ["ID", "CreatedByID"])
data class ShoppingList(
    var ID : Long,
    var Name : String,
    // Flatten list creator to allow direct ID access
    var CreatedByID : Long,
    var CreatedByName : String,
    var LastEdited : OffsetDateTime
) {

    suspend fun toShoppingListWire(database: ShoppingListDatabase): ShoppingListWire {
        var userId = 0L
        val listId = this.ID
        val createdBy = this.CreatedByID
        val items = mutableListOf<ItemWire>()
        withContext(Dispatchers.IO) {
            if (AppUserHandler.isAuthenticatedOnline()) {
                AppUserHandler.PostUserOnlineAsync(null)
                if (AppUserHandler.isAuthenticatedOnline()) {
                    Log.i("ShoppingListHandler", "Failed to create user online: Cannot complete request")
                    return@withContext
                }
            }
            val appUser = AppUserHandler.getUser()!!
            userId = appUser.OnlineID
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
        val appUser = AppUserHandler.getUser()
        val wireList = ShoppingListWire(
            ListId = this.ID,
            Name = this.Name,
            CreatedBy = ListCreator(userId, appUser?.Username ?: "Unknown"),
            Created = this.LastEdited,
            LastEdited = this.LastEdited,
            Items = items,
        )
        return wireList
    }

}