package com.growse.android.io.github.hidroh.materialistic.screens

import com.growse.android.io.github.hidroh.materialistic.ItemActivity
import com.growse.android.io.github.hidroh.materialistic.R
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.tabs.KTabLayout
import io.github.kakaocup.kakao.text.KTextView

object ItemScreen : KScreen<ItemScreen>() {
  override val layoutId = R.layout.activity_item
  override val viewClass = ItemActivity::class.java

  val bookmark = KImageView {
    withId(R.id.bookmarked)
    isDescendantOfA { withId(R.id.header_card_view) }
  }

  /** Comments are the first tab; stories open on the article tab by default. */
  val tabs = KTabLayout { withId(R.id.tab_layout) }

  /** The body of the comment whose text is [text]. */
  fun comment(text: String) = KTextView {
    withId(R.id.text)
    withText(text)
    isDescendantOfA { withId(R.id.recycler_view) }
  }
}
