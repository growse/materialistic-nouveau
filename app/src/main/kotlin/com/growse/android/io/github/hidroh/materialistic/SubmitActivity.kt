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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputLayout
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServices
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import java.net.MalformedURLException
import java.net.URL
import javax.inject.Inject

@AndroidEntryPoint
class SubmitActivity : ThemedActivity() {
  @Inject lateinit var userServices: UserServices
  @Inject lateinit var alertDialogBuilder: AlertDialogBuilder<*>

  private lateinit var titleEditText: TextView
  private lateinit var contentEditText: TextView
  private lateinit var titleLayout: TextInputLayout
  private lateinit var contentLayout: TextInputLayout
  private var sending = false

  private val backPressedCallback =
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          alertDialogBuilder
              .init(this@SubmitActivity)
              .setMessage(if (sending) R.string.confirm_no_waiting else R.string.confirm_no_submit)
              .setNegativeButton(android.R.string.cancel, null)
              .setPositiveButton(android.R.string.ok) { _, _ ->
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
              }
              .show()
        }
      }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    onBackPressedDispatcher.addCallback(this, backPressedCallback)
    @Suppress("DEPRECATION")
    window.statusBarColor = ContextCompat.getColor(this, R.color.blackT12)
    setContentView(R.layout.activity_submit)
    setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
    supportActionBar?.displayOptions =
        ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_SHOW_TITLE or ActionBar.DISPLAY_HOME_AS_UP
    val content = findViewById<LinearLayout>(R.id.content)
    val initialPaddingBottom = content.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(content) { v, windowInsets ->
      val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(
          v.paddingLeft,
          v.paddingTop,
          v.paddingRight,
          initialPaddingBottom + systemBars.bottom,
      )
      windowInsets
    }
    titleLayout = findViewById(R.id.textinput_title)
    contentLayout = findViewById(R.id.textinput_content)
    titleEditText = findViewById(R.id.edittext_title)
    contentEditText = findViewById(R.id.edittext_content)
    val subject: String?
    val text: String?
    if (savedInstanceState == null) {
      subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
      text = intent.getStringExtra(Intent.EXTRA_TEXT)
    } else {
      subject = savedInstanceState.getString(STATE_SUBJECT)
      text = savedInstanceState.getString(STATE_TEXT)
    }
    titleEditText.text = subject
    contentEditText.text = text
    if (subject.isNullOrEmpty() && !text.isNullOrEmpty()) {
      if (isUrl(text)) {
        fetchTitle(text)
      } else {
        extractUrl(text)
      }
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_submit, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    menu.findItem(R.id.menu_send).isEnabled = !sending
    return super.onPrepareOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      android.R.id.home -> onBackPressedDispatcher.onBackPressed()
      R.id.menu_send -> if (validate()) confirmSubmit()
      R.id.menu_guidelines ->
          alertDialogBuilder
              .init(this)
              .setView(WebView(this).apply { loadUrl(HN_GUIDELINES_URL) })
              .setPositiveButton(android.R.string.ok, null)
              .show()
      else -> return super.onOptionsItemSelected(item)
    }
    return true
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(STATE_SUBJECT, titleEditText.text.toString())
    outState.putString(STATE_TEXT, contentEditText.text.toString())
  }

  /** Fills in the title from the page at [url], unless the user has typed one first. */
  private fun fetchTitle(url: String) {
    WebView(this).apply {
      webChromeClient =
          object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
              if (titleEditText.length() == 0) {
                titleEditText.text = title
              }
            }
          }
      loadUrl(url)
    }
  }

  private fun validate(): Boolean {
    titleLayout.isErrorEnabled = false
    contentLayout.isErrorEnabled = false
    if (titleEditText.length() == 0) {
      titleLayout.error = getString(R.string.title_required)
    }
    if (contentEditText.length() == 0) {
      contentLayout.error = getString(R.string.url_text_required)
    }
    return titleEditText.length() > 0 && contentEditText.length() > 0
  }

  private fun confirmSubmit() {
    val isUrl = isUrl(contentEditText.text.toString())
    alertDialogBuilder
        .init(this)
        .setMessage(if (isUrl) R.string.confirm_submit_url else R.string.confirm_submit_question)
        .setPositiveButton(android.R.string.ok) { _, _ -> submit(isUrl) }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
  }

  private fun submit(isUrl: Boolean) {
    toggleControls(true)
    Toast.makeText(this, R.string.sending, Toast.LENGTH_SHORT).show()
    userServices.submit(
        this,
        titleEditText.text.toString(),
        contentEditText.text.toString(),
        isUrl,
        SubmitCallback(this),
    )
  }

  /** @param successful null on error, false when the user needs to log in again */
  private fun onSubmitted(successful: Boolean?) {
    when (successful) {
      null -> {
        toggleControls(false)
        Toast.makeText(this, R.string.submit_failed, Toast.LENGTH_SHORT).show()
      }
      true -> {
        Toast.makeText(this, R.string.submit_successful, Toast.LENGTH_SHORT).show()
        if (!isFinishing) {
          startActivity(
              Intent(this, NewActivity::class.java)
                  .putExtra(NewActivity.EXTRA_REFRESH, true)
                  .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
          ) // TODO should go to profile instead?
          finish()
        }
      }
      false ->
          if (!isFinishing) {
            AppUtils.showLogin(this, alertDialogBuilder)
          }
    }
  }

  /** Shows [message], then opens [data] (an existing duplicate submission) if there is one. */
  private fun onError(@StringRes message: Int, data: Uri?) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    if (data != null) {
      startActivity(Intent(Intent.ACTION_VIEW).setData(data))
    }
  }

  private fun isUrl(text: String): Boolean =
      try {
        URL(text)
        true
      } catch (_: MalformedURLException) {
        false
      }

  /** Splits shared "Some title - https://example.com" text into the title and URL fields. */
  private fun extractUrl(text: String) {
    val match = FUZZY_URL.find(text) ?: return
    titleEditText.text = match.groupValues[1].trim().trimEnd(' ', ':', '-')
    contentEditText.text = match.groupValues[2]
  }

  private fun toggleControls(sending: Boolean) {
    if (isFinishing) {
      return
    }
    this.sending = sending
    titleEditText.isEnabled = !sending
    contentEditText.isEnabled = !sending
    invalidateOptionsMenu()
  }

  private class SubmitCallback(submitActivity: SubmitActivity) : UserServices.Callback() {
    private val activity = WeakReference(submitActivity)

    override fun onDone(successful: Boolean) {
      activity.get()?.takeUnless { it.isActivityDestroyed }?.onSubmitted(successful)
    }

    override fun onError(throwable: Throwable?) {
      val activity = activity.get()?.takeUnless { it.isActivityDestroyed } ?: return
      if (throwable is UserServices.Exception) {
        activity.onError(
            throwable.messageResId,
            throwable.itemId?.let { AppUtils.createItemUri(it) },
        )
      } else {
        activity.onSubmitted(null)
      }
    }
  }

  companion object {
    private const val HN_GUIDELINES_URL = "https://news.ycombinator.com/newsguidelines.html"
    private const val STATE_SUBJECT = "state:subject"
    private const val STATE_TEXT = "state:text"

    // matching title url without any trailing text: group 1 title, group 2 url, group 3 scheme
    private val FUZZY_URL = Regex("(.*)((http|https)://[^\\s]*)$")
  }
}
