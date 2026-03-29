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
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.RelativeSizeSpan
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout

import java.lang.ref.WeakReference
import java.text.NumberFormat
import java.util.Locale

import javax.inject.Inject
import javax.inject.Named

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.recyclerview.widget.RecyclerView
import io.github.hidroh.materialistic.annotation.Synthetic
import io.github.hidroh.materialistic.data.ItemManager
import io.github.hidroh.materialistic.data.ResponseListener
import io.github.hidroh.materialistic.data.UserManager
import io.github.hidroh.materialistic.widget.CommentItemDecoration
import io.github.hidroh.materialistic.widget.SnappyLinearLayoutManager
import io.github.hidroh.materialistic.widget.SubmissionRecyclerViewAdapter

open class UserActivity : InjectableActivity(), Scrollable {
    val EXTRA_USERNAME: String = UserActivity::class.java.name + ".EXTRA_USERNAME"
    private const val STATE_USER: String = "state:user"
    private const val PARAM_ID: String = "id"
    private const val KARMA: String = " (%1$s)"
    @Inject var mUserManager: UserManager? = null
    @Inject @Named(ActivityModule.HN) var mItemManger: ItemManager? = null
    @Inject var mKeyDelegate: KeyDelegate? = null
    private var mScrollableHelper: KeyDelegate.RecyclerViewHelper? = null
    private var mUsername: String? = null
    private var mUser: UserManager.User? = null
    private var mTitle: TextView? = null
    private var mInfo: TextView? = null
    private var mAbout: TextView? = null
    var mRecyclerView: RecyclerView? = null
    private var mTabLayout: TabLayout? = null
    private var mEmpty: View? = null
    private var mBottomSheetBehavior: BottomSheetBehavior<View>? = null

    @SuppressWarnings("ConstantConditions")
    protected override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        mUsername = getIntent().getStringExtra(EXTRA_USERNAME)
        if (TextUtils.isEmpty(mUsername)) {
            mUsername = AppUtils.getDataUriId(getIntent(), PARAM_ID)
        }
        if (TextUtils.isEmpty(mUsername)) {
            finish()
            return
        }
        setTaskTitle(mUsername)
        AppUtils.setStatusBarDim(getWindow(), true)
        setContentView(R.layout.activity_user)
        findViewById(R.id.touch_outside).setOnClickListener(v -> finish())
        mBottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet))
        mBottomSheetBehavior.setBottomSheetCallback(BottomSheetBehavior.BottomSheetCallback() {
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        finish()
                        break
                    case BottomSheetBehavior.STATE_EXPANDED:
                        AppUtils.setStatusBarDim(getWindow(), false)
                        mRecyclerView.setLayoutFrozen(false)
                        break
                    case BottomSheetBehavior.STATE_COLLAPSED:
                    case BottomSheetBehavior.STATE_DRAGGING:
                    case BottomSheetBehavior.STATE_HALF_EXPANDED:
                    case BottomSheetBehavior.STATE_SETTLING:
                    default:
                        AppUtils.setStatusBarDim(getWindow(), true)
                        break
                }
            }

            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // no op
            }
        })
        mTitle = (TextView) findViewById(R.id.title)
        mTitle.setText(mUsername)
        mInfo = (TextView) findViewById(R.id.user_info)
        mAbout = (TextView) findViewById(R.id.about)
        mEmpty = findViewById(R.id.empty)
        mTabLayout = (TabLayout) findViewById(R.id.tab_layout)
        mTabLayout.addOnTabSelectedListener(TabLayout.OnTabSelectedListener() {
            public void onTabSelected(TabLayout.Tab tab) {
                // no op
            }

            public void onTabUnselected(TabLayout.Tab tab) {
                // no op
            }

            public void onTabReselected(TabLayout.Tab tab) {
                scrollToTop()
            }
        })
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view)
        mRecyclerView.setLayoutManager(SnappyLinearLayoutManager(this, true))
        mRecyclerView.addItemDecoration(CommentItemDecoration(this))
        mScrollableHelper = KeyDelegate.RecyclerViewHelper(mRecyclerView,
                KeyDelegate.RecyclerViewHelper.SCROLL_ITEM)
        if (savedInstanceState != null) {
            mUser = savedInstanceState.getParcelable(STATE_USER)
        }
        if (mUser == null) {
            load()
        } else {
            bind()
        }
        if (!AppUtils.hasConnection(this)) {
            Snackbar.make(findViewById(R.id.content_frame),
                    R.string.offline_notice, Snackbar.LENGTH_LONG)
                    .show()
        }
    }

    protected override fun onStart() {
        super.onStart()
        mKeyDelegate.attach(this)
    }

    protected override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_USER, mUser)
    }

    protected override fun onStop() {
        super.onStop()
        mKeyDelegate.detach(this)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        mKeyDelegate.setScrollable(this, null)
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

    override fun scrollToTop() {
        mScrollableHelper.scrollToTop()
    }

    override fun scrollToNext(): Boolean {
        return mScrollableHelper.scrollToNext()
    }

    override fun scrollToPrevious(): Boolean {
        return mScrollableHelper.scrollToPrevious()
    }

    protected override fun isTranslucent(): Boolean {
        return true
    }

    private fun load() {
        mUserManager.getUser(mUsername, UserResponseListener(this))
    }

    @Synthetic
    fun onUserLoaded(response: UserManager.User) {
        if (response != null) {
            mUser = response
            bind()
        } else {
            showEmpty()
        }
    }

    private fun showEmpty() {
        mInfo.setVisibility(View.GONE)
        mAbout.setVisibility(View.GONE)
        mEmpty.setVisibility(View.VISIBLE)
        mTabLayout.addTab(mTabLayout.newTab()
                .setText(getResources().getQuantityString(R.plurals.submissions_count, 0, "").trim()))
    }

    private fun bind() {
        val karma = SpannableString(String.format(Locale.US, KARMA,
                NumberFormat.getInstance(Locale.getDefault()).format(mUser.getKarma())))
        karma.setSpan(RelativeSizeSpan(0.8f), 0, karma.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        mTitle.append(karma)
        mInfo.setText(getString(R.string.user_info, mUser.getCreated(this)))
        if (TextUtils.isEmpty(mUser.getAbout())) {
            mAbout.setVisibility(View.GONE)
        } else {
            AppUtils.setTextWithLinks(mAbout, AppUtils.fromHtml(mUser.getAbout(), true))
        }
        int count = mUser.getItems().length
        mTabLayout.addTab(mTabLayout.newTab()
                .setText(getResources().getQuantityString(R.plurals.submissions_count, count, count)))
        mRecyclerView.setAdapter(SubmissionRecyclerViewAdapter(mItemManger, mUser.getItems()))
        mRecyclerView.setLayoutFrozen(mBottomSheetBehavior.getState() !=
                BottomSheetBehavior.STATE_EXPANDED)
    }

    open class UserResponseListener : ResponseListener<UserManager.User> {
        private var mUserActivity: WeakReference<UserActivity>? = null

        @Synthetic
        constructor(userActivity: UserActivity) {
            mUserActivity = WeakReference<>(userActivity)
        }

        override fun onResponse(response: UserManager.User) {
            if (mUserActivity.get() != null && !mUserActivity.get().isActivityDestroyed()) {
                mUserActivity.get().onUserLoaded(response)
            }
        }

        override fun onError(errorMessage: String) {
            if (mUserActivity.get() != null && !mUserActivity.get().isActivityDestroyed()) {
                Toast.makeText(mUserActivity.get(), R.string.user_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
