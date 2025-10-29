package com.cloudsheeptech.shoppinglist.testUtil

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.cloudsheeptech.shoppinglist.ShoppingListApplication
import com.cloudsheeptech.shoppinglist.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.list.repo.ItemLocalDataSource
import com.cloudsheeptech.shoppinglist.list.repo.ShoppingListLocalDataSource
import com.cloudsheeptech.shoppinglist.list.repo.ShoppingListRemoteDataSource
import com.cloudsheeptech.shoppinglist.list.repo.ShoppingListRepository
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.network.token.ShoppingListAuthenticationTokenProvider
import com.cloudsheeptech.shoppinglist.sharing.repo.OnlineUserLocalDataSource
import com.cloudsheeptech.shoppinglist.sharing.repo.OnlineUserRemoteDataSource
import com.cloudsheeptech.shoppinglist.sharing.repo.OnlineUserRepository
import com.cloudsheeptech.shoppinglist.user.repo.AppUserLocalDataSource
import com.cloudsheeptech.shoppinglist.user.repo.AppUserRemoteDataSource
import com.cloudsheeptech.shoppinglist.user.repo.AppUserRepository
import com.cloudsheeptech.shoppinglist.user.util.UserCreationDataProvider

object TestUtil {
    var shoppingListApplication: ShoppingListApplication = ShoppingListApplication()

    fun initialize(clearDatabase: Boolean = true) {
        createDatabase(clearDatabase)
        createLocalAppUserDS()
        createNetworking()
        createRemoteAppUserDS()
        createAppUserRepository()
        createItemLocalDataSource()
        createItemRepository()
        createItemToListLocalDataSource()
        createItemToListRepository()
        createLocalShoppingListDataSource()
        createRemoteShoppingListDataSource()
        createShoppingListRepository()
        createOnlineUserLocalDS()
        createOnlineUserRemoteDS()
        createOnlineUserRepository()
    }

    suspend fun initializeUser(
        username: String,
        online: Boolean = false,
    ) {
        if (online) {
            val appUserRepository = shoppingListApplication.appUserRepository
            appUserRepository.create(username)
        } else {
            val localAppUserDataSource = shoppingListApplication.appUserLocalDataSource
            localAppUserDataSource.create(username)
        }
    }

    private fun createDatabase(clear: Boolean = true): ShoppingListDatabase {
        val database: ShoppingListDatabase?
        if (shoppingListApplication.isDatabaseInitialized()) {
            database = shoppingListApplication.database
        } else {
            val application = ApplicationProvider.getApplicationContext<Application>()
            database = ShoppingListDatabase.getInstance(application)
            shoppingListApplication.database = database
        }
        if (clear) {
            database.clearAllTables()
        }
        return database
    }

    private fun createLocalAppUserDS(): AppUserLocalDataSource {
        val localDataSource: AppUserLocalDataSource?
        if (shoppingListApplication.isAppUserLocalDSInitialized()) {
            localDataSource = shoppingListApplication.appUserLocalDataSource
        } else {
            val database = createDatabase()
            localDataSource = AppUserLocalDataSource(database)
            shoppingListApplication.appUserLocalDataSource = localDataSource
        }
        return localDataSource
    }

    private fun createNetworking(): Networking {
        val networking: Networking?
        if (shoppingListApplication.isNetworkingInitialized()) {
            networking = shoppingListApplication.networking
        } else {
            val localUserDS = createLocalAppUserDS()

            val userDataPayloadProvider = UserCreationDataProvider(localUserDS)
            val tokenProvider =
                ShoppingListAuthenticationTokenProvider(userDataPayloadProvider, "tmp/")
            networking = Networking(tokenProvider)
            shoppingListApplication.userCreationPayloadProvider = userDataPayloadProvider
            shoppingListApplication.tokenProvider = tokenProvider
            shoppingListApplication.networking = networking
        }
        return networking
    }

