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

package io.github.hidroh.materialistic

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import dagger.Module
import dagger.Provides
import io.github.hidroh.materialistic.accounts.UserServices
import io.github.hidroh.materialistic.accounts.UserServicesClient
import io.github.hidroh.materialistic.data.AlgoliaClient
import io.github.hidroh.materialistic.data.AlgoliaPopularClient
import io.github.hidroh.materialistic.data.FeedbackClient
import io.github.hidroh.materialistic.data.HackerNewsClient
import io.github.hidroh.materialistic.data.ItemManager
import io.github.hidroh.materialistic.data.LocalCache
import io.github.hidroh.materialistic.data.MaterialisticDatabase
import io.github.hidroh.materialistic.data.ReadabilityClient
import io.github.hidroh.materialistic.data.SyncScheduler
import io.github.hidroh.materialistic.data.UserManager
import io.github.hidroh.materialistic.data.android.Cache
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.Call
import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import static

io.github.hidroh.materialistic.ActivityModule.ALGOLIA

import static io.github.hidroh.materialistic.ActivityModule.HN

import static io.github.hidroh.materialistic.ActivityModule.POPULAR

@Module(library = true, complete = false, includes = NetworkModule::class.java)
open class DataModule {
  const val MAIN_THREAD: String = "main"
  const val IO_THREAD: String = "io"

  @Provides
  @Singleton
  @Named(HN)
  fun provideHackerNewsClient(client: HackerNewsClient): ItemManager {
    return client
  }

  @Provides
  @Singleton
  @Named(ALGOLIA)
  fun provideAlgoliaClient(client: AlgoliaClient): ItemManager {
    return client
  }

  @Provides
  @Singleton
  @Named(POPULAR)
  fun provideAlgoliaPopularClient(client: AlgoliaPopularClient): ItemManager {
    return client
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
  fun provideUserServices(callFactory: Call.Factory, ioScheduler: Scheduler): UserServices {
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
  fun provideDatabase(context: Context): MaterialisticDatabase {
    return MaterialisticDatabase.getInstance(context)
  }

  @Provides
  fun provideSavedStoriesDao(
      database: MaterialisticDatabase
  ): MaterialisticDatabase.SavedStoriesDao {
    return database.getSavedStoriesDao()
  }

  @Provides
  fun provideReadStoriesDao(database: MaterialisticDatabase): MaterialisticDatabase.ReadStoriesDao {
    return database.getReadStoriesDao()
  }

  @Provides
  fun provideReadableDao(database: MaterialisticDatabase): MaterialisticDatabase.ReadableDao {
    return database.getReadableDao()
  }

  @Provides
  fun provideOpenHelper(database: MaterialisticDatabase): SupportSQLiteOpenHelper {
    return database.getOpenHelper()
  }
}
