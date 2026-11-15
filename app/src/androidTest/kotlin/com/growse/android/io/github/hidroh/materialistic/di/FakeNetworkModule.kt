package com.growse.android.io.github.hidroh.materialistic.di

import com.growse.android.io.github.hidroh.materialistic.data.RestServiceFactory
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackend
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton
import okhttp3.Call

/** Routes all of the app's HTTP traffic to [FakeBackend] instead of the real servers. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [NetworkModule::class])
object FakeNetworkModule {
  @Provides
  @Singleton
  fun provideRestServiceFactory(callFactory: Call.Factory): RestServiceFactory =
      RestServiceFactory.Impl(callFactory)

  @Provides @Singleton fun provideCallFactory(): Call.Factory = FakeBackend.client
}
