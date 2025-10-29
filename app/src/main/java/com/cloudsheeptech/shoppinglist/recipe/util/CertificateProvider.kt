package com.cloudsheeptech.shoppinglist.recipe.util

import android.content.Context
import com.cloudsheeptech.shoppinglist.R
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.InputStream
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class CertificateProvider {
    @Provides
    @Singleton
    fun provideAppCertificate(
        @ApplicationContext context: Context,
    ): InputStream = context.resources.openRawResource(R.raw.shoppinglist_local)
}
