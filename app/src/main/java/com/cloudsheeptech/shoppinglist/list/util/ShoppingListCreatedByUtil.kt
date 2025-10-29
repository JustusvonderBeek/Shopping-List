package com.cloudsheeptech.shoppinglist.list.util

import com.cloudsheeptech.shoppinglist.list.model.ShoppingList
import com.cloudsheeptech.shoppinglist.user.model.AppUser
import com.cloudsheeptech.shoppinglist.user.repo.AppUserRepository
import javax.inject.Inject

class ShoppingListCreatedByUtil
    @Inject
    constructor(
        private val userRepository: AppUserRepository,
    ) {
        fun updateListToCurrentUser(list: ShoppingList) {
            val user = userRepository.read() ?: throw IllegalStateException("user null after login screen")
            updateListCreatedBy(list, user)
        }

        companion object {
            fun updateListCreatedBy(
                list: ShoppingList,
                user: AppUser,
            ) {
                list.createdBy.onlineId = user.OnlineID
                list.createdBy.username = user.Username
                list.items.map { item -> item.addedBy = user.OnlineID }
            }
        }
    }
