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

import android.annotation.SuppressLint
import android.content.Context

import javax.inject.Singleton

import dagger.Module
import dagger.Provides
import io.github.hidroh.materialistic.appwidget.WidgetConfigActivity
import io.github.hidroh.materialistic.widget.FavoriteRecyclerViewAdapter
import io.github.hidroh.materialistic.widget.MultiPageItemRecyclerViewAdapter
import io.github.hidroh.materialistic.widget.PopupMenu
import io.github.hidroh.materialistic.widget.SinglePageItemRecyclerViewAdapter
import io.github.hidroh.materialistic.widget.StoryRecyclerViewAdapter
import io.github.hidroh.materialistic.widget.SubmissionRecyclerViewAdapter
import io.github.hidroh.materialistic.widget.ThreadPreviewRecyclerViewAdapter

@Module(
        injects = {
                AboutActivity::class.java,
                AskActivity::class.java,
                BestActivity::class.java,
                ComposeActivity::class.java,
                FavoriteActivity::class.java,
                FeedbackActivity::class.java,
                ItemActivity::class.java,
                JobsActivity::class.java,
                ListActivity::class.java,
                LoginActivity::class.java,
                NewActivity::class.java,
                OfflineWebActivity::class.java,
                PopularActivity::class.java,
                ReleaseNotesActivity::class.java,
                SearchActivity::class.java,
                SettingsActivity::class.java,
                ShowActivity::class.java,
                SubmitActivity::class.java,
                ThreadPreviewActivity::class.java,
                UserActivity::class.java,
                WidgetConfigActivity::class.java,
                FavoriteFragment::class.java,
                ItemFragment::class.java,
                ListFragment::class.java,
                WebFragment::class.java,
                FavoriteRecyclerViewAdapter::class.java,
                SinglePageItemRecyclerViewAdapter::class.java,
                StoryRecyclerViewAdapter::class.java,
                SubmissionRecyclerViewAdapter::class.java,
                MultiPageItemRecyclerViewAdapter::class.java,
                ThreadPreviewRecyclerViewAdapter::class.java
        },
        library = true,
        complete = false
)
open class UiModule {
    @Provides
    fun providePopupMenu(): PopupMenu {
        return PopupMenu.Impl()
    }

    @Provides @Singleton
    fun provideCustomTabsDelegate(): CustomTabsDelegate {
        return CustomTabsDelegate()
    }

    @Provides @Singleton
    fun provideKeyDelegate(): KeyDelegate {
        return KeyDelegate()
    }

    @Provides @Singleton
    fun provideActionViewResolver(): ActionViewResolver {
        return ActionViewResolver()
    }

    @Provides
    fun provideAlertDialogBuilder(): AlertDialogBuilder {
        return AlertDialogBuilder.Impl()
    }

    @SuppressLint("Recycle")
    @Provides @Singleton
    fun provideResourcesProvider(context: Context): ResourcesProvider {
        return resId -> context.getResources().obtainTypedArray(resId)
    }
}
