package com.growse.android.io.github.hidroh.materialistic.screens

import com.growse.android.io.github.hidroh.materialistic.FeedbackActivity
import com.growse.android.io.github.hidroh.materialistic.R
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.edit.KEditText
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView

object FeedbackScreen : KScreen<FeedbackScreen>() {
  override val layoutId = R.layout.activity_feedback
  override val viewClass = FeedbackActivity::class.java

  val titleEditText = KEditText { withId(R.id.edittext_title) }
  val bodyEditText = KEditText { withId(R.id.edittext_body) }
  val sendButton = KButton { withId(R.id.feedback_button) }
  val rateButton = KTextView { withId(R.id.button_rate) }
  val feedbackNote = KTextView { withId(R.id.feedback_note) }
}
