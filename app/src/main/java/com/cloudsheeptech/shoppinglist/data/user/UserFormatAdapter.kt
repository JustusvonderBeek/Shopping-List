package com.cloudsheeptech.shoppinglist.data.user

import javax.inject.Singleton

@Singleton
class UserFormatAdapter {
    companion object {
        private fun AppUser.toApiUser(): ApiUser =
            ApiUser(
                this.OnlineID,
                this.Username,
                this.Password,
                this.Created,
                null,
            )

        private fun ApiUser.toAppUser(): AppUser =
            AppUser(
                1,
                this.onlineId,
                this.username,
                this.password ?: "",
                this.created,
            )

        fun fromAppToApiUser(appUser: AppUser): ApiUser = appUser.toApiUser()

        fun fromApiToAppUser(apiUser: ApiUser): AppUser = apiUser.toAppUser()
    }
}
