package com.growse.android.io.github.hidroh.materialistic.screens

import com.growse.android.io.github.hidroh.materialistic.ListActivity
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.testing.waitUntilSettled
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.swiperefresh.KSwipeRefreshLayout
import io.github.kakaocup.kakao.text.KTextView
import io.github.kakaocup.kakao.toolbar.KToolbar

/** A story list: top, new, best, Show HN, Ask HN, jobs, popular and search results all share it. */
object StoryListScreen : KScreen<StoryListScreen>() {
  override val layoutId = R.layout.fragment_list
  override val viewClass = ListActivity::class.java

  val toolbar = KToolbar { withId(R.id.toolbar) }

  /** Pulling this down refreshes the list from the network. */
  val refreshLayout = KSwipeRefreshLayout { withId(R.id.swipe_layout) }

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

  private val list = KView { withId(R.id.recycler_view) }

  /** Taps the comment count of the story titled [title], once the list has stopped changing. */
  fun openComments(title: String) {
    storyTitle(title).isVisible()
    list.waitUntilSettled()
    commentsButton(title).click()
  }

  /** Shown in place of the list when it couldn't be fetched. */
  val connectionError = KView { withId(R.id.empty) }
}
