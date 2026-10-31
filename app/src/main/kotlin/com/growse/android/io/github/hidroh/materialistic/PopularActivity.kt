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
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.growse.android.io.github.hidroh.materialistic.Preferences.getPopularRange
import com.growse.android.io.github.hidroh.materialistic.Preferences.setPopularRange
import com.growse.android.io.github.hidroh.materialistic.data.AlgoliaPopularClient
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PopularActivity : BaseListActivity() {
  private var mRange: String? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) {
      setRange(savedInstanceState.getString(STATE_RANGE)!!)
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_popular, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_range_day -> {
        filter(AlgoliaPopularClient.LAST_24H)
        return true
      }
      R.id.menu_range_week -> {
        filter(AlgoliaPopularClient.PAST_WEEK)
        return true
      }
      R.id.menu_range_month -> {
        filter(AlgoliaPopularClient.PAST_MONTH)
        return true
      }
      R.id.menu_range_year -> {
        filter(AlgoliaPopularClient.PAST_YEAR)
        return true
      }
      else -> return super.onOptionsItemSelected(item)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(STATE_RANGE, mRange)
  }

  override val defaultTitle: String
    get() = getString(R.string.title_activity_popular)

  override fun instantiateListFragment(): Fragment {
    val args = Bundle()
    setRange(getPopularRange(this))
    args.putString(ListFragment.EXTRA_FILTER, mRange)
    args.putString(ListFragment.EXTRA_ITEM_MANAGER, AlgoliaPopularClient::class.java.name)
    return Fragment.instantiate(this, ListFragment::class.java.name, args)
  }

  override val isSearchable: Boolean
    get() = false

  private fun filter(@AlgoliaPopularClient.Range range: String) {
    setRange(range)
    setPopularRange(this, range)
    val listFragment = supportFragmentManager.findFragmentByTag(LIST_FRAGMENT_TAG) as ListFragment?
    listFragment?.filter(range)
  }

  private fun setRange(range: String) {
    mRange = range
    val stringRes: Int =
        when (range) {
          AlgoliaPopularClient.LAST_24H -> R.string.popular_range_last_24h
          AlgoliaPopularClient.PAST_WEEK -> R.string.popular_range_past_week
          AlgoliaPopularClient.PAST_MONTH -> R.string.popular_range_past_month
          AlgoliaPopularClient.PAST_YEAR -> R.string.popular_range_past_year
          else -> R.string.popular_range_last_24h
        }
    supportActionBar.setSubtitle(stringRes)
  }

  companion object {
    private const val STATE_RANGE = "state:range"
  }
}
