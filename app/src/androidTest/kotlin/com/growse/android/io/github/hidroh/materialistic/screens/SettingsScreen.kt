package com.growse.android.io.github.hidroh.materialistic.screens

import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.settings.SettingsActivity
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.common.views.KView

/** The top level of settings, which links to each group of preferences. */
object SettingsScreen : KScreen<SettingsScreen>() {
  override val layoutId = R.layout.activity_settings
  override val viewClass = SettingsActivity::class.java

  val listOptions = KView { withId(R.id.menu_list) }

  val commentOptions = KView { withId(R.id.menu_comments) }
}
