package com.growse.android.io.github.hidroh.materialistic.screens

import com.growse.android.io.github.hidroh.materialistic.FavoriteActivity
import com.growse.android.io.github.hidroh.materialistic.R
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.text.KTextView

object SavedStoriesScreen : KScreen<SavedStoriesScreen>() {
  override val layoutId = R.layout.fragment_favorite
  override val viewClass = FavoriteActivity::class.java

  /** The title of the row for the saved story titled [title]. */
  fun storyTitle(title: String) = KTextView {
    withId(R.id.title)
    withText(title)
    isDescendantOfA { withId(R.id.recycler_view) }
  }
}
