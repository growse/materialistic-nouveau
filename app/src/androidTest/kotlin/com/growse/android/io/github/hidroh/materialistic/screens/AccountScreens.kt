package com.growse.android.io.github.hidroh.materialistic.screens

import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.SubmitActivity
import com.growse.android.io.github.hidroh.materialistic.accounts.LoginActivity
import com.growse.android.io.github.hidroh.materialistic.item.ComposeActivity
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.dialog.KAlertDialog
import io.github.kakaocup.kakao.edit.KEditText
import io.github.kakaocup.kakao.edit.KTextInputLayout
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView

object LoginScreen : KScreen<LoginScreen>() {
  override val layoutId = R.layout.activity_login
  override val viewClass = LoginActivity::class.java

  val usernameLayout = KTextInputLayout { withId(R.id.textinput_username) }
  val passwordLayout = KTextInputLayout { withId(R.id.textinput_password) }
  val username = KEditText { withId(R.id.edittext_username) }
  val password = KEditText { withId(R.id.edittext_password) }
  val loginButton = KButton { withId(R.id.login_button) }
  val registerButton = KButton { withId(R.id.register_button) }

  /** The dialog's title, which asks to re-enter the password for a stale sign-in. */
  fun title(text: String) = KTextView { withText(text) }
}

object ComposeScreen : KScreen<ComposeScreen>() {
  override val layoutId = R.layout.activity_compose
  override val viewClass = ComposeActivity::class.java

  val body = KEditText { withId(R.id.edittext_body) }
  val send = KView { withId(R.id.menu_send) }
  val quote = KView { withId(R.id.menu_quote) }

  /** The "Save draft?" prompt shown when backing out of an unsaved reply. */
  val saveDraftPrompt = KAlertDialog()
}

object SubmitScreen : KScreen<SubmitScreen>() {
  override val layoutId = R.layout.activity_submit
  override val viewClass = SubmitActivity::class.java

  val titleLayout = KTextInputLayout { withId(R.id.textinput_title) }
  val contentLayout = KTextInputLayout { withId(R.id.textinput_content) }
  val title = KEditText { withId(R.id.edittext_title) }
  val content = KEditText { withId(R.id.edittext_content) }
  val send = KView { withId(R.id.menu_send) }

  /** Asks whether to submit as a link or a question before anything is sent. */
  val confirmation = KAlertDialog()
}
