/*
 * Copyright (c) 2016 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.growse.android.io.github.hidroh.materialistic.di

import android.accounts.AccountManager
import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServices
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServicesClient
import com.growse.android.io.github.hidroh.materialistic.data.AlgoliaClient
import com.growse.android.io.github.hidroh.materialistic.data.AlgoliaPopularClient
import com.growse.android.io.github.hidroh.materialistic.data.FeedbackClient
import com.growse.android.io.github.hidroh.materialistic.data.HackerNewsClient
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager
import com.growse.android.io.github.hidroh.materialistic.data.ItemManagerQualifiers
import com.growse.android.io.github.hidroh.materialistic.data.LocalCache
import com.growse.android.io.github.hidroh.materialistic.data.MaterialisticDatabase
import com.growse.android.io.github.hidroh.materialistic.data.MaterialisticDatabase.ItemCacheDao
import com.growse.android.io.github.hidroh.materialistic.data.MaterialisticDatabase.ReadStoriesDao
import com.growse.android.io.github.hidroh.materialistic.data.MaterialisticDatabase.ReadableDao
import com.growse.android.io.github.hidroh.materialistic.data.MaterialisticDatabase.SavedStoriesDao
import com.growse.android.io.github.hidroh.materialistic.data.ReadabilityClient
import com.growse.android.io.github.hidroh.materialistic.data.SyncScheduler
import com.growse.android.io.github.hidroh.materialistic.data.UserManager
import com.growse.android.io.github.hidroh.materialistic.data.android.Cache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.Call
import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

@Module
@InstallIn(SingletonComponent::class)
class DataModule {
  @Provides
  @Singleton
  @Named(ItemManagerQualifiers.HN)
  fun provideHackerNewsClient(client: HackerNewsClient): ItemManager {
    return client
  }

  @Provides
  @Singleton
  @Named(ItemManagerQualifiers.ALGOLIA)
  fun provideAlgoliaClient(client: AlgoliaClient): ItemManager {
    return client
  }

  @Provides
  @Singleton
  @Named(ItemManagerQualifiers.POPULAR)
  fun provideAlgoliaPopularClient(client: AlgoliaPopularClient): ItemManager {
    return client
  }

  @Provides
  @Singleton
  fun provideAccountManager(@ApplicationContext context: Context): AccountManager {
    return AccountManager.get(context)
  }

  @Provides
  @Singleton
  fun provideUserManager(client: HackerNewsClient): UserManager {
    return client
  }

  @Provides
  @Singleton
  fun provideFeedbackClient(client: FeedbackClient.Impl): FeedbackClient {
    return client
  }

  @Provides
  @Singleton
  fun provideReadabilityClient(client: ReadabilityClient.Impl): ReadabilityClient {
    return client
  }

  @Provides
  @Singleton
  fun provideUserServices(
      callFactory: Call.Factory,
      @Named(IO_THREAD) ioScheduler: Scheduler,
  ): UserServices {
    return UserServicesClient(callFactory, ioScheduler)
  }

  @Provides
  @Singleton
  @Named(IO_THREAD)
  fun provideIoScheduler(): Scheduler {
    return Schedulers.io()
  }

  @Provides
  @Singleton
  @Named(MAIN_THREAD)
  fun provideMainThreadScheduler(): Scheduler {
    return AndroidSchedulers.mainThread()
  }

  @Provides
  @Singleton
  fun provideSyncScheduler(): SyncScheduler {
    return SyncScheduler()
  }

  @Provides
  @Singleton
  fun provideLocalCache(cache: Cache): LocalCache {
    return cache
  }

  @Provides
  @Singleton
  fun provideDatabase(@ApplicationContext context: Context): MaterialisticDatabase {
    return MaterialisticDatabase.getInstance(context)
  }

  @Provides
  fun provideSavedStoriesDao(database: MaterialisticDatabase): SavedStoriesDao {
    return database.savedStoriesDao
  }

  @Provides
  fun provideReadStoriesDao(database: MaterialisticDatabase): ReadStoriesDao {
    return database.readStoriesDao
  }

  @Provides
  fun provideReadableDao(database: MaterialisticDatabase): ReadableDao {
    return database.readableDao
  }

  @Provides
  fun provideItemCacheDao(database: MaterialisticDatabase): ItemCacheDao {
    return database.itemCacheDao
  }

  @Provides
  fun provideOpenHelper(database: MaterialisticDatabase): SupportSQLiteOpenHelper {
    return database.openHelper
  }

  companion object {
    const val MAIN_THREAD: String = "main"
    const val IO_THREAD: String = "io"
  }
}
