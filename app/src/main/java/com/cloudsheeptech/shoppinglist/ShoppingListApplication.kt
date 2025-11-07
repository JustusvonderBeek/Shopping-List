
package com.cloudsheeptech.shoppinglist

import android.app.Application
import com.cloudsheeptech.shoppinglist.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.list.api.interceptor.AuthInterceptor
import com.cloudsheeptech.shoppinglist.list.api.interceptor.CreateUserInterceptor
import com.cloudsheeptech.shoppinglist.list.repo.ItemLocalDataSource
import com.cloudsheeptech.shoppinglist.list.repo.ShoppingListLocalDataSource
import com.cloudsheeptech.shoppinglist.list.repo.ShoppingListRemoteDataSource
import com.cloudsheeptech.shoppinglist.list.repo.ShoppingListRepository
import com.cloudsheeptech.shoppinglist.network.IUserCreationDataProvider
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.network.token.ShoppingListAuthenticationTokenProvider
import com.cloudsheeptech.shoppinglist.recipe.util.BinaryFileHandler
import com.cloudsheeptech.shoppinglist.recipe.util.CompressionHandler
import com.cloudsheeptech.shoppinglist.sharing.repo.ListShareLocalDataSource
import com.cloudsheeptech.shoppinglist.sharing.repo.ListShareRemoteDataSource
import com.cloudsheeptech.shoppinglist.sharing.repo.ListShareRepository
import com.cloudsheeptech.shoppinglist.sharing.repo.OnlineUserLocalDataSource
import com.cloudsheeptech.shoppinglist.sharing.repo.OnlineUserRemoteDataSource
import com.cloudsheeptech.shoppinglist.sharing.repo.OnlineUserRepository
import com.cloudsheeptech.shoppinglist.user.api.UserAuthenticatedApi
import com.cloudsheeptech.shoppinglist.user.api.UserUnauthenticatedApi
import com.cloudsheeptech.shoppinglist.user.repo.AppUserLocalDataSource
import com.cloudsheeptech.shoppinglist.user.repo.AppUserRemoteDataSource
import com.cloudsheeptech.shoppinglist.user.repo.AppUserRepository
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
    @Inject
    lateinit var appUserLocalDataSource: AppUserLocalDataSource

    @Inject
    lateinit var appUserRemoteDataSource: AppUserRemoteDataSource

    @Inject
    lateinit var appUserRepository: AppUserRepository

    @Inject
    lateinit var database: ShoppingListDatabase

    @Inject
    lateinit var networking: Networking

    @Inject
    lateinit var userUnauthenticatedApi: UserUnauthenticatedApi

    @Inject
    lateinit var authInterceptor: AuthInterceptor

    @Inject
    lateinit var createUserInterceptor: CreateUserInterceptor

    @Inject
    lateinit var userAuthenticatedApi: UserAuthenticatedApi

    @Inject
    lateinit var shoppingListLocalDataSource: ShoppingListLocalDataSource

    @Inject
    lateinit var shoppingListRemoteDataSource: ShoppingListRemoteDataSource

    @Inject
    lateinit var shoppingListRepository: ShoppingListRepository

    @Inject
    lateinit var itemLocalDataSource: ItemLocalDataSource

    @Inject
    lateinit var onlineUserLocalDataSource: OnlineUserLocalDataSource

    @Inject
    lateinit var onlineUserRemoteDataSource: OnlineUserRemoteDataSource

    @Inject
    lateinit var onlineUserRepository: OnlineUserRepository

    @Inject
    lateinit var listShareLocalDataSource: ListShareLocalDataSource

    @Inject
    lateinit var listShareRemoteDataSource: ListShareRemoteDataSource

    @Inject
    lateinit var listShareRepository: ListShareRepository

    @Inject
    lateinit var userCreationPayloadProvider: IUserCreationDataProvider

    @Inject
    lateinit var tokenProvider: ShoppingListAuthenticationTokenProvider

    @Inject
    lateinit var binaryFileHandler: BinaryFileHandler

    @Inject
    lateinit var compressionHandler: CompressionHandler

    // ------ Testing Utilities -------

    fun isDatabaseInitialized() = ::database.isInitialized

    fun isAppUserLocalDSInitialized() = ::appUserLocalDataSource.isInitialized

    fun isAppUserRemoteDSInitialized() = ::appUserRemoteDataSource.isInitialized

    fun isAppUserRepositoryInitialized() = ::appUserRepository.isInitialized

    fun isNetworkingInitialized() = ::networking.isInitialized

    fun isAuthInterceptorInitialized() = ::authInterceptor.isInitialized

    fun isCreateUserInterceptorInitialized() = ::createUserInterceptor.isInitialized

    fun isUserUnauthenticatedApiInitialized() = ::userUnauthenticatedApi.isInitialized

    fun isUserAuthenticatedApiInitialized() = ::userAuthenticatedApi.isInitialized

    fun isItemLocalDSInitialized() = ::itemLocalDataSource.isInitialized

    fun isShoppingListLocalDSInitialized() = ::shoppingListLocalDataSource.isInitialized

    fun isShoppingListRemoteDSInitialized() = ::shoppingListRemoteDataSource.isInitialized

    fun isShoppingListRepositoryInitialized() = ::shoppingListRepository.isInitialized

    fun isBinaryFileHandlerInitialized() = ::binaryFileHandler.isInitialized

    fun isOnlineUserLocalDSInitialized() = ::onlineUserLocalDataSource.isInitialized

    fun isOnlineUserRemoteDSInitialized() = ::onlineUserRemoteDataSource.isInitialized

    fun isOnlineUserRepositoryInitialized() = ::onlineUserRepository.isInitialized
}
