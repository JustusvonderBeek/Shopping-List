package com.cloudsheeptech.shoppinglist.data.sharing

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.lifecycle.LiveData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListShareRepository
    @Inject
    constructor(
        private val localDataSource: ListShareLocalDataSource,
        private val remoteDataSource: ListShareRemoteDataSource,
    ) {
        suspend fun create(
            listId: Long,
            createdBy: Long,
            sharedWith: Long,
        ): Boolean {
            try {
                val success = remoteDataSource.create(listId, createdBy, sharedWith)
                if (!success) {
                    Log.e("ListShareRepository", "Failed to create sharing $listId online")
                    return false
                }

                localDataSource.create(listId, createdBy, sharedWith)
                return true
            } catch (ex: SQLiteConstraintException) {
                Log.e("ListShareRepository", "List already shared: $ex")
            } catch (ex: Exception) {
                Log.e("ListShareRepository", "Unknown error while sharing list: $ex")
            }
            return false
        }

        suspend fun read(
            listId: Long,
            createdBy: Long,
        ): List<Long> = localDataSource.read(listId, createdBy)

        fun readLive(
            listId: Long,
            createdBy: Long,
        ): LiveData<List<ShareUserPreview>> = localDataSource.readPreviewLive(listId, createdBy)

        suspend fun update(
            listId: Long,
            createdBy: Long,
            sharedWith: List<Long>,
        ) {
            localDataSource.update(listId, createdBy, sharedWith)
            remoteDataSource.update(listId, sharedWith)
        }

        suspend fun delete(
            listId: Long,
            createdBy: Long,
            sharedWith: Long,
        ) {
            val success = remoteDataSource.delete(listId, createdBy, sharedWith)
            if (!success) {
                Log.e("ListShareRepository", "Unsharing $listId with $sharedWith online failed")
                return
            }
            localDataSource.delete(listId, createdBy, sharedWith)
            Log.d("ListShareRepository", "Unshared $listId with $sharedWith")
        }

        suspend fun deleteAll(
            listId: Long,
            createdBy: Long,
        ) {
            localDataSource.deleteAll(listId, createdBy)
            remoteDataSource.deleteAll(listId)
        }
    }
