package com.cloudsheeptech.shoppinglist.user.util

import com.cloudsheeptech.shoppinglist.user.model.ApiUser
import com.cloudsheeptech.shoppinglist.user.model.AppUser
import com.cloudsheeptech.shoppinglist.user.model.UserRightsEnum
import java.time.OffsetDateTime
import javax.inject.Singleton

@Singleton
class UserFormatAdapter {
    companion object {
        private fun AppUser.toApiUser(): ApiUser =
            ApiUser(
                this.OnlineID,
                this.Username,
                this.Password,
                UserRightsEnum.USER.value,
                this.Created,
                OffsetDateTime.now(),
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
