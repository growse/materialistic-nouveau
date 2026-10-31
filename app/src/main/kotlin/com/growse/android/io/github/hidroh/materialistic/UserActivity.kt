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

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.growse.android.io.github.hidroh.materialistic.KeyDelegate.RecyclerViewHelper
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServices
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager
import com.growse.android.io.github.hidroh.materialistic.data.ItemManagerQualifiers
import com.growse.android.io.github.hidroh.materialistic.data.ResponseListener
import com.growse.android.io.github.hidroh.materialistic.data.UserManager
import com.growse.android.io.github.hidroh.materialistic.widget.CommentItemDecoration
import com.growse.android.io.github.hidroh.materialistic.widget.PopupMenu
import com.growse.android.io.github.hidroh.materialistic.widget.SnappyLinearLayoutManager
import com.growse.android.io.github.hidroh.materialistic.widget.SubmissionRecyclerViewAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class UserActivity : ThemedActivity(), Scrollable {
  @Inject lateinit var userManager: UserManager
  @Inject lateinit var userServices: UserServices
  @Inject lateinit var popupMenu: PopupMenu
  @Inject lateinit var alertDialogBuilder: AlertDialogBuilder<*>
  @Inject @field:Named(ItemManagerQualifiers.HN) lateinit var itemManager: ItemManager
  @Inject lateinit var keyDelegate: KeyDelegate

  private lateinit var username: String
  private var user: UserManager.User? = null
  private lateinit var title: TextView
  private lateinit var info: TextView
  private lateinit var about: TextView
  private lateinit var recyclerView: RecyclerView
  private lateinit var tabLayout: TabLayout
  private lateinit var empty: View
  private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
  private lateinit var scrollableHelper: RecyclerViewHelper

  override val isTranslucent: Boolean
    get() = true

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val username =
        intent.getStringExtra(EXTRA_USERNAME).takeUnless { it.isNullOrEmpty() }
            ?: AppUtils.getDataUriId(intent, PARAM_ID)
    if (username.isNullOrEmpty()) {
      finish()
      return
    }
    this.username = username
    setTaskTitle(username)
    setStatusBarDim(window, true)
    setContentView(R.layout.activity_user)
    findViewById<View>(R.id.touch_outside).setOnClickListener { finish() }
    bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet))
    bottomSheetBehavior.addBottomSheetCallback(
        object : BottomSheetBehavior.BottomSheetCallback() {
          override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
              BottomSheetBehavior.STATE_HIDDEN -> finish()
              BottomSheetBehavior.STATE_EXPANDED -> {
                setStatusBarDim(window, false)
                recyclerView.suppressLayout(false)
              }
              else -> setStatusBarDim(window, true)
            }
          }

          override fun onSlide(bottomSheet: View, slideOffset: Float) {
            // no op
          }
        }
    )
    title = findViewById(R.id.title)
    title.text = username
    info = findViewById(R.id.user_info)
    about = findViewById(R.id.about)
    empty = findViewById(R.id.empty)
    tabLayout = findViewById(R.id.tab_layout)
    tabLayout.addOnTabSelectedListener(
        object : TabLayout.OnTabSelectedListener {
          override fun onTabSelected(tab: TabLayout.Tab?) {
            // no op
          }

          override fun onTabUnselected(tab: TabLayout.Tab?) {
            // no op
          }

          override fun onTabReselected(tab: TabLayout.Tab?) {
            scrollToTop()
          }
        }
    )
    recyclerView = findViewById(R.id.recycler_view)
    recyclerView.layoutManager = SnappyLinearLayoutManager(this, true)
    recyclerView.addItemDecoration(CommentItemDecoration(this))
    applyBottomInset(recyclerView)
    applyBottomInset(empty)
    scrollableHelper = RecyclerViewHelper(recyclerView, RecyclerViewHelper.SCROLL_ITEM)
    user = savedInstanceState?.let {
      BundleCompat.getParcelable(it, STATE_USER, UserManager.User::class.java)
    }
    if (user == null) {
      userManager.getUser(username, UserResponseListener(this))
    } else {
      bind()
    }
    if (!AppUtils.hasConnection(this)) {
      Snackbar.make(findViewById(R.id.content_frame), R.string.offline_notice, Snackbar.LENGTH_LONG)
          .show()
    }
  }

  override fun onStart() {
    super.onStart()
    keyDelegate.attach(this)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putParcelable(STATE_USER, user)
  }

  override fun onStop() {
    super.onStop()
    keyDelegate.detach(this)
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    keyDelegate.setScrollable(this, null)
    return keyDelegate.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
      keyDelegate.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event)

  override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean =
      keyDelegate.onKeyLongPress(keyCode, event) || super.onKeyLongPress(keyCode, event)

  override fun scrollToTop() {
    scrollableHelper.scrollToTop()
  }

  override fun scrollToNext(): Boolean = scrollableHelper.scrollToNext()

  override fun scrollToPrevious(): Boolean = scrollableHelper.scrollToPrevious()

  private fun applyBottomInset(view: View) {
    val initialPaddingBottom = view.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
      val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(
          v.paddingLeft,
          v.paddingTop,
          v.paddingRight,
          initialPaddingBottom + systemBars.bottom,
      )
      windowInsets
    }
  }

  private fun onUserLoaded(response: UserManager.User?) {
    if (response != null) {
      user = response
      bind()
    } else {
      showEmpty()
    }
  }

  private fun showEmpty() {
    info.isVisible = false
    about.isVisible = false
    empty.isVisible = true
    tabLayout.addTab(
        tabLayout
            .newTab()
            .setText(resources.getQuantityString(R.plurals.submissions_count, 0, "").trim())
    )
  }

  private fun bind() {
    val user = user ?: return
    val karma =
        SpannableString(
            KARMA.format(
                Locale.US,
                NumberFormat.getInstance(Locale.getDefault()).format(user.karma),
            )
        )
    karma.setSpan(RelativeSizeSpan(0.8f), 0, karma.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
    title.append(karma)
    info.text = getString(R.string.user_info, user.getCreated(this))
    if (user.about.isNullOrEmpty()) {
      about.isVisible = false
    } else {
      AppUtils.setTextWithLinks(about, AppUtils.fromHtml(user.about, true))
    }
    val items = user.items
    tabLayout.addTab(
        tabLayout
            .newTab()
            .setText(
                resources.getQuantityString(R.plurals.submissions_count, items.size, items.size)
            )
    )
    recyclerView.adapter =
        SubmissionRecyclerViewAdapter(
            itemManager,
            userServices,
            popupMenu,
            alertDialogBuilder,
            items,
        )
    // stay frozen until the sheet is fully expanded, see onStateChanged
    recyclerView.suppressLayout(bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED)
  }

  private class UserResponseListener(userActivity: UserActivity) :
      ResponseListener<UserManager.User?> {
    private val activity = WeakReference(userActivity)

    override fun onResponse(response: UserManager.User?) {
      activity.get()?.takeUnless { it.isActivityDestroyed }?.onUserLoaded(response)
    }

    override fun onError(errorMessage: String?) {
      activity
          .get()
          ?.takeUnless { it.isActivityDestroyed }
          ?.let { Toast.makeText(it, R.string.user_failed, Toast.LENGTH_SHORT).show() }
    }
  }

  companion object {
    @JvmField val EXTRA_USERNAME: String = UserActivity::class.java.name + ".EXTRA_USERNAME"
    private const val STATE_USER = "state:user"
    private const val PARAM_ID = "id"
    private const val KARMA = " (%1\$s)"

    @Suppress("DEPRECATION")
    private fun setStatusBarDim(window: Window, dim: Boolean) {
      window.statusBarColor =
          if (dim) {
            Color.TRANSPARENT
          } else {
            ContextCompat.getColor(
                window.context,
                AppUtils.getThemedResId(window.context, R.attr.colorPrimaryDark),
            )
          }
    }
  }
}
