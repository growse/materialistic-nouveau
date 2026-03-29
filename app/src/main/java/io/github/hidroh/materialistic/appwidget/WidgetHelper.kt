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

package io.github.hidroh.materialistic.appwidget

import android.annotation.TargetApi
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
import android.os.Build
import android.text.TextUtils
import android.text.format.DateUtils
import android.widget.RemoteViews

import java.util.Locale

import androidx.annotation.LayoutRes
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import io.github.hidroh.materialistic.BestActivity
import io.github.hidroh.materialistic.ListActivity
import io.github.hidroh.materialistic.NewActivity
import io.github.hidroh.materialistic.R
import io.github.hidroh.materialistic.SearchActivity

import static android.content.Context.ALARM_SERVICE
import static android.content.Context.MODE_PRIVATE

open class WidgetHelper {
    private const val SP_NAME: String = "WidgetConfiguration_%1$d"
    private const val DEFAULT_FREQUENCY_HOUR: Int = 6
    private var mContext: Context? = null
    private var mAppWidgetManager: AppWidgetManager? = null
    private var mAlarmManager: AlarmManager? = null

    constructor(context: Context) {
        mContext = context
        mAppWidgetManager = AppWidgetManager.getInstance(context)
        mAlarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE)
    }

    fun getConfigName(appWidgetId: Int): String {
        return String.format(Locale.US, SP_NAME, appWidgetId)
    }

    fun configure(appWidgetId: Int) {
        scheduleUpdate(appWidgetId)
        update(appWidgetId)
    }

    fun update(appWidgetId: Int) {
        WidgetConfig config = WidgetConfig.createWidgetConfig(mContext,
                getConfig(appWidgetId, R.string.pref_widget_theme),
                getConfig(appWidgetId, R.string.pref_widget_section),
                getConfig(appWidgetId, R.string.pref_widget_query))
        RemoteViews remoteViews = RemoteViews(mContext.getPackageName(), config.widgetLayout)
        updateTitle(remoteViews, config)
        updateCollection(appWidgetId, remoteViews, config)
        mAppWidgetManager.updateAppWidget(appWidgetId, remoteViews)
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    fun refresh(appWidgetId: Int) {
        mAppWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, android.R.id.list)
        update(appWidgetId)
    }

    fun remove(appWidgetId: Int) {
        cancelScheduledUpdate(appWidgetId)
        clearConfig(appWidgetId)
    }

    private fun scheduleUpdate(appWidgetId: Int) {
        String frequency = getConfig(appWidgetId, R.string.pref_widget_frequency)
        long frequencyHourMillis = DateUtils.HOUR_IN_MILLIS * (TextUtils.isEmpty(frequency) ?
                DEFAULT_FREQUENCY_HOUR : Integer.valueOf(frequency))
        getJobScheduler().schedule(JobInfo.Builder(appWidgetId,
                ComponentName(mContext.getPackageName(), WidgetRefreshJobService::class.java.getName()))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(frequencyHourMillis)
                .build())
    }

    private fun cancelScheduledUpdate(appWidgetId: Int) {
        getJobScheduler().cancel(appWidgetId)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getJobScheduler(): JobScheduler {
        return (JobScheduler) mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE)
    }

    private fun getConfig(appWidgetId: Int, key: Int): String {
        return mContext.getSharedPreferences(getConfigName(appWidgetId), MODE_PRIVATE)
                .getString(mContext.getString(key), null)
    }

    private fun clearConfig(appWidgetId: Int) {
        mContext.getSharedPreferences(getConfigName(appWidgetId), MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
    }

    private fun updateTitle(remoteViews: RemoteViews, config: WidgetConfig) {
        remoteViews.setTextViewText(R.id.title, config.title)
        remoteViews.setOnClickPendingIntent(R.id.title,
                PendingIntent.getActivity(mContext, 0, config.customQuery ?
                        Intent(mContext, config.destination)
                                .putExtra(SearchManager.QUERY, config.title) :
                        Intent(mContext, config.destination),
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                                PendingIntent.FLAG_IMMUTABLE :
                                0))
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private fun updateCollection(appWidgetId: Int, remoteViews: RemoteViews, config: WidgetConfig) {
        remoteViews.setTextViewText(R.id.subtitle,
                DateUtils.formatDateTime(mContext, System.currentTimeMillis(),
                        DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_TIME))
        remoteViews.setOnClickPendingIntent(R.id.button_refresh,
                createRefreshPendingIntent(appWidgetId))
        Intent intent = Intent(mContext, WidgetService::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                .putExtra(WidgetService.EXTRA_CUSTOM_QUERY, config.customQuery)
                .putExtra(WidgetService.EXTRA_SECTION, config.section)
                .putExtra(WidgetService.EXTRA_LIGHT_THEME, config.isLightTheme)
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)))
        remoteViews.setRemoteAdapter(android.R.id.list, intent)
        remoteViews.setEmptyView(android.R.id.list, R.id.empty)
        remoteViews.setPendingIntentTemplate(android.R.id.list,
                PendingIntent.getActivity(mContext, 0, Intent(Intent.ACTION_VIEW),
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                                PendingIntent.FLAG_IMMUTABLE :
                                0))
    }

    private fun createRefreshPendingIntent(appWidgetId: Int): PendingIntent {
        return PendingIntent.getBroadcast(mContext, appWidgetId,
                Intent(WidgetProvider.ACTION_REFRESH_WIDGET)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        .setPackage(mContext.getPackageName()),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE :
                        PendingIntent.FLAG_UPDATE_CURRENT)
    }

    open class WidgetConfig {
        val customQuery: Boolean = false
        final Class<? extends Activity> destination
        var title: String? = null
        val isLightTheme: Boolean = false
        final @LayoutRes int widgetLayout
        var section: String? = null

        @NonNull
        fun createWidgetConfig(context: Context, theme: String, section: String, query: String): WidgetConfig {
            int widgetLayout
            boolean isLightTheme = false
            if (TextUtils.equals(theme, context.getString(R.string.pref_widget_theme_value_dark))) {
                widgetLayout = R.layout.appwidget_dark
            } else if (TextUtils.equals(theme, context.getString(R.string.pref_widget_theme_value_light))) {
                widgetLayout = R.layout.appwidget_light
                isLightTheme = true
            } else {
                widgetLayout = R.layout.appwidget
            }
            String title
            Class<? extends Activity> destination
            if (!TextUtils.isEmpty(query)) {
                title = query
                section = query
                destination = SearchActivity::class.java
            } else if (TextUtils.equals(section, context.getString(R.string.pref_widget_section_value_best))) {
                title = context.getString(R.string.title_activity_best)
                destination = BestActivity::class.java
            } else if (TextUtils.equals(section, context.getString(R.string.pref_widget_section_value_top))) {
                title = context.getString(R.string.title_activity_list)
                destination = ListActivity::class.java
            } else {
                // legacy "new stories" widget
                title = context.getString(R.string.title_activity_new)
                destination = NewActivity::class.java
            }
            return WidgetConfig(destination, title, section, isLightTheme, widgetLayout)
        }

        private constructor(destination: Class<? extends Activity>, title: String, section: String, isLightTheme: Boolean, widgetLayout: Int) {
            this.destination = destination
            this.title = title
            this.section = section
            this.isLightTheme = isLightTheme
            this.widgetLayout = widgetLayout
            this.customQuery = destination == SearchActivity::class.java
        }
    }
}
