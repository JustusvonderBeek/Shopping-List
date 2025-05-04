package com.cloudsheeptech.shoppinglist.data.sharing

import androidx.lifecycle.LiveData
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListShareLocalDataSource
    @Inject
    constructor(
        database: ShoppingListDatabase,
        private val userRepo: AppUserRepository,
        private val listRepository: ShoppingListRepository,
    ) {
        private val shareDao = database.sharedDao()

        /**
         * Creating a new sharing for a single user in the database
         * @throws IllegalStateException if the app is not correctly initialized
         * @throws IllegalArgumentException if the given list does not exist
         */
        suspend fun create(
            listId: Long,
            createdBy: Long,
            sharedWith: Long,
        ) {
            withContext(Dispatchers.IO) {
                val user = userRepo.read() ?: throw IllegalStateException("user not initialized")
                val listExists = listRepository.exist(listId, user.OnlineID)
                if (!listExists) {
                    throw IllegalArgumentException("list does not exist")
                }
                val newShare = ListShareDatabase(listId, createdBy, sharedWith)
                shareDao.insertShared(newShare)
            }
        }

        suspend fun read(listId: Long): List<Long> {
            val sharedWith = mutableListOf<Long>()
            withContext(Dispatchers.IO) {
                val dbSharedWith = shareDao.getListSharedWith(listId)
                dbSharedWith.forEach { share -> sharedWith.add(share.SharedWith) }
            }
            return sharedWith
        }

        fun readLive(
            listId: Long,
            createdBy: Long,
        ): LiveData<List<ListShareDatabase>> = shareDao.getListSharedWithLive(listId, createdBy)

        fun readPreviewLive(
            listId: Long,
            createdBy: Long,
        ): LiveData<List<ShareUserPreview>> = shareDao.getListPreviewSharedWith(listId, createdBy)

        suspend fun update(
            listId: Long,
            createdBy: Long,
            sharedWith: List<Long>,
        ) {
            withContext(Dispatchers.IO) {
                shareDao.deleteAllFromList(listId, createdBy)
                sharedWith.forEach { share -> create(listId, createdBy, share) }
            }
        }

        suspend fun delete(
            listId: Long,
            createdBy: Long,
            sharedWith: Long,
        ) {
            withContext(Dispatchers.IO) {
                shareDao.delete(listId, createdBy, sharedWith)
            }
        }

        suspend fun deleteAll(
            listId: Long,
            createdBy: Long,
        ) {
            withContext(Dispatchers.IO) {
                shareDao.deleteAllFromList(listId, createdBy)
            }
        }
    }
