package com.cloudsheeptech.shoppinglist.sharing.repo

import androidx.lifecycle.LiveData
import com.cloudsheeptech.shoppinglist.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.list.model.ListCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineUserLocalDataSource
    @Inject
    constructor(
        private val database: ShoppingListDatabase,
    ) {
        private val onlineUserDao = database.onlineUserDao()

        suspend fun create(user: ListCreator) {
            withContext(Dispatchers.IO) {
                onlineUserDao.insertUser(user)
            }
        }

        /**
         * Reads the locally stored data for the user with given ID
         * @throws IllegalArgumentException if the user is not found
         * @return the user if found
         */
        suspend fun read(userId: Long): ListCreator? {
            val creator: ListCreator?
            withContext(Dispatchers.IO) {
                creator = onlineUserDao.getUser(userId)
            }
            return creator
        }

        fun readAllLive(): LiveData<List<ListCreator>> = onlineUserDao.getAllOnlineUsersLive()

        suspend fun update(updatedUser: ListCreator) {
            withContext(Dispatchers.IO) {
                val user = onlineUserDao.getUser(updatedUser.onlineId) ?: throw IllegalArgumentException("user not found")
                onlineUserDao.updateUser(updatedUser)
            }
        }

        suspend fun delete(userId: Long) {
            withContext(Dispatchers.IO) {
                onlineUserDao.deleteUser(userId)
            }
        }
    }
