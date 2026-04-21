package com.growse.android.io.github.hidroh.materialistic.screens

import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.ReleaseNotesActivity
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KButton

object ReleaseNotesScreen : KScreen<ReleaseNotesScreen>() {
  override val layoutId = R.layout.activity_release
  override val viewClass = ReleaseNotesActivity::class.java

  val webView = KView { withId(R.id.web_view) }
  val okButton = KButton { withId(R.id.button_ok) }
  val rateButton = KButton { withId(R.id.button_rate) }
}