    private fun createRemoteAppUserDS(): AppUserRemoteDataSource {
        val remoteAppUserDataSource: AppUserRemoteDataSource?
        if (shoppingListApplication.isAppUserRemoteDSInitialized()) {
            remoteAppUserDataSource = shoppingListApplication.appUserRemoteDataSource
        } else {
            val networking = createNetworking()
            remoteAppUserDataSource = AppUserRemoteDataSource(networking)
            shoppingListApplication.appUserRemoteDataSource = remoteAppUserDataSource
        }
        return remoteAppUserDataSource
    }

    private fun createAppUserRepository(): AppUserRepository {
        val appUserRepository: AppUserRepository?
        if (shoppingListApplication.isAppUserRepositoryInitialized()) {
            appUserRepository = shoppingListApplication.appUserRepository
        } else {
            val localAppUserDataSource = createLocalAppUserDS()
            val remoteAppUserDataSource = createRemoteAppUserDS()
            appUserRepository = AppUserRepository(localAppUserDataSource, remoteAppUserDataSource)
            shoppingListApplication.appUserRepository = appUserRepository
        }
        return appUserRepository
    }

    private fun createItemLocalDataSource(): ItemLocalDataSource {
        val itemLocalDataSource: ItemLocalDataSource?
        if (shoppingListApplication.isItemLocalDSInitialized()) {
            itemLocalDataSource = shoppingListApplication.itemLocalDataSource
        } else {
            val database = createDatabase()
            itemLocalDataSource = ItemLocalDataSource(database)
            shoppingListApplication.itemLocalDataSource = itemLocalDataSource
        }
        return itemLocalDataSource
    }

    private fun createItemRepository(): ItemRepository {
        val itemRepository: ItemRepository?
        if (shoppingListApplication.isItemRepositoryInitialized()) {
            itemRepository = shoppingListApplication.itemRepository
        } else {
            val itemLocalDataSource = createItemLocalDataSource()
            itemRepository = ItemRepository(itemLocalDataSource)
            shoppingListApplication.itemRepository = itemRepository
        }
        return itemRepository
    }

    private fun createItemToListLocalDataSource(): ItemToListLocalDataSource {
        val itemToListLocalDataSource: ItemToListLocalDataSource?
        if (shoppingListApplication.isItemToListLocalDSInitialized()) {
            itemToListLocalDataSource = shoppingListApplication.itemToListLocalDataSource
        } else {
            val database = createDatabase()
            itemToListLocalDataSource = ItemToListLocalDataSource(database)
            shoppingListApplication.itemToListLocalDataSource = itemToListLocalDataSource
        }
        return itemToListLocalDataSource
    }

    private fun createItemToListRepository(): ItemToListRepository {
        val itemToListRepository: ItemToListRepository?
        if (shoppingListApplication.isItemToListRepositoryInitialized()) {
            itemToListRepository = shoppingListApplication.itemToListRepository
        } else {
            val itemToListLocalDataSource = createItemToListLocalDataSource()
            itemToListRepository = ItemToListRepository(itemToListLocalDataSource)
            shoppingListApplication.itemToListRepository = itemToListRepository
        }
        return itemToListRepository
    }

    private fun createLocalShoppingListDataSource(): ShoppingListLocalDataSource {
        val localShoppingListDataSource: ShoppingListLocalDataSource?
        if (shoppingListApplication.isShoppingListLocalDSInitialized()) {
            localShoppingListDataSource = shoppingListApplication.shoppingListLocalDataSource
        } else {
            val database = createDatabase()
            val appUserRepository = createAppUserRepository()
            val itemRepository = createItemRepository()
            val itemToListRepository = createItemToListRepository()
            val localUserDs = AppUserLocalDataSource(database)
            val payloadProvider = UserCreationDataProvider(localUserDs)
            val tokenProvider = ShoppingListAuthenticationTokenProvider(payloadProvider, "tmp")
            val networking = Networking(tokenProvider)
            val onlineUserLocalDataSource = OnlineUserLocalDataSource(database)
            val onlineUserRemoteDataSource = OnlineUserRemoteDataSource(networking)
            val onlineUserRepository =
                OnlineUserRepository(onlineUserLocalDataSource, onlineUserRemoteDataSource)
            localShoppingListDataSource =
                ShoppingListLocalDataSource(
                    database,
                    appUserRepository,
                    onlineUserRepository,
                    itemRepository,
                    itemToListRepository,
                )
            shoppingListApplication.shoppingListLocalDataSource = localShoppingListDataSource
        }
        return localShoppingListDataSource
    }

