package com.cloudsheeptech.shoppinglist.data.onlineUser

import android.util.Log
import com.cloudsheeptech.shoppinglist.exception.UserAuthenticationFailedException
import com.cloudsheeptech.shoppinglist.exception.UserNotAuthenticatedException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineUserRepository @Inject constructor(
    private val localDataSource: OnlineUserLocalDataSource,
    private val remoteDataSource: OnlineUserRemoteDataSource,
) {

    private val LOG_TAG = "OnlineUserRepository"

    suspend fun create(user: ListCreator) {
        localDataSource.create(user)
    }

    suspend fun read(userId: Long): ListCreator? {
        return localDataSource.read(userId)
    }

    suspend fun readOnline(username: String): List<ListCreator> {
        try {
            return remoteDataSource.read(username)
        } catch (e: UserAuthenticationFailedException) {
            Log.e(LOG_TAG, "User not authenticated")
        } catch (e: UserNotAuthenticatedException) {
            Log.e(LOG_TAG, "User not authenticated")
        }
        return emptyList()
    }

    suspend fun update(updatedUser: ListCreator) {
        localDataSource.update(updatedUser)
    }

    suspend fun delete(userId: Long) {
        localDataSource.delete(userId)
    }

}