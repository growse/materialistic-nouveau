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

import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.ContextCompat
import com.growse.android.io.github.hidroh.materialistic.AppUtils
import com.growse.android.io.github.hidroh.materialistic.AppUtils.createItemUri
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.data.Item
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager
import com.growse.android.io.github.hidroh.materialistic.data.ItemManagerQualifiers
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.min

@AndroidEntryPoint
class WidgetService : RemoteViewsService() {
  @JvmField @Inject @Named(ItemManagerQualifiers.HN) var mItemManager: ItemManager? = null

  @JvmField @Inject @Named(ItemManagerQualifiers.ALGOLIA) var mSearchManager: ItemManager? = null

  override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
    return ListRemoteViewsFactory(
        applicationContext,
        (if (
            intent.getBooleanExtra(
                EXTRA_CUSTOM_QUERY,
                false,
            )
        )
            mSearchManager
        else mItemManager)!!,
        intent.getStringExtra(EXTRA_SECTION) ?: "",
        intent.getBooleanExtra(EXTRA_LIGHT_THEME, false),
    )
  }

  internal class ListRemoteViewsFactory(
      private val mContext: Context,
      private val mItemManager: ItemManager,
      section: String,
      private val mLightTheme: Boolean,
  ) : RemoteViewsFactory {
    private val mFilter: String
    private val mHotThreshold: Int
    private var mItems: Array<Item?>? = null

    init {
      if (
          TextUtils.equals(
              section,
              mContext.getString(R.string.pref_widget_section_value_best),
          )
      ) {
        mFilter = ItemManager.BEST_FETCH_MODE
        mHotThreshold = AppUtils.HOT_THRESHOLD_HIGH
      } else if (
          TextUtils.equals(
              section,
              mContext.getString(R.string.pref_widget_section_value_top),
          )
      ) {
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
      return if (mItems != null) min(mItems!!.size, MAX_ITEMS) else 0
    }

    override fun getViewAt(position: Int): RemoteViews {
      val remoteViews =
          RemoteViews(
              mContext.packageName,
              if (mLightTheme) R.layout.item_widget_light else R.layout.item_widget,
          )
      val item = getItem(position) ?: return remoteViews
      if (!isItemAvailable(item)) {
        val remoteItem = mItemManager.getItem(item.id, ItemManager.MODE_NETWORK)
        if (remoteItem != null) {
          item.populate(remoteItem)
        } else {
          return remoteViews
        }
      }
      remoteViews.setTextViewText(R.id.title, item.displayedTitle)
      remoteViews.setTextViewText(
          R.id.score,
          SpannableStringBuilder()
              .append(getSpan(item.score, SCORE, mHotThreshold * AppUtils.HOT_FACTOR))
              .append(SUBTITLE_SEPARATOR)
              .append(getSpan(item.kidCount, COMMENT, mHotThreshold)),
      )
      remoteViews.setOnClickFillInIntent(
          R.id.item_view,
          Intent().setData(createItemUri(item.id)),
      )
      return remoteViews
    }

    override fun getLoadingView(): RemoteViews {
      return RemoteViews(mContext.packageName, R.layout.item_widget)
    }

    override fun getViewTypeCount(): Int {
      return 1
    }

    override fun getItemId(position: Int): Long {
      val item = getItem(position)
      return item?.longId ?: 0L
    }

    override fun hasStableIds(): Boolean {
      return true
    }

    private fun getItem(position: Int): Item? {
      return if (mItems != null && position < mItems!!.size) mItems!![position] else null
    }

    private fun isItemAvailable(item: Item?): Boolean {
      return item != null && item.localRevision > 0
    }

    private fun getSpan(value: Int, format: String, hotThreshold: Int): SpannableString {
      val text = String.format(Locale.US, format, value)
      val spannable = SpannableString(text)
      if (value >= hotThreshold) {
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(mContext, R.color.orange500)),
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
      }
      return spannable
    }

    companion object {
      private const val SCORE = $$"%1$dp"
      private const val COMMENT = $$"%1$dc"
      private const val SUBTITLE_SEPARATOR = " - "
      private const val MAX_ITEMS = 10
    }
  }

  companion object {
    const val EXTRA_SECTION: String = "extra:section"
    const val EXTRA_LIGHT_THEME: String = "extra:lightTheme"
    const val EXTRA_CUSTOM_QUERY: String = "extra:customQuery"
  }
}
