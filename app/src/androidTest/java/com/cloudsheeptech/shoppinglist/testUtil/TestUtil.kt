package com.cloudsheeptech.shoppinglist.testUtil

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.cloudsheeptech.shoppinglist.ShoppingListApplication
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.user.AppUserLocalDataSource
import com.cloudsheeptech.shoppinglist.data.user.AppUserRemoteDataSource
import com.cloudsheeptech.shoppinglist.data.user.UserCreationDataProvider
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.network.TokenProvider

object TestUtil {
    var shoppingListApplication: ShoppingListApplication = ShoppingListApplication()

    fun initialize() {
        createDatabase()
        createLocalAppUserDS()
        createNetworking()
        createRemoteAppUserDS()
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
        val localUserDS = createLocalAppUserDS()
        val tokenFile = ApplicationProvider.getApplicationContext<Application>().filesDir.path + "/token.txt"

        val userDataPayloadProvider = UserCreationDataProvider(localUserDS)
        val tokenProvider = TokenProvider(userDataPayloadProvider)
        val networking = Networking(tokenFile, tokenProvider)
        shoppingListApplication.userCreationPayloadProvider = userDataPayloadProvider
        shoppingListApplication.tokenProvider = tokenProvider
        shoppingListApplication.networking = networking
        return networking
    }

    private fun createRemoteAppUserDS(): AppUserRemoteDataSource {
        val networking = createNetworking()
        val remoteDataSource = AppUserRemoteDataSource(networking)
        shoppingListApplication.appUserRemoteDataSource = remoteDataSource
        return remoteDataSource
    }
}
