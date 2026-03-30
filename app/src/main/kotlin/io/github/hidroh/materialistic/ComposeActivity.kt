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

import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

import java.lang.ref.WeakReference

import javax.inject.Inject

import io.github.hidroh.materialistic.accounts.UserServices
import io.github.hidroh.materialistic.annotation.Synthetic

open class ComposeActivity : InjectableActivity() {
    val EXTRA_PARENT_ID: String = ComposeActivity::class.java.name + ".EXTRA_PARENT_ID"
    val EXTRA_PARENT_TEXT: String = ComposeActivity::class.java.name + ".EXTRA_PARENT_TEXT"
    private const val HN_FORMAT_DOC_URL: String = "https://news.ycombinator.com/formatdoc"
    private const val FORMAT_QUOTE: String = "> %s\n\n"
    private const val PARAGRAPH_QUOTE: String = "\n\n> "
    private const val PARAGRAPH_BREAK_REGEX: String = "[\\n]{2,}"
    @Inject var mUserServices: UserServices? = null
    @Inject var mAlertDialogBuilder: AlertDialogBuilder? = null
    private var mEditText: EditText? = null
    private var mParentText: String? = null
    private var mQuoteText: String? = null
    private var mParentId: String? = null
    private var mSending: Boolean = false

    protected override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        mParentId = getIntent().getStringExtra(EXTRA_PARENT_ID)
        if (TextUtils.isEmpty(mParentId)) {
            finish()
            return
        }
        AppUtils.setStatusBarColor(getWindow(), ContextCompat.getColor(this, R.color.blackT12))
        setContentView(R.layout.activity_compose)
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar))
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_HOME_AS_UP)
        mEditText = (EditText) findViewById(R.id.edittext_body)
        if (savedInstanceState == null) {
            mEditText.setText(Preferences.getDraft(this, mParentId))
        }
        findViewById(R.id.empty).setOnClickListener(v -> mEditText.requestFocus())
        findViewById(R.id.empty).setOnLongClickListener(v -> {
            mEditText.requestFocus()
            return mEditText.performLongClick()
        })
        mParentText = getIntent().getStringExtra(EXTRA_PARENT_TEXT)
        if (!TextUtils.isEmpty(mParentText)) {
            findViewById(R.id.quote).setVisibility(View.VISIBLE)
            val toggle = (TextView) findViewById(R.id.toggle)
            val textView = (TextView) findViewById(R.id.text)
            AppUtils.setTextWithLinks(textView, AppUtils.fromHtml(mParentText))
            toggle.setOnClickListener(v -> {
                if (textView.getVisibility() == View.VISIBLE) {
                    toggle.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                            R.drawable.ic_expand_more_white_24dp, 0)
                    textView.setVisibility(View.GONE)

                } else {
                    toggle.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                            R.drawable.ic_expand_less_white_24dp, 0)
                    textView.setVisibility(View.VISIBLE)
                }
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        getMenuInflater().inflate(R.menu.menu_compose, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_quote).setVisible(!mSending && !TextUtils.isEmpty(mParentText))
        menu.findItem(R.id.menu_send).setEnabled(!mSending)
        menu.findItem(R.id.menu_save_draft).setEnabled(!mSending)
        menu.findItem(R.id.menu_discard_draft).setEnabled(!mSending)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == R.id.menu_send) {
            if (mEditText.length() == 0) {
                Toast.makeText(this, R.string.comment_required, Toast.LENGTH_SHORT).show()
                return false
            } else {
                send()
                return true
            }
        }
        if (item.getItemId() == R.id.menu_quote) {
            mEditText.getEditableText().insert(0, createQuote())
        }
        if (item.getItemId() == android.R.id.home) {
            onBackPressed()
            return true
        }
        if (item.getItemId() == R.id.menu_save_draft) {
            Preferences.saveDraft(this, mParentId, mEditText.getText().toString())
            return true
        }
        if (item.getItemId() == R.id.menu_discard_draft) {
            Preferences.deleteDraft(this, mParentId)
            return true
        }
        if (item.getItemId() == R.id.menu_guidelines) {
            val webView = WebView(ComposeActivity.this)
            webView.loadUrl(HN_FORMAT_DOC_URL)
            mAlertDialogBuilder
                    .init(ComposeActivity.this)
                    .setView(webView)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (mEditText.length() == 0 || mSending ||
                TextUtils.equals(Preferences.getDraft(this, mParentId), mEditText.getText().toString())) {
            super.onBackPressed()
            return
        }
        mAlertDialogBuilder
                .init(this)
                .setMessage(R.string.confirm_save_draft)
                .setNegativeButton(android.R.string.no, (dialog, which) ->
                        ComposeActivity.super.onBackPressed())
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        Preferences.saveDraft(this, mParentId, mEditText.getText().toString())
                        ComposeActivity.super.onBackPressed()
                })
                .show()
    }

    private fun send() {
        val content = mEditText.getText().toString()
        Preferences.saveDraft(this, mParentId, content)
        toggleControls(true)
        Toast.makeText(this, R.string.sending, Toast.LENGTH_SHORT).show()
        mUserServices.reply(this, mParentId, content, ComposeCallback(this, mParentId))
    }

    @Synthetic
    fun onSent(successful: Boolean) {
        if (successful == null) {
            Toast.makeText(this, R.string.comment_failed, Toast.LENGTH_SHORT).show()
            toggleControls(false)
        } else if (successful) {
            Toast.makeText(this, R.string.comment_successful, Toast.LENGTH_SHORT).show()
            if (!isFinishing()) {
                finish()
                // TODO refresh parent
            }
        } else {
            if (!isFinishing()) {
                AppUtils.showLogin(this, mAlertDialogBuilder)
            }
            toggleControls(false)
        }
    }

    private fun createQuote(): String {
        if (mQuoteText == null) {
            mQuoteText = String.format(FORMAT_QUOTE, AppUtils.fromHtml(mParentText)
                    .toString()
                    .trim()
                    .replaceAll(PARAGRAPH_BREAK_REGEX, PARAGRAPH_QUOTE))
        }
        return mQuoteText
    }

    private fun toggleControls(sending: Boolean) {
        if (isFinishing()) {
            return
        }
        mSending = sending
        mEditText.setEnabled(!sending)
        supportInvalidateOptionsMenu()
    }

    open class ComposeCallback : UserServices.Callback() {
        private var mComposeActivity: WeakReference<ComposeActivity>? = null
        private var mAppContext: Context? = null
        private var mParentId: String? = null

        @Synthetic
        constructor(composeActivity: ComposeActivity, parentId: String) {
            mComposeActivity = WeakReference<>(composeActivity)
            mAppContext = composeActivity.getApplicationContext()
            mParentId = parentId
        }

        override fun onDone(successful: Boolean) {
            Preferences.deleteDraft(mAppContext, mParentId)
            if (mComposeActivity.get() != null && !mComposeActivity.get().isActivityDestroyed()) {
                mComposeActivity.get().onSent(successful)
            }
        }

        override fun onError(throwable: Throwable) {
            if (mComposeActivity.get() != null && !mComposeActivity.get().isActivityDestroyed()) {
                mComposeActivity.get().onSent(null)
            }
        }
    }
}
