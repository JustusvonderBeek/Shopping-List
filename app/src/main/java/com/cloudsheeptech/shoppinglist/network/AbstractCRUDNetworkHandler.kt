package com.cloudsheeptech.shoppinglist.network

import android.util.Log
import com.cloudsheeptech.shoppinglist.exception.UserAuthenticationFailedException
import com.cloudsheeptech.shoppinglist.exception.UserNotAuthenticatedException
import javax.inject.Inject

abstract class AbstractCRUDNetworkHandler<T : Any?> @Inject constructor(private val networking: Networking) :
    IAbstractCRUDDefinition<T>, ICreateAccountHandler {

    private val LOG_TAG = "AbstractCRUDNetworkHandler"
    private val retryCount = 3

    abstract fun createAccount(): Boolean

    override suspend fun create(data: T, responseHandler: IHttpResponseHandler): Boolean {
        var success = false
        for (i in 0..retryCount) {
            try {
                networking.POST("path", data.toString()) { response ->
                    success = responseHandler.handle(response)
                }
            } catch (ex: UserNotAuthenticatedException) {
                Log.w(LOG_TAG, "User does not exist online: $ex")
                success = false
            } catch (ex: UserAuthenticationFailedException) {
                Log.e(LOG_TAG, "Authenticating the user online failed: $ex")
                success = false
            }
            if (success) {
                return true
            }
            try {
                success = this.createAccount()
            } catch (ex: Exception) {
                Log.e(LOG_TAG, "An unknown exception occured while creating the user online: $ex")
                success = false
            }
            // TODO: Do we want to continue even if this fails?
        }
        return success
    }

    override fun read(primaryKey: IPrimaryKey): T? {
        TODO("Not yet implemented")
    }

    override fun readAll(primaryKey: IPrimaryKey): List<T> {
        TODO("Not yet implemented")
    }

    override fun update(data: Any): Boolean {
        TODO("Not yet implemented")
    }

    override fun delete(primaryKey: IPrimaryKey): Boolean {
        TODO("Not yet implemented")
    }
}