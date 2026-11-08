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

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServices
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class ComposeActivity : ThemedActivity() {
  @Inject lateinit var userServices: UserServices
  @Inject lateinit var alertDialogBuilder: AlertDialogBuilder<*>

  private lateinit var editText: EditText
  private lateinit var parentId: String
  private var parentText: String? = null
  private val quoteText: String by lazy {
    FORMAT_QUOTE.format(
        AppUtils.fromHtml(parentText)
            .toString()
            .trim()
            .replace(PARAGRAPH_BREAK_REGEX, PARAGRAPH_QUOTE),
    )
  }
  private var sending = false

  private val backPressedCallback =
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          val text = editText.text.toString()
          if (
              text.isEmpty() ||
                  sending ||
                  Preferences.getDraft(this@ComposeActivity, parentId) == text
          ) {
            finishBack()
            return
          }
          alertDialogBuilder
              .init(this@ComposeActivity)
              .setMessage(R.string.confirm_save_draft)
              .setNegativeButton(android.R.string.no) { _, _ -> finishBack() }
              .setPositiveButton(android.R.string.yes) { _, _ ->
                Preferences.saveDraft(this@ComposeActivity, parentId, text)
                finishBack()
              }
              .show()
        }

        private fun finishBack() {
          isEnabled = false
          onBackPressedDispatcher.onBackPressed()
          isEnabled = true
        }
      }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val parentId = intent.getStringExtra(EXTRA_PARENT_ID)
    if (parentId.isNullOrEmpty()) {
      finish()
      return
    }
    this.parentId = parentId
    onBackPressedDispatcher.addCallback(this, backPressedCallback)
    @Suppress("DEPRECATION")
    window.statusBarColor = ContextCompat.getColor(this, R.color.blackT12)
    setContentView(R.layout.activity_compose)
    setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
    supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_HOME_AS_UP
    val scrollView = findViewById<ScrollView>(R.id.content)
    val initialPaddingBottom = scrollView.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(scrollView) { v, windowInsets ->
      val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(
          v.paddingLeft,
          v.paddingTop,
          v.paddingRight,
          initialPaddingBottom + systemBars.bottom,
      )
      windowInsets
    }
    editText = findViewById(R.id.edittext_body)
    if (savedInstanceState == null) {
      editText.setText(Preferences.getDraft(this, parentId))
    }
    val empty = findViewById<View>(R.id.empty)
    empty.setOnClickListener { editText.requestFocus() }
    empty.setOnLongClickListener {
      editText.requestFocus()
      editText.performLongClick()
    }
    parentText = intent.getStringExtra(EXTRA_PARENT_TEXT)
    if (!parentText.isNullOrEmpty()) {
      bindParentText()
    }
  }

  private fun bindParentText() {
    findViewById<View>(R.id.quote).isVisible = true
    val toggle = findViewById<TextView>(R.id.toggle)
    val textView = findViewById<TextView>(R.id.text)
    AppUtils.setTextWithLinks(textView, AppUtils.fromHtml(parentText))
    toggle.setOnClickListener {
      val expand = !textView.isVisible
      toggle.setCompoundDrawablesWithIntrinsicBounds(
          0,
          0,
          if (expand) R.drawable.ic_expand_less_white_24dp
          else R.drawable.ic_expand_more_white_24dp,
          0,
      )
      textView.isVisible = expand
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_compose, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    menu.findItem(R.id.menu_quote).isVisible = !sending && !parentText.isNullOrEmpty()
    menu.findItem(R.id.menu_send).isEnabled = !sending
    menu.findItem(R.id.menu_save_draft).isEnabled = !sending
    menu.findItem(R.id.menu_discard_draft).isEnabled = !sending
    return super.onPrepareOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_send -> {
        if (editText.length() == 0) {
          Toast.makeText(this, R.string.comment_required, Toast.LENGTH_SHORT).show()
          return false
        }
        send()
      }
      R.id.menu_quote -> editText.editableText.insert(0, quoteText)
      android.R.id.home -> onBackPressedDispatcher.onBackPressed()
      R.id.menu_save_draft -> Preferences.saveDraft(this, parentId, editText.text.toString())
      R.id.menu_discard_draft -> Preferences.deleteDraft(this, parentId)
      R.id.menu_guidelines ->
          alertDialogBuilder
              .init(this)
              .setView(WebView(this).apply { loadUrl(HN_FORMAT_DOC_URL) })
              .setPositiveButton(android.R.string.ok, null)
              .show()
      else -> return super.onOptionsItemSelected(item)
    }
    return true
  }

  private fun send() {
    val content = editText.text.toString()
    Preferences.saveDraft(this, parentId, content)
    toggleControls(true)
    Toast.makeText(this, R.string.sending, Toast.LENGTH_SHORT).show()
    userServices.reply(this, parentId, content, ComposeCallback(this, parentId))
  }

  /** @param successful null on error, false when the user needs to log in again */
  private fun onSent(successful: Boolean?) {
    when (successful) {
      null -> {
        Toast.makeText(this, R.string.comment_failed, Toast.LENGTH_SHORT).show()
        toggleControls(false)
      }
      true -> {
        Toast.makeText(this, R.string.comment_successful, Toast.LENGTH_SHORT).show()
        if (!isFinishing) {
          finish()
          // TODO refresh parent
        }
      }
      false -> {
        if (!isFinishing) {
          AppUtils.showLogin(this, alertDialogBuilder)
        }
        toggleControls(false)
      }
    }
  }

  private fun toggleControls(sending: Boolean) {
    if (isFinishing) {
      return
    }
    this.sending = sending
    editText.isEnabled = !sending
    invalidateOptionsMenu()
  }

  private class ComposeCallback(composeActivity: ComposeActivity, private val parentId: String) :
      UserServices.Callback() {
    private val activity = WeakReference(composeActivity)
    private val appContext: Context = composeActivity.applicationContext

    override fun onDone(successful: Boolean) {
      Preferences.deleteDraft(appContext, parentId)
      activity.get()?.takeUnless { it.isActivityDestroyed }?.onSent(successful)
    }

    override fun onError(throwable: Throwable?) {
      activity.get()?.takeUnless { it.isActivityDestroyed }?.onSent(null)
    }
  }

  companion object {
    @JvmField val EXTRA_PARENT_ID: String = ComposeActivity::class.java.name + ".EXTRA_PARENT_ID"
    @JvmField
    val EXTRA_PARENT_TEXT: String = ComposeActivity::class.java.name + ".EXTRA_PARENT_TEXT"
    private const val HN_FORMAT_DOC_URL = "https://news.ycombinator.com/formatdoc"
    private const val FORMAT_QUOTE = "> %s\n\n"
    private const val PARAGRAPH_QUOTE = "\n\n> "
    private val PARAGRAPH_BREAK_REGEX = Regex("\\n{2,}")
  }
}
