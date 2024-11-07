package com.cloudsheeptech.shoppinglist.data.user

import android.util.Log
import androidx.lifecycle.LiveData
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
        // No need to perform this action here
    }

    // Should only provide the local user, since the online
    // user doesn't provide any different information than what is stored
    // locally
    fun read(): AppUser? = appUserLocalSource.getUser()

    fun readLive(): LiveData<AppUser> = appUserLocalSource.getUserLive()

    fun loaded(): Boolean = appUserLocalSource.loaded()

    // Update the information offline and online
    suspend fun update(user: AppUser) {
        // Currently, we don't want to update the other
        // parameters of the user
        appUserLocalSource.setUsername(user.Username)
        appUserLocalSource.store()
        appUserRemoteSource.update(user)
    }

    // Delete the user offline and online
    suspend fun delete() {
        val localUser = appUserLocalSource.getUser() ?: return
        // TODO: What happens if we cannot remove the user online?
        // Think about this problem later
        try {
            appUserRemoteSource.delete(localUser)
        } catch (ex: Exception) {
            Log.w("AppUserRepository", "User not deleted online")
        }
        appUserLocalSource.delete()
    }
}
