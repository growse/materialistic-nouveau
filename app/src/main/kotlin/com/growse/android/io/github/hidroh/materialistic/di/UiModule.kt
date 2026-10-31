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

import android.annotation.SuppressLint
import android.content.Context
import com.growse.android.io.github.hidroh.materialistic.ActionViewResolver
import com.growse.android.io.github.hidroh.materialistic.AlertDialogBuilder
import com.growse.android.io.github.hidroh.materialistic.CustomTabsDelegate
import com.growse.android.io.github.hidroh.materialistic.KeyDelegate
import com.growse.android.io.github.hidroh.materialistic.ResourcesProvider
import com.growse.android.io.github.hidroh.materialistic.widget.PopupMenu
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
internal class UiModule {
  @Provides
  fun providePopupMenu(): PopupMenu {
    return PopupMenu.Impl()
  }

  @Provides
  @ActivityScoped
  fun provideCustomTabsDelegate(): CustomTabsDelegate {
    return CustomTabsDelegate()
  }

  @Provides
  @ActivityScoped
  fun provideKeyDelegate(): KeyDelegate {
    return KeyDelegate()
  }

  @Provides
  @ActivityScoped
  fun provideActionViewResolver(): ActionViewResolver {
    return ActionViewResolver()
  }

  @Provides
  fun provideAlertDialogBuilder(): AlertDialogBuilder<*> {
    return AlertDialogBuilder.Impl()
  }

  @SuppressLint("Recycle")
  @Provides
  @ActivityScoped
  fun provideResourcesProvider(@ActivityContext context: Context): ResourcesProvider {
    return ResourcesProvider { resId: Int -> context.resources.obtainTypedArray(resId) }
  }
}
