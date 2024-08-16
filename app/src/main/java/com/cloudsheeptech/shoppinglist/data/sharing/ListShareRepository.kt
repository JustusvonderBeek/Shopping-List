package com.cloudsheeptech.shoppinglist.data.sharing

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListShareRepository @Inject constructor(private val localDataSource: ListShareLocalDataSource, private val remoteDataSource: ListShareRemoteDataSource) {

    suspend fun create(listId: Long, sharedWith: Long) {
        localDataSource.create(listId, sharedWith)
        remoteDataSource.create(listId, sharedWith)
    }

    suspend fun read(listId: Long) : List<Long> {
        return localDataSource.read(listId)
    }

    suspend fun update(listId: Long, sharedWith: List<Long>) {
        localDataSource.update(listId, sharedWith)
        remoteDataSource.update(listId, sharedWith)
    }

    suspend fun delete(listId: Long) {
        localDataSource.delete(listId)
        remoteDataSource.delete(listId)
    }

}