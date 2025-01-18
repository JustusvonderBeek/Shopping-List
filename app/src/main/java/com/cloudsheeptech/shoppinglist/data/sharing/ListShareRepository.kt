package com.cloudsheeptech.shoppinglist.data.sharing

import androidx.lifecycle.LiveData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListShareRepository @Inject constructor(
    private val localDataSource: ListShareLocalDataSource,
    private val remoteDataSource: ListShareRemoteDataSource
) {

    suspend fun create(listId: Long, createdBy: Long, sharedWith: Long) {
        localDataSource.create(listId, createdBy, sharedWith)
        remoteDataSource.create(listId, createdBy, sharedWith)
    }

    suspend fun read(listId: Long): List<Long> {
        return localDataSource.read(listId)
    }

    fun readLive(listId: Long, createdBy: Long): LiveData<List<ListShareDatabase>> {
        return localDataSource.readLive(listId, createdBy)
    }

    suspend fun update(listId: Long, createdBy: Long, sharedWith: List<Long>) {
        localDataSource.update(listId, createdBy, sharedWith)
        remoteDataSource.update(listId, sharedWith)
    }

    suspend fun delete(listId: Long, createdBy: Long, sharedWith: Long) {
        localDataSource.delete(listId, createdBy, sharedWith)
        remoteDataSource.delete(listId, createdBy, sharedWith)
    }

    suspend fun deleteAll(listId: Long, createdBy: Long) {
        localDataSource.deleteAll(listId, createdBy)
        remoteDataSource.deleteAll(listId)
    }

}