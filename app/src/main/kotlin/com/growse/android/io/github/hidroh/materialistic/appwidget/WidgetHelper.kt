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

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.SearchManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.text.format.DateUtils
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.growse.android.io.github.hidroh.materialistic.BestActivity
import com.growse.android.io.github.hidroh.materialistic.ListActivity
import com.growse.android.io.github.hidroh.materialistic.NewActivity
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.SearchActivity
import java.util.Locale

internal class WidgetHelper(private val mContext: Context) {
  private val mAppWidgetManager: AppWidgetManager
  private val mAlarmManager: AlarmManager?

  init {
    mAppWidgetManager = AppWidgetManager.getInstance(mContext)
    mAlarmManager = mContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
  }

  fun configure(appWidgetId: Int) {
    scheduleUpdate(appWidgetId)
    update(appWidgetId)
  }

  fun update(appWidgetId: Int) {
    val config =
        WidgetConfig.createWidgetConfig(
            mContext,
            getConfig(appWidgetId, R.string.pref_widget_theme),
            getConfig(appWidgetId, R.string.pref_widget_section),
            getConfig(appWidgetId, R.string.pref_widget_query),
        )
    val remoteViews = RemoteViews(mContext.packageName, config.widgetLayout)
    updateTitle(remoteViews, config)
    updateCollection(appWidgetId, remoteViews, config)
    mAppWidgetManager.updateAppWidget(appWidgetId, remoteViews)
  }

  fun refresh(appWidgetId: Int) {
    mAppWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, android.R.id.list)
    update(appWidgetId)
  }

  fun remove(appWidgetId: Int) {
    cancelScheduledUpdate(appWidgetId)
    clearConfig(appWidgetId)
  }

  private fun scheduleUpdate(appWidgetId: Int) {
    val frequency = getConfig(appWidgetId, R.string.pref_widget_frequency)
    val frequencyHourMillis =
        DateUtils.HOUR_IN_MILLIS *
            (if (TextUtils.isEmpty(frequency)) DEFAULT_FREQUENCY_HOUR else frequency!!.toInt())
    this.jobScheduler!!.schedule(
        JobInfo.Builder(
                appWidgetId,
                ComponentName(
                    mContext.packageName,
                    WidgetRefreshJobService::class.java.name,
                ),
            )
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(frequencyHourMillis)
            .build()
    )
  }

  private fun cancelScheduledUpdate(appWidgetId: Int) {
    this.jobScheduler!!.cancel(appWidgetId)
  }

  private val jobScheduler: JobScheduler?
    get() = mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler?

  private fun getConfig(appWidgetId: Int, @StringRes key: Int): String? {
    return mContext
        .getSharedPreferences(getConfigName(appWidgetId), Context.MODE_PRIVATE)
        .getString(mContext.getString(key), null)
  }

  private fun clearConfig(appWidgetId: Int) {
    mContext.getSharedPreferences(getConfigName(appWidgetId), Context.MODE_PRIVATE).edit { clear() }
  }

  private fun updateTitle(remoteViews: RemoteViews, config: WidgetConfig) {
    remoteViews.setTextViewText(R.id.title, config.title)
    remoteViews.setOnClickPendingIntent(
        R.id.title,
        PendingIntent.getActivity(
            mContext,
            0,
            if (config.customQuery)
                Intent(mContext, config.destination).putExtra(SearchManager.QUERY, config.title)
            else
                Intent(
                    mContext,
                    config.destination,
                ),
            PendingIntent.FLAG_IMMUTABLE,
        ),
    )
  }

  private fun updateCollection(appWidgetId: Int, remoteViews: RemoteViews, config: WidgetConfig) {
    remoteViews.setTextViewText(
        R.id.subtitle,
        DateUtils.formatDateTime(
            mContext,
            System.currentTimeMillis(),
            DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_TIME,
        ),
    )
    remoteViews.setOnClickPendingIntent(
        R.id.button_refresh,
        createRefreshPendingIntent(appWidgetId),
    )
    val intent =
        Intent(mContext, WidgetService::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .putExtra(WidgetService.EXTRA_CUSTOM_QUERY, config.customQuery)
            .putExtra(WidgetService.EXTRA_SECTION, config.section)
            .putExtra(WidgetService.EXTRA_LIGHT_THEME, config.isLightTheme)
    intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))
    remoteViews.setRemoteAdapter(android.R.id.list, intent)
    remoteViews.setEmptyView(android.R.id.list, R.id.empty)
    remoteViews.setPendingIntentTemplate(
        android.R.id.list,
        PendingIntent.getActivity(
            mContext,
            0,
            Intent(Intent.ACTION_VIEW),
            PendingIntent.FLAG_IMMUTABLE,
        ),
    )
  }

  private fun createRefreshPendingIntent(appWidgetId: Int): PendingIntent? {
    return PendingIntent.getBroadcast(
        mContext,
        appWidgetId,
        Intent(WidgetProvider.ACTION_REFRESH_WIDGET)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .setPackage(mContext.packageName),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  internal class WidgetConfig
  private constructor(
      val destination: Class<out Activity?>?,
      val title: String?,
      val section: String?,
      val isLightTheme: Boolean,
      @field:LayoutRes val widgetLayout: Int,
  ) {
    val customQuery: Boolean

    init {
      this.customQuery = destination == SearchActivity::class.java
    }

    companion object {
      fun createWidgetConfig(
          context: Context,
          theme: String?,
          section: String?,
          query: String?,
      ): WidgetConfig {
        var section = section
        val widgetLayout: Int
        var isLightTheme = false
        if (
            TextUtils.equals(
                theme,
                context.getString(R.string.pref_widget_theme_value_dark),
            )
        ) {
          widgetLayout = R.layout.appwidget_dark
        } else if (
            TextUtils.equals(
                theme,
                context.getString(R.string.pref_widget_theme_value_light),
            )
        ) {
          widgetLayout = R.layout.appwidget_light
          isLightTheme = true
        } else {
          widgetLayout = R.layout.appwidget
        }
        val title: String?
        val destination: Class<out Activity?>?
        if (!TextUtils.isEmpty(query)) {
          title = query
          section = query
          destination = SearchActivity::class.java
        } else if (
            TextUtils.equals(
                section,
                context.getString(R.string.pref_widget_section_value_best),
            )
        ) {
          title = context.getString(R.string.title_activity_best)
          destination = BestActivity::class.java
        } else if (
            TextUtils.equals(
                section,
                context.getString(R.string.pref_widget_section_value_top),
            )
        ) {
          title = context.getString(R.string.title_activity_list)
          destination = ListActivity::class.java
        } else {
          // legacy "new stories" widget
          title = context.getString(R.string.title_activity_new)
          destination = NewActivity::class.java
        }
        return WidgetConfig(destination, title, section, isLightTheme, widgetLayout)
      }
    }
  }

  companion object {
    private const val SP_NAME = "WidgetConfiguration_%1\$d"
    private const val DEFAULT_FREQUENCY_HOUR = 6

    fun getConfigName(appWidgetId: Int): String {
      return String.format(Locale.US, SP_NAME, appWidgetId)
    }
  }
}
