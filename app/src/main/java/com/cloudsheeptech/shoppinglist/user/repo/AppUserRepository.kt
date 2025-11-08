package com.cloudsheeptech.shoppinglist.user.repo

import android.util.Log
import androidx.lifecycle.LiveData
import com.cloudsheeptech.shoppinglist.user.model.AppUser
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/*
* This class combines the offline and offline handling of the
* user information and provides a unified way of creating,
* reading, updating and deleting the user information.
 */
@Singleton
class AppUserRepository
    @Inject
    constructor(
        private val appUserLocalSource: AppUserLocalDataSource,
        private val appUserRemoteSource: AppUserRemoteDataSource,
    ) {
        // Creating the user information both offline and online
        suspend fun create(username: String) {
            appUserLocalSource.create(username)
            appUserLocalSource.store()
            // Online user creation can take place anytime
            // and is therefore explicitly build into the
            // networking modules
        }

        // Should only provide the local user, since the online
        // user doesn't provide any different information than what is stored
        // locally
        fun read(): AppUser? = appUserLocalSource.getUser()

        fun readLive(): LiveData<AppUser> = appUserLocalSource.getUserLive()

        fun readLiveFlow(): Flow<AppUser> = appUserLocalSource.getUserFlow()

        fun loaded(): Boolean = appUserLocalSource.loaded()

        suspend fun update(user: AppUser) {
            try {
                // Currently, we don't want to update the other
                // parameters of the user
                appUserLocalSource.setUsername(user.Username)
                appUserLocalSource.store()
                appUserRemoteSource.update(user)
            } catch (ex: Exception) {
                Log.e("AppUserRepository", "Failed to update user information: $ex")
            }
        }

        suspend fun updateOnlineId(onlineId: Long) {
            try {
                appUserLocalSource.setOnlineId(onlineId)
                appUserLocalSource.store()
                Log.i("AppUserRepository", "Updated onlineId to $onlineId and stored user")
            } catch (ex: Exception) {
                Log.e("AppUserRepository", "Failed to update onlineId to $onlineId: $ex")
            }
        }

        suspend fun delete() {
            val localUser = appUserLocalSource.getUser()
            if (localUser == null) {
                Log.w("AppUserRepository", "Currently stored user is null, skipping delete")
                return
            }
            var success = false
            try {
                success = appUserRemoteSource.delete(localUser)
            } catch (ex: Exception) {
                Log.w("AppUserRepository", "User not deleted online: $ex")
            } finally {
                if (!success) {
                    Log.e("AppUserRepository", "User not deleted online, continue anyway")
                }
                appUserLocalSource.delete()
            }
        }
    }
