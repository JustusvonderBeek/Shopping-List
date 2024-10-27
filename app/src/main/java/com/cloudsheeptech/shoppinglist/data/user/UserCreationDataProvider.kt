package com.cloudsheeptech.shoppinglist.data.user

import com.cloudsheeptech.shoppinglist.data.typeConverter.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.network.UserCreationPayloadProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserCreationDataProvider
    @Inject
    constructor(
        private val appUserLocalDataSource: AppUserLocalDataSource,
    ) : UserCreationPayloadProvider {
        private val json =
            Json {
                serializersModule =
                    SerializersModule {
                        contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
                    }
                ignoreUnknownKeys = false
                encodeDefaults = true
            }

        override suspend fun provideUserCreationPayload(): String? {
            appUserLocalDataSource.read()
            val user = appUserLocalDataSource.getUser() ?: return null
            if (user.OnlineID != 0L) return null
            val convertedUser = UserFormatAdapter.fromAppToApiUser(user)
            return json.encodeToString(convertedUser)
        }

        override suspend fun processUserCreationResponse(payload: String) {
            val userFromAnswer = json.decodeFromString<ApiUser>(payload)
            if (userFromAnswer.onlineId == 0L) return
            appUserLocalDataSource.setOnlineId(userFromAnswer.onlineId)
            appUserLocalDataSource.store()
        }

        override fun provideLoginPayload(): Pair<String, Long> {
            val user = appUserLocalDataSource.getUser() ?: return Pair("", -1L)
            val convertedUser = UserFormatAdapter.fromAppToApiUser(user)
            val encodedUser = json.encodeToString(convertedUser)
            val loginInformation = Pair(encodedUser, convertedUser.onlineId)
            return loginInformation
        }
    }