    private fun createRemoteShoppingListDataSource(): ShoppingListRemoteDataSource {
        val remoteShoppingListDataSource: ShoppingListRemoteDataSource?
        if (shoppingListApplication.isShoppingListRemoteDSInitialized()) {
            remoteShoppingListDataSource = shoppingListApplication.shoppingListRemoteDataSource
        } else {
            val networking = createNetworking()
            val appUserRepository = createAppUserRepository()
            remoteShoppingListDataSource =
                ShoppingListRemoteDataSource(networking, appUserRepository)
            shoppingListApplication.shoppingListRemoteDataSource = remoteShoppingListDataSource
        }
        return remoteShoppingListDataSource
    }

    private fun createShoppingListRepository(): ShoppingListRepository {
        val shoppingListRepository: ShoppingListRepository?
        if (shoppingListApplication.isShoppingListRepositoryInitialized()) {
            shoppingListRepository = shoppingListApplication.shoppingListRepository
        } else {
            val localShoppingListDataSource = createLocalShoppingListDataSource()
            val remoteShoppingListDataSource = createRemoteShoppingListDataSource()
            val appUserRepository = createAppUserRepository()
            shoppingListRepository =
                ShoppingListRepository(
                    localShoppingListDataSource,
                    remoteShoppingListDataSource,
                    appUserRepository,
                )
            shoppingListApplication.shoppingListRepository = shoppingListRepository
        }
        return shoppingListRepository
    }

    private fun createOnlineUserLocalDS(): OnlineUserLocalDataSource {
        val onlineUserLocalDataSource: OnlineUserLocalDataSource?
        if (shoppingListApplication.isOnlineUserLocalDSInitialized()) {
            onlineUserLocalDataSource = shoppingListApplication.onlineUserLocalDataSource
        } else {
            val database = createDatabase()
            onlineUserLocalDataSource = OnlineUserLocalDataSource(database)
            shoppingListApplication.onlineUserLocalDataSource = onlineUserLocalDataSource
        }
        return onlineUserLocalDataSource
    }

    private fun createOnlineUserRemoteDS(): OnlineUserRemoteDataSource {
        val onlineUserRemoteDataSource: OnlineUserRemoteDataSource?
        if (shoppingListApplication.isOnlineUserRemoteDSInitialized()) {
            onlineUserRemoteDataSource = shoppingListApplication.onlineUserRemoteDataSource
        } else {
            val networking = createNetworking()
            onlineUserRemoteDataSource = OnlineUserRemoteDataSource(networking)
            shoppingListApplication.onlineUserRemoteDataSource = onlineUserRemoteDataSource
        }
        return onlineUserRemoteDataSource
    }

    private fun createOnlineUserRepository(): OnlineUserRepository {
        val onlineUserRepository: OnlineUserRepository?
        if (shoppingListApplication.isOnlineUserRepositoryInitialized()) {
            onlineUserRepository = shoppingListApplication.onlineUserRepository
        } else {
            val localOnlineUserDataSource = createOnlineUserLocalDS()
            val remoteOnlineUserDataSource = createOnlineUserRemoteDS()
            onlineUserRepository =
                OnlineUserRepository(localOnlineUserDataSource, remoteOnlineUserDataSource)
            shoppingListApplication.onlineUserRepository = onlineUserRepository
        }
        return onlineUserRepository
    }
}
