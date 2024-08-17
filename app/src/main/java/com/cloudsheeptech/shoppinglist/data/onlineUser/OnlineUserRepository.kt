package com.cloudsheeptech.shoppinglist.data.onlineUser

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineUserRepository @Inject constructor(
    private val localDataSource: OnlineUserLocalDataSource,
    private val remoteDataSource: OnlineUserRemoteDataSource,
) {

    suspend fun create(user: ListCreator) {
        localDataSource.create(user)
    }

    suspend fun read(userId: Long) : ListCreator? {
        return localDataSource.read(userId)
    }

    suspend fun readOnline(username: String) : List<ListCreator> {
        return remoteDataSource.read(username)
    }

    suspend fun update(updatedUser: ListCreator) {
        localDataSource.update(updatedUser)
    }

    suspend fun delete(userId: Long) {
        localDataSource.delete(userId)
    }

}