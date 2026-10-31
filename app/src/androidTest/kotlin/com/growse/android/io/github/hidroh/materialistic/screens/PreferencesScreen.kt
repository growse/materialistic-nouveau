package com.growse.android.io.github.hidroh.materialistic.screens

import com.growse.android.io.github.hidroh.materialistic.PreferencesActivity
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.text.KTextView

object PreferencesScreen : KScreen<PreferencesScreen>() {
  override val layoutId: Int? = null
  override val viewClass = PreferencesActivity::class.java

  /** The preference row whose title is [title]. */
  fun row(title: String) = KTextView { withText(title) }

  /** The summary line shown beneath the preference titled [title]. */
  fun summaryOf(title: String) = KTextView {
    withId(android.R.id.summary)
    withSibling { withText(title) }
  }

  /** An option in an open dropdown, which lives in its own popup window. */
  fun dropDownOption(label: String) = KTextView {
    withText(label)
  }
      .apply { inRoot { isPlatformPopup() } }
}
