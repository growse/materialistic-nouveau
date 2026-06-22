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

package com.growse.android.io.github.hidroh.materialistic;

import android.accounts.AccountManager;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import android.content.Context;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServices;
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServicesClient;
import com.growse.android.io.github.hidroh.materialistic.data.AlgoliaClient;
import com.growse.android.io.github.hidroh.materialistic.data.AlgoliaPopularClient;
import com.growse.android.io.github.hidroh.materialistic.data.FeedbackClient;
import com.growse.android.io.github.hidroh.materialistic.data.HackerNewsClient;
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager;
import com.growse.android.io.github.hidroh.materialistic.data.ItemManagerQualifiers;
import com.growse.android.io.github.hidroh.materialistic.data.LocalCache;
import com.growse.android.io.github.hidroh.materialistic.data.MaterialisticDatabase;
import com.growse.android.io.github.hidroh.materialistic.data.ReadabilityClient;
import com.growse.android.io.github.hidroh.materialistic.data.SyncScheduler;
import com.growse.android.io.github.hidroh.materialistic.data.UserManager;
import com.growse.android.io.github.hidroh.materialistic.data.android.Cache;
import okhttp3.Call;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

@Module
@InstallIn(SingletonComponent.class)
public class DataModule {
    public static final String MAIN_THREAD = "main";
    public static final String IO_THREAD = "io";

    @Provides @Singleton @Named(ItemManagerQualifiers.HN)
    public ItemManager provideHackerNewsClient(HackerNewsClient client) {
        return client;
    }

    @Provides @Singleton @Named(ItemManagerQualifiers.ALGOLIA)
    public ItemManager provideAlgoliaClient(AlgoliaClient client) {
        return client;
    }

    @Provides @Singleton @Named(ItemManagerQualifiers.POPULAR)
    public ItemManager provideAlgoliaPopularClient(AlgoliaPopularClient client) {
        return client;
    }

    @Provides @Singleton
    public AccountManager provideAccountManager(@ApplicationContext Context context) {
        return AccountManager.get(context);
    }

    @Provides @Singleton
    public UserManager provideUserManager(HackerNewsClient client) {
        return client;
    }

    @Provides @Singleton
    public FeedbackClient provideFeedbackClient(FeedbackClient.Impl client) {
        return client;
    }

    @Provides @Singleton
    public ReadabilityClient provideReadabilityClient(ReadabilityClient.Impl client) {
        return client;
    }

    @Provides @Singleton
    public UserServices provideUserServices(Call.Factory callFactory,
                                            @Named(IO_THREAD) Scheduler ioScheduler) {
        return new UserServicesClient(callFactory, ioScheduler);
    }

    @Provides @Singleton @Named(IO_THREAD)
    public Scheduler provideIoScheduler() {
        return Schedulers.io();
    }

    @Provides @Singleton @Named(MAIN_THREAD)
    public Scheduler provideMainThreadScheduler() {
        return AndroidSchedulers.mainThread();
    }

    @Provides @Singleton
    public SyncScheduler provideSyncScheduler() {
        return new SyncScheduler();
    }

    @Provides @Singleton
    public LocalCache provideLocalCache(Cache cache) {
        return cache;
    }

    @Provides @Singleton
    public MaterialisticDatabase provideDatabase(@ApplicationContext Context context) {
        return MaterialisticDatabase.getInstance(context);
    }

    @Provides
    public MaterialisticDatabase.SavedStoriesDao provideSavedStoriesDao(MaterialisticDatabase database) {
        return database.getSavedStoriesDao();
    }

    @Provides
    public MaterialisticDatabase.ReadStoriesDao provideReadStoriesDao(MaterialisticDatabase database) {
        return database.getReadStoriesDao();
    }

    @Provides
    public MaterialisticDatabase.ReadableDao provideReadableDao(MaterialisticDatabase database) {
        return database.getReadableDao();
    }

    @Provides
    public SupportSQLiteOpenHelper provideOpenHelper(MaterialisticDatabase database) {
        return database.getOpenHelper();
    }
}
