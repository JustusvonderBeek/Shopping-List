package com.cloudsheeptech.shoppinglist.util

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppFileDirProvider {

    @Singleton
    @Named("appFileDir")
    @Provides
    fun provides(@ApplicationContext context: Context): String {
        return context.filesDir.absolutePath
    }

}