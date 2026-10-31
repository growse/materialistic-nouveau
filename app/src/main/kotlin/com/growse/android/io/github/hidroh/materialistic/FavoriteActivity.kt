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
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.growse.android.io.github.hidroh.materialistic.data.FavoriteManager.Companion.isCleared
import com.growse.android.io.github.hidroh.materialistic.data.FavoriteManager.Companion.isRemoved
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager
import com.growse.android.io.github.hidroh.materialistic.data.MaterialisticDatabase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FavoriteActivity : BaseListActivity() {
  private val mObserver = Observer { uri: Uri? ->
    if (uri == null) {
      return@Observer
    }
    if (isRemoved(uri)) {
      val selected = selectedItem
      if (selected != null && TextUtils.equals(selected.id, uri.lastPathSegment)) {
        onItemSelected(null)
      }
    } else if (isCleared(uri)) {
      onItemSelected(null)
    }
  }
  private var mFilter = ""

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) {
      mFilter = savedInstanceState.getString(STATE_FILTER) ?: ""
      supportActionBar.subtitle = mFilter
    }
    MaterialisticDatabase.getInstance(this).liveData.observe(this, mObserver)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    if (!intent.hasExtra(SearchManager.QUERY)) {
      return
    }
    onItemSelected(null)
    mFilter = intent.getStringExtra(SearchManager.QUERY) ?: ""
    if (TextUtils.equals(mFilter, EMPTY_QUERY)) {
      mFilter = ""
    }
    supportActionBar.subtitle = mFilter
    val fragment = supportFragmentManager.findFragmentByTag(LIST_FRAGMENT_TAG) as FavouriteFragment?
    fragment?.filter(mFilter)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(STATE_FILTER, mFilter)
  }

  override val defaultTitle: String
    get() = getString(R.string.title_activity_favorite)

  override fun instantiateListFragment(): Fragment {
    val args = Bundle()
    args.putString(FavouriteFragment.EXTRA_FILTER, mFilter)
    return Fragment.instantiate(this, FavouriteFragment::class.java.name, args)
  }

  override val isSearchable: Boolean
    get() = false

  override val itemCacheMode: Int
    get() = ItemManager.MODE_CACHE

  companion object {
    val EMPTY_QUERY: String = MaterialisticDatabase::class.java.name
    private const val STATE_FILTER = "state:filter"
  }
}
