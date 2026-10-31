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
package com.growse.android.io.github.hidroh.materialistic

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.growse.android.io.github.hidroh.materialistic.ListFragment.RefreshCallback
import com.growse.android.io.github.hidroh.materialistic.data.HackerNewsClient
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager.FetchMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
abstract class BaseStoriesActivity : BaseListActivity(), RefreshCallback {
  private var lastUpdated: Long? = null
  private val handler = Handler(Looper.getMainLooper())

  /** Shows when the list was last refreshed as the subtitle, updating it every minute. */
  private val lastUpdateTask: Runnable =
      object : Runnable {
        override fun run() {
          val lastUpdated = lastUpdated ?: return
          if (AppUtils.hasConnection(this@BaseStoriesActivity)) {
            supportActionBar.subtitle =
                getString(
                    R.string.last_updated,
                    DateUtils.getRelativeTimeSpanString(
                        lastUpdated,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL,
                    ),
                )
            handler.postDelayed(this, DateUtils.MINUTE_IN_MILLIS)
          } else {
            supportActionBar.setSubtitle(R.string.offline)
          }
        }
      }

  @get:FetchMode protected abstract val fetchMode: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState?.containsKey(STATE_LAST_UPDATED) == true) {
      lastUpdated = savedInstanceState.getLong(STATE_LAST_UPDATED)
    }
  }

  override fun onResume() {
    super.onResume()
    restartLastUpdateTask()
  }

  override fun onPause() {
    super.onPause()
    handler.removeCallbacks(lastUpdateTask)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    lastUpdated?.let { outState.putLong(STATE_LAST_UPDATED, it) }
  }

  override fun onRefreshed() {
    onItemSelected(null)
    lastUpdated = System.currentTimeMillis()
    restartLastUpdateTask()
  }

  override fun instantiateListFragment(): Fragment =
      ListFragment().apply {
        arguments =
            bundleOf(
                ListFragment.EXTRA_ITEM_MANAGER to HackerNewsClient::class.java.name,
                ListFragment.EXTRA_FILTER to fetchMode,
            )
      }

  private fun restartLastUpdateTask() {
    handler.removeCallbacks(lastUpdateTask)
    handler.post(lastUpdateTask)
  }

  companion object {
    private const val STATE_LAST_UPDATED = "state:lastUpdated"
  }
}
