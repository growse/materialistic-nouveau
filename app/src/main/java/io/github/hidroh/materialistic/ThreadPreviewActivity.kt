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

import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar
import android.view.KeyEvent
import android.view.MenuItem
import android.view.Window

import javax.inject.Inject
import javax.inject.Named

import io.github.hidroh.materialistic.data.Item
import io.github.hidroh.materialistic.data.ItemManager
import io.github.hidroh.materialistic.widget.CommentItemDecoration
import io.github.hidroh.materialistic.widget.SnappyLinearLayoutManager
import io.github.hidroh.materialistic.widget.ThreadPreviewRecyclerViewAdapter

open class ThreadPreviewActivity : InjectableActivity() {
    val EXTRA_ITEM: String = ThreadPreviewActivity::class.java.name + ".EXTRA_ITEM"

    @Inject @Named(ActivityModule.HN) var mItemManager: ItemManager? = null
    @Inject var mKeyDelegate: KeyDelegate? = null

    protected override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        val item = getIntent().getParcelableExtra(EXTRA_ITEM)
        if (item == null) {
            finish()
            return
        }
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_thread_preview)
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar))
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP)
        val recyclerView = (RecyclerView) findViewById(R.id.recycler_view)
        recyclerView.setLayoutManager(SnappyLinearLayoutManager(this, false))
        recyclerView.addItemDecoration(CommentItemDecoration(this))
        recyclerView.setAdapter(ThreadPreviewRecyclerViewAdapter(mItemManager, item))
        mKeyDelegate.setScrollable(
                KeyDelegate.RecyclerViewHelper(recyclerView,
                        KeyDelegate.RecyclerViewHelper.SCROLL_ITEM), null)
    }

    protected override fun onStart() {
        super.onStart()
        mKeyDelegate.attach(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    protected override fun onStop() {
        super.onStop()
        mKeyDelegate.detach(this)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return mKeyDelegate.onKeyDown(keyCode, event) ||
                super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return mKeyDelegate.onKeyUp(keyCode, event) ||
                super.onKeyUp(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        return mKeyDelegate.onKeyLongPress(keyCode, event) ||
                super.onKeyLongPress(keyCode, event)
    }

    protected override fun isDialogTheme(): Boolean {
        return true
    }
}
