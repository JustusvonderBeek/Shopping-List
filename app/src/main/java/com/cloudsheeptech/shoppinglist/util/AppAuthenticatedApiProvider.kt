package com.cloudsheeptech.shoppinglist.util

import com.cloudsheeptech.shoppinglist.list.api.ShoppingListApi
import com.cloudsheeptech.shoppinglist.list.api.ShoppingListEndpoints
import com.cloudsheeptech.shoppinglist.list.api.interceptor.AuthInterceptor
import com.cloudsheeptech.shoppinglist.list.api.interceptor.CreateUserInterceptor
import com.cloudsheeptech.shoppinglist.network.token.ShoppingListAuthenticationTokenProvider
import com.cloudsheeptech.shoppinglist.user.api.UserAuthenticatedApi
import com.cloudsheeptech.shoppinglist.user.api.UserUnauthenticatedApi
import com.cloudsheeptech.shoppinglist.user.repo.AppUserRepository
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.OffsetDateTime
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppAuthenticatedApiProvider {
    private val gson =
        GsonBuilder()
            .registerTypeAdapter(OffsetDateTime::class.java, OffsetDateTimeFormatHandler())
            .create()

//        private val authClient =
//            OkHttpClient
//                .Builder()
//                .addInterceptor(authInterceptor)
//                .authenticator(createUserInterceptor)
//                .build()
//
//        private val authRetrofitProvider =
//            Retrofit
//                .Builder()
//                .baseUrl(ShoppingListEndpoints.BASE_ENDPOINT)
//                .client(authClient)
//                .addConverterFactory(GsonConverterFactory.create(gson))
//                .build()

    @Provides
    @Singleton
    fun provideAuthInterceptor(shopingListAuthenticationTokenProvider: ShoppingListAuthenticationTokenProvider): AuthInterceptor =
        AuthInterceptor(shopingListAuthenticationTokenProvider)

    @Provides
    @Singleton
    fun provideCreateUserInterceptor(
        userRepository: AppUserRepository,
        userUnauthenticatedApi: UserUnauthenticatedApi,
    ): CreateUserInterceptor = CreateUserInterceptor(userRepository, userUnauthenticatedApi)

    @Provides
    @Singleton
    fun provideAuthClient(
        authInterceptor: AuthInterceptor,
        createUserInterceptor: CreateUserInterceptor,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(authInterceptor)
            .authenticator(createUserInterceptor)
            .build()

    @Provides
    @Singleton
    fun provideAuthRetrofitProvider(authClient: OkHttpClient): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(ShoppingListEndpoints.BASE_ENDPOINT)
            .client(authClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    // All authorized APIs must use the same retrofit builder
    @Provides
    @Singleton
    fun provideUserAuthenticatedApi(authRetrofitProvider: Retrofit): UserAuthenticatedApi =
        authRetrofitProvider.create(UserAuthenticatedApi::class.java)

    @Provides
    @Singleton
    fun provideShoppingListApi(authRetrofitProvider: Retrofit): ShoppingListApi = authRetrofitProvider.create(ShoppingListApi::class.java)

//        val shoppingListApi =
//            authRetrofitProvider.create(ShoppingListApi::class.java)
//        val shoppingRecipeApi = authRetrofitProvider.create(ShoppingRecipeApi::class.java)
}
