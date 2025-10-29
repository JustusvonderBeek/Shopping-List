package com.cloudsheeptech.shoppinglist

import com.cloudsheeptech.shoppinglist.network.IUserCreationDataProvider
import com.cloudsheeptech.shoppinglist.user.model.ApiUser
import com.cloudsheeptech.shoppinglist.user.model.UserRightsEnum
import com.cloudsheeptech.shoppinglist.util.OffsetDateTimeFormatHandler
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.time.OffsetDateTime

class TestUserPayloadProvider : IUserCreationDataProvider {
    private val json =
        Json {
            ignoreUnknownKeys = false
            encodeDefaults = true
            serializersModule =
                SerializersModule {
                    contextual(OffsetDateTime::class, OffsetDateTimeFormatHandler())
                }
        }

    private val mockUser =
        ApiUser(
            0L,
            "test user",
            "test password",
            UserRightsEnum.USER.value,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
        )

    private var decodedOnlineUser: ApiUser? = null

    override suspend fun provideUserCreationPayload(): String? {
        if (decodedOnlineUser != null) {
            return null
        }
        val user = mockUser
        val encoded = json.encodeToString(user)
        return encoded
    }

    override suspend fun processUserCreationResponse(payload: String): Boolean {
        val decoded = json.decodeFromString<ApiUser>(payload)
        decodedOnlineUser = decoded
        mockUser.onlineId = decoded.onlineId
        mockUser.lastLogin = decoded.lastLogin
        mockUser.created = decoded.created
        return decoded.onlineId != 0L
    }

    override fun provideLoginPayload(): Pair<String, Long>? {
        if (decodedOnlineUser == null || decodedOnlineUser!!.onlineId == 0L) {
            return null
        }
        decodedOnlineUser!!.password = "test password"
        val encoded = json.encodeToString(decodedOnlineUser)
        return Pair(encoded, decodedOnlineUser!!.onlineId)
    }
}
