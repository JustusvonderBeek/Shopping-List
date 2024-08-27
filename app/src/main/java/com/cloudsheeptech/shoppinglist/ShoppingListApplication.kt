package com.cloudsheeptech.shoppinglist

import android.app.Application
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ItemToListLocalDataSource
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ItemToListRepository
import com.cloudsheeptech.shoppinglist.data.items.ItemLocalDataSource
import com.cloudsheeptech.shoppinglist.data.items.ItemRepository
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListLocalDataSource
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListRemoteDataSource
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListRepository
import com.cloudsheeptech.shoppinglist.data.onlineUser.OnlineUserLocalDataSource
import com.cloudsheeptech.shoppinglist.data.onlineUser.OnlineUserRemoteDataSource
import com.cloudsheeptech.shoppinglist.data.onlineUser.OnlineUserRepository
import com.cloudsheeptech.shoppinglist.data.sharing.ListShareLocalDataSource
import com.cloudsheeptech.shoppinglist.data.sharing.ListShareRemoteDataSource
import com.cloudsheeptech.shoppinglist.data.sharing.ListShareRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUserLocalDataSource
import com.cloudsheeptech.shoppinglist.data.user.AppUserRemoteDataSource
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.network.Networking
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * This class is used to create and inject dependencies throughout the application
 * so that no more manual labor is necessary to handle dependencies.
 * More information: https://developer.android.com/training/dependency-injection/hilt-android
 */
@HiltAndroidApp
class ShoppingListApplication : Application() {

    // For these injections to work, the constructor needs an @Inject as well
    @Inject lateinit var appUserLocalDataSource: AppUserLocalDataSource
    @Inject lateinit var appUserRemoteDataSource: AppUserRemoteDataSource
    @Inject lateinit var appUserRepository: AppUserRepository

    @Inject lateinit var database: ShoppingListDatabase
    @Inject lateinit var networking: Networking
    @Inject lateinit var shoppingListLocalDataSource: ShoppingListLocalDataSource
    @Inject lateinit var shoppingListRemoteDataSource: ShoppingListRemoteDataSource
    @Inject lateinit var shoppingListRepository: ShoppingListRepository

    @Inject lateinit var itemLocalDataSource: ItemLocalDataSource
    @Inject lateinit var itemRepository: ItemRepository

    @Inject lateinit var itemToListLocalDataSource: ItemToListLocalDataSource
    @Inject lateinit var itemToListRepository: ItemToListRepository

    @Inject lateinit var onlineUserLocalDataSource: OnlineUserLocalDataSource
    @Inject lateinit var onlineUserRemoteDataSource: OnlineUserRemoteDataSource
    @Inject lateinit var onlineUserRepository: OnlineUserRepository

    @Inject lateinit var listShareLocalDataSource: ListShareLocalDataSource
    @Inject lateinit var listShareRemoteDataSource: ListShareRemoteDataSource
    @Inject lateinit var listShareRepository: ListShareRepository
}