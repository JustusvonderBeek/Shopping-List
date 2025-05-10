package com.cloudsheeptech.shoppinglist.data.onlineUser

import android.util.Log
import androidx.lifecycle.LiveData
import com.cloudsheeptech.shoppinglist.exception.UserAuthenticationFailedException
import com.cloudsheeptech.shoppinglist.exception.UserNotAuthenticatedException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineUserRepository
    @Inject
    constructor(
        private val localDataSource: OnlineUserLocalDataSource,
        private val remoteDataSource: OnlineUserRemoteDataSource,
    ) {
        private val LOG_TAG = "OnlineUserRepository"

        suspend fun create(user: ListCreator) {
            localDataSource.create(user)
        }

        suspend fun read(userId: Long): ListCreator? = localDataSource.read(userId)

        suspend fun readOnline(username: String): List<ListCreator> {
            try {
                val onlineUsers = remoteDataSource.read(username)
                for (onlineUser in onlineUsers) {
                    if (localDataSource.read(onlineUser.onlineId) == null) {
                        localDataSource.create(onlineUser)
                    }
                }
                return onlineUsers
            } catch (e: UserAuthenticationFailedException) {
                Log.e(LOG_TAG, "User not authenticated")
            } catch (e: UserNotAuthenticatedException) {
                Log.e(LOG_TAG, "User not authenticated")
            }
            return emptyList()
        }

        fun readAllLive(): LiveData<List<ListCreator>> = localDataSource.readAllLive()

        suspend fun update(updatedUser: ListCreator) {
            localDataSource.update(updatedUser)
        }

        suspend fun delete(userId: Long) {
            localDataSource.delete(userId)
        }
    }
