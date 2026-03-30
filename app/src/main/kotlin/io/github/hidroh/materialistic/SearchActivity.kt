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

package io.github.hidroh.materialistic

import android.app.SearchManager
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import androidx.fragment.app.Fragment
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem

import io.github.hidroh.materialistic.data.AlgoliaClient
import io.github.hidroh.materialistic.data.HackerNewsClient
import io.github.hidroh.materialistic.data.SearchRecentSuggestionsProvider

open class SearchActivity : BaseListActivity() {

    private const val MAX_RECENT_SUGGESTIONS: Int = 10
    private var mQuery: String? = null

    protected override fun onCreate(savedInstanceState: Bundle) {
        if (getIntent().hasExtra(SearchManager.QUERY)) {
            mQuery = getIntent().getStringExtra(SearchManager.QUERY)
        }
        super.onCreate(savedInstanceState)
        if (!TextUtils.isEmpty(mQuery)) {
            getSupportActionBar().setSubtitle(mQuery)
            val suggestions = SearchRecentSuggestions(this,
                    SearchRecentSuggestionsProvider.PROVIDER_AUTHORITY,
                    SearchRecentSuggestionsProvider.MODE) {
                public void saveRecentQuery(String queryString, String line2) {
                    truncateHistory(getContentResolver(), MAX_RECENT_SUGGESTIONS - 1)
                    super.saveRecentQuery(queryString, line2)
                }
            }
            suggestions.saveRecentQuery(mQuery, null)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        getMenuInflater().inflate(R.menu.menu_sort, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(AlgoliaClient.sSortByTime ? R.id.menu_sort_recent : R.id.menu_sort_popular)
                .setChecked(true)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getGroupId() == R.id.menu_sort_group) {
            item.setChecked(true)
            sort(item.getItemId() == R.id.menu_sort_recent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    protected override fun getDefaultTitle(): String {
        return getString(R.string.title_activity_search)
    }

    protected override fun instantiateListFragment(): Fragment {
        val args = Bundle()
        args.putString(ListFragment.EXTRA_FILTER, mQuery)
        if (TextUtils.isEmpty(mQuery)) {
            args.putString(ListFragment.EXTRA_ITEM_MANAGER, HackerNewsClient::class.java.getName())
        } else {
            args.putString(ListFragment.EXTRA_ITEM_MANAGER, AlgoliaClient::class.java.getName())
        }
        return Fragment.instantiate(this, ListFragment::class.java.getName(), args)
    }

    private fun sort(byTime: Boolean) {
        if (AlgoliaClient.sSortByTime == byTime) {
            return
        }
        AlgoliaClient.sSortByTime = byTime
        Preferences.setSortByRecent(this, byTime)
        val listFragment = (ListFragment) getSupportFragmentManager()
                .findFragmentByTag(LIST_FRAGMENT_TAG)
        if (listFragment != null) {
            listFragment.filter(mQuery)
        }
    }
}
