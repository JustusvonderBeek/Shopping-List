package com.cloudsheeptech.shoppinglist.data

import androidx.room.Entity
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.items.ItemWire
import com.cloudsheeptech.shoppinglist.data.list.ApiShoppingList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime

@Entity(tableName = "list_table", primaryKeys = ["ID", "CreatedByID"])
data class DbShoppingList(
    var listId : Long,
    var title : String,
    // Flatten list creator to allow direct ID access
    var createdBy : Long,
    // FIXME: This column is deprecated and should be removed / updated to createdAt
    var createdByName : String,
    var lastUpdated : OffsetDateTime
) {

    // TODO: Move into the repository function
    suspend fun toShoppingListWire(database: ShoppingListDatabase): ApiShoppingList {
        var userId = 0L
        val listId = this.listId
        val createdBy = this.createdBy
        val items = mutableListOf<ItemWire>()
        withContext(Dispatchers.IO) {
//            if (AppUserLocalDataSource.isAuthenticatedOnline()) {
//                AppUserLocalDataSource.PostUserOnlineAsync(null)
//                if (AppUserLocalDataSource.isAuthenticatedOnline()) {
//                    Log.i("ShoppingListHandler", "Failed to create user online: Cannot complete request")
//                    return@withContext
//                }
//            }
//            val appUser = AppUserLocalDataSource.getUser()!!
//            userId = appUser.OnlineID
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
//        val appUser = AppUserLocalDataSource.getUser()
        val wireList = ApiShoppingList(
            listId = this.listId,
            title = this.title,
            createdBy = ListCreator(userId, "Unknown"),
            createdAt = this.lastUpdated,
            lastUpdated = this.lastUpdated,
            items = items,
        )
        return wireList
    }

}