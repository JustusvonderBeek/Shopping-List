package com.cloudsheeptech.shoppinglist.util

import com.cloudsheeptech.shoppinglist.user.api.UserApiEndpoints
import com.cloudsheeptech.shoppinglist.user.api.UserUnauthenticatedApi
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.OffsetDateTime
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppUnauthenticatedApiProvider {
    private val gson =
        GsonBuilder()
            .registerTypeAdapter(OffsetDateTime::class.java, OffsetDateTimeFormatHandler())
            .create()

    // Attention: Order is relevant here, injecting unauthClient into
    private val unauthRetrofitProvider =
        Retrofit
            .Builder()
            .baseUrl(UserApiEndpoints.BASE_ENDPOINT)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Singleton
    @Provides
    fun provideUnauthApi(): UserUnauthenticatedApi = unauthRetrofitProvider.create(UserUnauthenticatedApi::class.java)
}
