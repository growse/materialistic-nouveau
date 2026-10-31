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
package com.growse.android.io.github.hidroh.materialistic.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import com.growse.android.io.github.hidroh.materialistic.BuildConfig

class WidgetProvider : AppWidgetProvider() {
  override fun onReceive(context: Context, intent: Intent) {
    if (TextUtils.equals(intent.action, ACTION_REFRESH_WIDGET)) {
      val appWidgetId =
          intent.getIntExtra(
              AppWidgetManager.EXTRA_APPWIDGET_ID,
              AppWidgetManager.INVALID_APPWIDGET_ID,
          )
      WidgetHelper(context).refresh(appWidgetId)
    } else if (TextUtils.equals(intent.action, AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
      val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
      if (appWidgetIds != null) {
        val widgetHelper = WidgetHelper(context)
        for (appWidgetId in appWidgetIds) {
          widgetHelper.configure(appWidgetId)
        }
      }
    } else {
      super.onReceive(context, intent)
    }
  }

  override fun onUpdate(
      context: Context,
      appWidgetManager: AppWidgetManager?,
      appWidgetIds: IntArray,
  ) {
    val widgetHelper = WidgetHelper(context)
    for (appWidgetId in appWidgetIds) {
      widgetHelper.update(appWidgetId)
    }
  }

  override fun onDeleted(context: Context, appWidgetIds: IntArray) {
    val widgetHelper = WidgetHelper(context)
    for (appWidgetId in appWidgetIds) {
      widgetHelper.remove(appWidgetId)
    }
  }

  companion object {
    const val ACTION_REFRESH_WIDGET: String = BuildConfig.APPLICATION_ID + ".ACTION_REFRESH_WIDGET"
  }
}
