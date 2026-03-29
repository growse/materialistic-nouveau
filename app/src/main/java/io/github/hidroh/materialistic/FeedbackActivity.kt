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

package io.github.hidroh.materialistic

import android.os.Bundle
import com.google.android.material.textfield.TextInputLayout
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

import java.lang.ref.WeakReference

import javax.inject.Inject

import io.github.hidroh.materialistic.annotation.Synthetic
import io.github.hidroh.materialistic.data.FeedbackClient

open class FeedbackActivity : InjectableActivity() {
    @Inject var mFeedbackClient: FeedbackClient? = null

    @SuppressWarnings("ConstantConditions")
    protected override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_feedback)
        AppUtils.setTextWithLinks((TextView) findViewById(R.id.feedback_note),
                AppUtils.fromHtml(getString(R.string.feedback_note)))
        final TextInputLayout titleLayout = (TextInputLayout)
                findViewById(R.id.textinput_title)
        final TextInputLayout bodyLayout = (TextInputLayout)
                findViewById(R.id.textinput_body)
        final EditText title = (EditText) findViewById(R.id.edittext_title)
        final EditText body = (EditText) findViewById(R.id.edittext_body)
        final View sendButton = findViewById(R.id.feedback_button)
        findViewById(R.id.button_rate).setOnClickListener(v -> {
            AppUtils.openPlayStore(FeedbackActivity.this)
            finish()
        })
        sendButton.setOnClickListener(v -> {
            titleLayout.setErrorEnabled(false)
            bodyLayout.setErrorEnabled(false)
            if (title.length() == 0) {
                titleLayout.setError(getString(R.string.title_required))
            }
            if (body.length() == 0) {
                bodyLayout.setError(getString(R.string.comment_required))
            }
            if (title.length() == 0 || body.length() == 0) {
                return
            }
            sendButton.setEnabled(false)
            mFeedbackClient.send(title.getText().toString(), body.getText().toString(),
                    FeedbackCallback(this))
        })
    }

    protected override fun isDialogTheme(): Boolean {
        return true
    }

    @Synthetic
    fun onFeedbackSent(success: Boolean) {
        Toast.makeText(this,
                success ? R.string.feedback_sent : R.string.feedback_failed,
                Toast.LENGTH_SHORT)
                .show()
        if (success) {
            finish()
        } else {
            //noinspection ConstantConditions
            findViewById(R.id.feedback_button).setEnabled(true)
        }
    }

    open class FeedbackCallback : FeedbackClient.Callback {
        private var mFeedbackActivity: WeakReference<FeedbackActivity>? = null

        @Synthetic
        constructor(drawerActivity: FeedbackActivity) {
            mFeedbackActivity = WeakReference<>(drawerActivity)
        }

        override fun onSent(success: Boolean) {
            if (mFeedbackActivity.get() != null && !mFeedbackActivity.get().isActivityDestroyed()) {
                mFeedbackActivity.get().onFeedbackSent(success)
            }
        }
    }
}
