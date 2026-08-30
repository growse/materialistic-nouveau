package com.growse.android.io.github.hidroh.materialistic.screens

import com.growse.android.io.github.hidroh.materialistic.PreferencesActivity
import com.growse.android.io.github.hidroh.materialistic.R
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KTextView

object DisplayPreferencesScreen : KScreen<DisplayPreferencesScreen>() {
  override val layoutId = R.layout.activity_preferences
  override val viewClass = PreferencesActivity::class.java

  val primaryColorTitle = KTextView { withText(R.string.pref_primary_color_title) }
  val accentColorTitle = KTextView { withText(R.string.pref_accent_color_title) }

  fun summaryWithText(text: String) = KTextView { withText(text) }

  /** [contentDescription] is "<preference title> <swatch label>", e.g. "Toolbar color Teal". */
  fun swatch(contentDescription: String) = KView { withContentDescription(contentDescription) }
}
