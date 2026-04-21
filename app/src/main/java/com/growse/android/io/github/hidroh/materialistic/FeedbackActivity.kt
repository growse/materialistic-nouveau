/*
 * Copyright (c) 2016 Ha Duy Trung
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
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import com.growse.android.io.github.hidroh.materialistic.annotation.Synthetic
import com.growse.android.io.github.hidroh.materialistic.data.FeedbackClient
import java.lang.ref.WeakReference
import javax.inject.Inject

class FeedbackActivity : InjectableActivity() {
  @JvmField @Inject var mFeedbackClient: FeedbackClient? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
    setContentView(R.layout.activity_feedback)
    AppUtils.setTextWithLinks(
        findViewById<TextView?>(R.id.feedback_note),
        AppUtils.fromHtml(getString(R.string.feedback_note)),
    )
    val titleLayout = findViewById<TextInputLayout>(R.id.textinput_title)
    val bodyLayout = findViewById<TextInputLayout>(R.id.textinput_body)
    val title = findViewById<EditText>(R.id.edittext_title)
    val body = findViewById<EditText>(R.id.edittext_body)
    val sendButton = findViewById<View>(R.id.feedback_button)
    findViewById<View?>(R.id.button_rate).setOnClickListener { v: View? ->
      AppUtils.openPlayStore(this@FeedbackActivity)
      finish()
    }
    sendButton.setOnClickListener { v: View? ->
      titleLayout.isErrorEnabled = false
      bodyLayout.isErrorEnabled = false
      if (title.length() == 0) {
        titleLayout.setError(getString(R.string.title_required))
      }
      if (body.length() == 0) {
        bodyLayout.setError(getString(R.string.comment_required))
      }
      if (title.length() == 0 || body.length() == 0) {
        return@setOnClickListener
      }
      sendButton.setEnabled(false)
      mFeedbackClient!!.send(
          title.getText().toString(),
          body.getText().toString(),
          FeedbackCallback(this),
      )
    }
  }

  override fun isDialogTheme(): Boolean {
    return true
  }

  @Synthetic
  fun onFeedbackSent(success: Boolean) {
    Toast.makeText(
            this,
            if (success) R.string.feedback_sent else R.string.feedback_failed,
            Toast.LENGTH_SHORT,
        )
        .show()
    if (success) {
      finish()
    } else {
      findViewById<View?>(R.id.feedback_button).isEnabled = true
    }
  }

  internal class FeedbackCallback @Synthetic constructor(drawerActivity: FeedbackActivity?) :
      FeedbackClient.Callback {
    private val mFeedbackActivity: WeakReference<FeedbackActivity?> =
        WeakReference<FeedbackActivity?>(drawerActivity)

    override fun onSent(success: Boolean) {
      if (mFeedbackActivity.get() != null && !mFeedbackActivity.get()!!.isActivityDestroyed) {
        mFeedbackActivity.get()!!.onFeedbackSent(success)
      }
    }
  }
}
