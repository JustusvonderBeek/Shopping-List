package com.cloudsheeptech.shoppinglist.data.onlineUser

import android.util.Log
import com.cloudsheeptech.shoppinglist.data.typeConverter.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.network.Networking
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineUserRemoteDataSource @Inject constructor(
    private val networking: Networking
) {

    // TODO: I think this serializer can also be reused across instances, to increase efficiency
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
        serializersModule = SerializersModule {
            contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
        }
    }

    /**
     * This function is not used, since users are only created by the login creation process
     */
    @Throws(NotImplementedError::class)
    suspend fun create(user: ListCreator) : Boolean {
        throw NotImplementedError("this function is not used by the application")
    }

    suspend fun read(username: String) : List<ListCreator> {
        if (username.isEmpty() || username.length > 50)
            return emptyList()
        val foundCreators = mutableListOf<ListCreator>()
        withContext(Dispatchers.IO) {
            networking.GET("/v1/users/name?username=$username") { response ->
                if (response.status != HttpStatusCode.OK) {
                    Log.e("OnlineUserRemoteDataSource", "Failed to read users with name $username from online")
                    return@GET
                }
                val rawBody = response.bodyAsText(Charsets.UTF_8)
                if (rawBody.isEmpty() || rawBody == "null") {
                    Log.w("OnlineUserRemoteDataSource", "No user found online")
                    return@GET
                }
                val decoded = json.decodeFromString<List<ListCreator>>(rawBody)
                foundCreators.addAll(decoded)
                Log.d("OnlineUserRemoterDataSource", "${decoded.size} users found online for $username")
            }
        }
        return foundCreators
    }

    /**
     * This function is not used
     * @throws NotImplementedError
     */
    @Throws(NotImplementedError::class)
    suspend fun update(updatedUser: ListCreator) {
        throw NotImplementedError("this function is not used by the application")
    }

    /**
     * This function is not used
     * @throws NotImplementedError
     */
    @Throws(NotImplementedError::class)
    suspend fun delete(userId: Long) {
        throw NotImplementedError("this function is not used by the application")
    }

}