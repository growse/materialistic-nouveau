package com.growse.android.io.github.hidroh.materialistic.screens

import com.growse.android.io.github.hidroh.materialistic.ListActivity
import com.growse.android.io.github.hidroh.materialistic.R
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KTextView

object StoryListScreen : KScreen<StoryListScreen>() {
  override val layoutId = R.layout.fragment_list
  override val viewClass = ListActivity::class.java

  /** The title of the row for the story titled [title]. */
  fun storyTitle(title: String) = KTextView {
    withId(R.id.title)
    withText(title)
    isDescendantOfA { withId(R.id.recycler_view) }
  }

  /** The comment count button on the row for the story titled [title]; opens its comments. */
  fun commentsButton(title: String) = KView {
    withId(R.id.comment)
    isDescendantOfA {
      withId(R.id.story_view)
      withDescendant { withText(title) }
    }
  }

  /** Shown in place of the list when it couldn't be fetched. */
  val connectionError = KView { withId(R.id.empty) }
}
