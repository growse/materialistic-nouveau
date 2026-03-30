/*
 * Copyright (c) 2015 Ha Duy Trung
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

import android.accounts.AccountManager
import android.content.Context

import javax.inject.Singleton

import dagger.Module
import dagger.Provides
import io.github.hidroh.materialistic.appwidget.WidgetService
import io.github.hidroh.materialistic.data.ItemSyncJobService
import io.github.hidroh.materialistic.data.ItemSyncService

@Module(
        injects = {
                ItemSyncService::class.java,
                WidgetService::class.java,
                ItemSyncJobService::class.java
        },
        library = true,
        includes = DataModule::class.java
)
open class ActivityModule {
    const val ALGOLIA: String = "algolia"
    const val POPULAR: String = "popular"
    const val HN: String = "hn"

    private var mContext: Context? = null

    constructor(context: Context) {
        mContext = context
    }

    @Provides @Singleton
    fun provideContext(): Context {
        return mContext
    }

    @Provides @Singleton
    fun provideAccountManager(context: Context): AccountManager {
        return AccountManager.get(context)
    }
}
