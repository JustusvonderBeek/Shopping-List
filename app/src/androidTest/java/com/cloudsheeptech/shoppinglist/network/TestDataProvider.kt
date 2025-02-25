package com.cloudsheeptech.shoppinglist.network

import com.cloudsheeptech.shoppinglist.data.user.ApiUser
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.OffsetDateTime

class TestDataProvider : IUserCreationDataProvider {

    private val json = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
    }

    private val mockUser = ApiUser(
        0L,
        "test user",
        "test password",
        OffsetDateTime.now(),
        OffsetDateTime.now()
    )

    private var decodedOnlineUser: ApiUser? = null

    override suspend fun provideUserCreationPayload(): String {
        val user = mockUser
        val encoded = json.encodeToString(user)
        return encoded
    }

    override suspend fun processUserCreationResponse(payload: String): Boolean {
        val decoded = json.decodeFromString<ApiUser>(payload)
        mockUser.onlineId = decoded.onlineId
        mockUser.lastLogin = decoded.lastLogin
        mockUser.created = decoded.created
        return decoded.onlineId != 0L
    }

    override fun provideLoginPayload(): Pair<String, Long> {
        if (decodedOnlineUser == null || decodedOnlineUser!!.onlineId == 0L) {
            return Pair("", -1L)
        }
        val encoded = json.encodeToString(decodedOnlineUser)
        return Pair(encoded, decodedOnlineUser!!.onlineId)
    }
}