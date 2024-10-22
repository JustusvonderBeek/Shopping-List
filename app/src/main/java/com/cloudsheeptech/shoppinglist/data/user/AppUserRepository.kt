package com.cloudsheeptech.shoppinglist.data.user

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
        local: AppUserLocalDataSource,
        remote: AppUserRemoteDataSource,
    ) {
        private val job = Job()
        private val vmCoroutine = CoroutineScope(Dispatchers.Main + job)

        private val appUserLocalSource: AppUserLocalDataSource = local
        private val appUserRemoteSource: AppUserRemoteDataSource = remote

        init {
            vmCoroutine.launch {
                appUserLocalSource.read()
                createOnline()
                val user = appUserLocalSource.getUser() ?: return@launch
                remote.registerCreateUser(user)
            }
        }

        // Creating the user information both offline and online
        suspend fun create(username: String) {
            appUserLocalSource.create(username)
            appUserLocalSource.store()
            val user = appUserLocalSource.getUser()
            if (user != null) {
                val onlineUser = appUserRemoteSource.create(user) ?: return
                appUserLocalSource.setOnlineId(onlineUser.OnlineID)
                appUserLocalSource.store()
            }
        }

        suspend fun createOnline() {
            val localUser = appUserLocalSource.getUser() ?: return
            if (localUser.OnlineID != 0L) {
                return
            }
            val onlineUser = appUserRemoteSource.create(localUser) ?: return
            appUserLocalSource.setOnlineId(onlineUser.OnlineID)
            appUserLocalSource.store()
        }

        // Should only provide the local user, since the online
        // user doesn't provide any different information than what is stored
        // locally
        fun read(): AppUser? = appUserLocalSource.getUser()

        fun readLive(): LiveData<AppUser> = appUserLocalSource.getUserLive()

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
            appUserRemoteSource.delete(localUser)
            appUserLocalSource.delete()
        }
    }
