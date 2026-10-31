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
import android.view.KeyEvent
import android.view.MenuItem
import android.view.Window
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.content.IntentCompat
import androidx.recyclerview.widget.RecyclerView
import com.growse.android.io.github.hidroh.materialistic.KeyDelegate.RecyclerViewHelper
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServices
import com.growse.android.io.github.hidroh.materialistic.data.Item
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager
import com.growse.android.io.github.hidroh.materialistic.data.ItemManagerQualifiers
import com.growse.android.io.github.hidroh.materialistic.widget.CommentItemDecoration
import com.growse.android.io.github.hidroh.materialistic.widget.PopupMenu
import com.growse.android.io.github.hidroh.materialistic.widget.SnappyLinearLayoutManager
import com.growse.android.io.github.hidroh.materialistic.widget.ThreadPreviewRecyclerViewAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class ThreadPreviewActivity : ThemedActivity() {
  @Inject @Named(ItemManagerQualifiers.HN) lateinit var itemManager: ItemManager
  @Inject lateinit var keyDelegate: KeyDelegate
  @Inject lateinit var userServices: UserServices
  @Inject lateinit var popupMenu: PopupMenu
  @Inject lateinit var alertDialogBuilder: AlertDialogBuilder<*>

  override val isDialogTheme: Boolean
    get() = true

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val item = IntentCompat.getParcelableExtra(intent, EXTRA_ITEM, Item::class.java)
    if (item == null) {
      finish()
      return
    }
    supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
    setContentView(R.layout.activity_thread_preview)
    setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
    supportActionBar?.displayOptions =
        ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_SHOW_TITLE or ActionBar.DISPLAY_HOME_AS_UP
    val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
    recyclerView.layoutManager = SnappyLinearLayoutManager(this, false)
    recyclerView.addItemDecoration(CommentItemDecoration(this))
    recyclerView.adapter =
        ThreadPreviewRecyclerViewAdapter(
            itemManager,
            userServices,
            popupMenu,
            alertDialogBuilder,
            item,
        )
    keyDelegate.setScrollable(
        RecyclerViewHelper(recyclerView, RecyclerViewHelper.SCROLL_ITEM),
        null,
    )
  }

  override fun onStart() {
    super.onStart()
    keyDelegate.attach(this)
  }

  override fun onStop() {
    super.onStop()
    keyDelegate.detach(this)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      finish()
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
      keyDelegate.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)

  override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
      keyDelegate.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event)

  override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean =
      keyDelegate.onKeyLongPress(keyCode, event) || super.onKeyLongPress(keyCode, event)

  companion object {
    @JvmField val EXTRA_ITEM: String = ThreadPreviewActivity::class.java.name + ".EXTRA_ITEM"
  }
}
