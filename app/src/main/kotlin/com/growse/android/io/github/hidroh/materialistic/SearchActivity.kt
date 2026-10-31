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

import android.app.SearchManager
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import android.view.Menu
import android.view.MenuItem
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.growse.android.io.github.hidroh.materialistic.data.AlgoliaClient
import com.growse.android.io.github.hidroh.materialistic.data.HackerNewsClient
import com.growse.android.io.github.hidroh.materialistic.data.SearchRecentSuggestionsProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchActivity : BaseListActivity() {
  private var query = ""

  override val defaultTitle: String
    get() = getString(R.string.title_activity_search)

  override fun onCreate(savedInstanceState: Bundle?) {
    // read before super.onCreate(), which instantiates the list fragment from it
    query = intent.getStringExtra(SearchManager.QUERY) ?: ""
    super.onCreate(savedInstanceState)
    val query = query
    if (query.isNotEmpty()) {
      supportActionBar.subtitle = query
      object :
              SearchRecentSuggestions(
                  this,
                  SearchRecentSuggestionsProvider.PROVIDER_AUTHORITY,
                  SearchRecentSuggestionsProvider.MODE,
              ) {
            override fun saveRecentQuery(queryString: String?, line2: String?) {
              truncateHistory(contentResolver, MAX_RECENT_SUGGESTIONS - 1)
              super.saveRecentQuery(queryString, line2)
            }
          }
          .saveRecentQuery(query, null)
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_sort, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    val checked = if (AlgoliaClient.sSortByTime) R.id.menu_sort_recent else R.id.menu_sort_popular
    menu.findItem(checked).isChecked = true
    return super.onPrepareOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.groupId == R.id.menu_sort_group) {
      item.isChecked = true
      sort(item.itemId == R.id.menu_sort_recent)
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  override fun instantiateListFragment(): Fragment {
    val itemManager =
        if (query.isEmpty()) HackerNewsClient::class.java else AlgoliaClient::class.java
    return ListFragment().apply {
      arguments =
          bundleOf(
              ListFragment.EXTRA_FILTER to query,
              ListFragment.EXTRA_ITEM_MANAGER to itemManager.name,
          )
    }
  }

  private fun sort(byTime: Boolean) {
    if (AlgoliaClient.sSortByTime == byTime) {
      return
    }
    AlgoliaClient.sSortByTime = byTime
    Preferences.setSortByRecent(this, byTime)
    (supportFragmentManager.findFragmentByTag(LIST_FRAGMENT_TAG) as ListFragment?)?.filter(query)
  }

  companion object {
    private const val MAX_RECENT_SUGGESTIONS = 10
  }
}
