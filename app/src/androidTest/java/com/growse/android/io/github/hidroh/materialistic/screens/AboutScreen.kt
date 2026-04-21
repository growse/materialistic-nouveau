package com.growse.android.io.github.hidroh.materialistic.screens

import com.growse.android.io.github.hidroh.materialistic.AboutActivity
import com.growse.android.io.github.hidroh.materialistic.R
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.text.KTextView
import io.github.kakaocup.kakao.toolbar.KToolbar

object AboutScreen : KScreen<AboutScreen>() {
  override val layoutId = R.layout.activity_about
  override val viewClass = AboutActivity::class.java

  val toolbar = KToolbar { withId(R.id.toolbar) }
  val applicationInfoText = KTextView { withId(R.id.text_application_info) }
  val developerInfoText = KTextView { withId(R.id.text_developer_info) }
  val librariesText = KTextView { withId(R.id.text_libraries) }
  val licenseText = KTextView { withId(R.id.text_license) }
  val thirdPartyLicensesText = KTextView { withId(R.id.text_3rd_party_licenses) }
  val privacyPolicyText = KTextView { withId(R.id.text_privacy_policy) }
}
