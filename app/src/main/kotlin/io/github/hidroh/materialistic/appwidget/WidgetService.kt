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
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.widget.RemoteViews
import android.widget.RemoteViewsService

import java.util.Locale

import javax.inject.Inject
import javax.inject.Named

import io.github.hidroh.materialistic.ActivityModule
import io.github.hidroh.materialistic.AppUtils
import io.github.hidroh.materialistic.Application
import io.github.hidroh.materialistic.Injectable
import io.github.hidroh.materialistic.R
import io.github.hidroh.materialistic.data.Item
import io.github.hidroh.materialistic.data.ItemManager

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
open class WidgetService : RemoteViewsService() {
    const val EXTRA_SECTION: String = "extra:section"
    const val EXTRA_LIGHT_THEME: String = "extra:lightTheme"
    const val EXTRA_CUSTOM_QUERY: String = "extra:customQuery"
    @Inject @Named(ActivityModule.HN) var mItemManager: ItemManager? = null
    @Inject @Named(ActivityModule.ALGOLIA) var mSearchManager: ItemManager? = null

    override fun onCreate() {
        super.onCreate()
        ((Injectable) getApplication())
                .getApplicationGraph()
                .plus(ActivityModule(this))
                .inject(this)
    }

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ListRemoteViewsFactory(getApplicationContext(),
                intent.getBooleanExtra(EXTRA_CUSTOM_QUERY, false) ? mSearchManager : mItemManager,
                intent.getStringExtra(EXTRA_SECTION),
                intent.getBooleanExtra(EXTRA_LIGHT_THEME, false))
    }

    open class ListRemoteViewsFactory : RemoteViewsFactory {

        private const val SCORE: String = "%1$dp"
        private const val COMMENT: String = "%1$dc"
        private const val SUBTITLE_SEPARATOR: String = " - "
        private const val MAX_ITEMS: Int = 10
        private var mContext: Context? = null
        private var mItemManager: ItemManager? = null
        private var mFilter: String? = null
        private val mLightTheme: Boolean = false
        private val mHotThreshold: Int = 0
        private var mItems: Array<Item>? = null

        constructor(context: Context, itemManager: ItemManager, section: String, lightTheme: Boolean) {
            mContext = context
            mItemManager = itemManager
            mLightTheme = lightTheme
            if (TextUtils.equals(section,
                    context.getString(R.string.pref_widget_section_value_best))) {
                mFilter = ItemManager.BEST_FETCH_MODE
                mHotThreshold = AppUtils.HOT_THRESHOLD_HIGH
            } else if (TextUtils.equals(section,
                    context.getString(R.string.pref_widget_section_value_top))) {
                mFilter = ItemManager.TOP_FETCH_MODE
                mHotThreshold = AppUtils.HOT_THRESHOLD_NORMAL
            } else {
                mFilter = section
                mHotThreshold = AppUtils.HOT_THRESHOLD_NORMAL
            }
        }

        override fun onCreate() {
            // no op
        }

        override fun onDataSetChanged() {
            mItems = mItemManager.getStories(mFilter, ItemManager.MODE_NETWORK)
        }

        override fun onDestroy() {
            // no op
        }

        override fun getCount(): Int {
            return mItems != null ? Math.min(mItems.length, MAX_ITEMS) : 0
        }

        override fun getViewAt(position: Int): RemoteViews {
            val remoteViews = RemoteViews(mContext.getPackageName(),
                    mLightTheme ? R.layout.item_widget_light : R.layout.item_widget)
            val item = getItem(position)
            if (item == null) {
                return remoteViews
            }
            if (!isItemAvailable(item)) {
                val remoteItem = mItemManager.getItem(item.getId(), ItemManager.MODE_NETWORK)
                if (remoteItem != null) {
                    item.populate(remoteItem)
                } else {
                    return remoteViews
                }
            }
            remoteViews.setTextViewText(R.id.title, item.getDisplayedTitle())
            remoteViews.setTextViewText(R.id.score, SpannableStringBuilder()
                    .append(getSpan(item.getScore(), SCORE, mHotThreshold * AppUtils.HOT_FACTOR))
                    .append(SUBTITLE_SEPARATOR)
                    .append(getSpan(item.getKidCount(), COMMENT, mHotThreshold)))
            remoteViews.setOnClickFillInIntent(R.id.item_view, Intent().setData(
                    AppUtils.createItemUri(item.getId())))
            return remoteViews
        }

        override fun getLoadingView(): RemoteViews {
            return RemoteViews(mContext.getPackageName(), R.layout.item_widget)
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun getItemId(position: Int): Long {
            val item = getItem(position)
            return item != null ? item.getLongId() : 0L
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        private fun getItem(position: Int): Item {
            return mItems != null && position < mItems.length ? mItems[position] : null
        }

        private fun isItemAvailable(item: Item): Boolean {
            return item != null && item.getLocalRevision() > 0
        }

        private fun getSpan(value: Int, format: String, hotThreshold: Int): SpannableString {
            val text = String.format(Locale.US, format, value)
            val spannable = SpannableString(text)
            if (value >= hotThreshold) {
                spannable.setSpan(ForegroundColorSpan(
                                ContextCompat.getColor(mContext, R.color.orange500)),
                        0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            return spannable
        }
    }
}
