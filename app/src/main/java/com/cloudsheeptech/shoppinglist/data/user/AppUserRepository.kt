package com.cloudsheeptech.shoppinglist.data.user

import android.content.Context

/*
* This class combines the offline and offline handling of the
* user information and provides a unified way of creating,
* reading, updating and deleting the user information.
 */
class AppUserRepository(local: AppUserLocalDataSource, remote: AppUserRemoteDataSource) {

    private lateinit var appUserLocalSource : AppUserLocalDataSource
    private lateinit var appUserRemoteSource: AppUserRemoteDataSource

    // Creating the user information both offline and online
    suspend fun create(username: String) {
        appUserLocalSource.create(username)
        appUserLocalSource.store()
        val user = appUserLocalSource.getUser()
        if (user != null) {
            val onlineUser = appUserRemoteSource.create(user) ?: return
            appUserLocalSource.resetOnlineId(onlineUser.OnlineID)
            appUserLocalSource.store()
        }
    }

    // Should only provide the local user, since the online
    // user doesn't provide anymore information than what is stored
    // locally
    fun read() : AppUser? {
        return appUserLocalSource.getUser()
    }

    // Update the information offline and online
    suspend fun update(user: AppUser) {
        // Currently, we don't want to update the other
        // parameters of the user
        appUserLocalSource.resetUsername(user.Username)
        appUserLocalSource.store()
        appUserRemoteSource.update(user)
    }

    // Delete the user offline and online
    suspend fun delete() {
        val localUser = appUserLocalSource.getUser() ?: return
        // TODO: What happens if we cannot remove the user online?
        // Think about this problem later
        appUserRemoteSource.delete(localUser)
        appUserLocalSource.delete()
    }
}