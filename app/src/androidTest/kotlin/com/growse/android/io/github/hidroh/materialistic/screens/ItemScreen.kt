package com.growse.android.io.github.hidroh.materialistic.screens

import com.growse.android.io.github.hidroh.materialistic.ItemActivity
import com.growse.android.io.github.hidroh.materialistic.R
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.image.KImageView

object ItemScreen : KScreen<ItemScreen>() {
  override val layoutId = R.layout.activity_item
  override val viewClass = ItemActivity::class.java

  val bookmark = KImageView {
    withId(R.id.bookmarked)
    isDescendantOfA { withId(R.id.header_card_view) }
  }
}
