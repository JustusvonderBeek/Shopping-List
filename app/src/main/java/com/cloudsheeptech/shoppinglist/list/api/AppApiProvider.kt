package com.cloudsheeptech.shoppinglist.list.api

import com.cloudsheeptech.shoppinglist.list.api.interceptor.AuthInterceptor
import com.cloudsheeptech.shoppinglist.list.api.interceptor.CreateUserInterceptor
import com.cloudsheeptech.shoppinglist.recipe.api.ShoppingRecipeApi
import com.cloudsheeptech.shoppinglist.user.api.UserApiEndpoints
import com.cloudsheeptech.shoppinglist.user.api.UserUnauthenticatedApi
import com.cloudsheeptech.shoppinglist.user.repo.AppUserRepository
import com.cloudsheeptech.shoppinglist.util.OffsetDateTimeFormatHandler
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppApiProvider
    @Inject
    constructor(
        private val authInterceptor: AuthInterceptor,
        userRepository: AppUserRepository,
    ) {
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

        // Different instance without interceptor to be able to call the user
        // endpoints without blocking the thread
        val userUnauthenticatedApi = unauthRetrofitProvider.create(UserUnauthenticatedApi::class.java)

        private val createUserInterceptor: CreateUserInterceptor =
            CreateUserInterceptor(
                userRepository,
                userUnauthenticatedApi,
            )

        private val authClient =
            OkHttpClient
                .Builder()
                .addInterceptor(authInterceptor)
                .authenticator(createUserInterceptor)
                .build()

        private val authRetrofitProvider =
            Retrofit
                .Builder()
                .baseUrl(ShoppingListEndpoints.BASE_ENDPOINT)
                .client(authClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

        // All authorized APIs must use the same retrofit builder
        val shoppingListApi =
            authRetrofitProvider.create(ShoppingListApi::class.java)
        val shoppingRecipeApi = authRetrofitProvider.create(ShoppingRecipeApi::class.java)
    }
