package com.cloudsheeptech.shoppinglist.data.list.util

import com.cloudsheeptech.shoppinglist.data.list.ShoppingList
import com.cloudsheeptech.shoppinglist.data.user.AppUser
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import javax.inject.Inject

class ShoppingListUtil
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
