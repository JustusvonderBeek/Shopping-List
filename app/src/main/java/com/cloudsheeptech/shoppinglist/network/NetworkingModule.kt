package com.cloudsheeptech.shoppinglist.network

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkingModule {

    @Provides
    @Singleton
    fun provideTokenFileString(@ApplicationContext context: Context) : String {
        // This module is used to provide the application dir + tokenFile
        // to the network class
        // TODO: Maybe move the concrete filename into a config or global const list
        val tokenFileName = "token.txt"
        return context.filesDir.path + "/$tokenFileName"
    }

}