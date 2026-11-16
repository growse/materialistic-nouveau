package com.growse.android.io.github.hidroh.materialistic.screens

import android.view.View
import androidx.test.espresso.ViewAssertion
import androidx.viewpager.widget.ViewPager
import com.growse.android.io.github.hidroh.materialistic.ItemActivity
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.testing.waitUntilSettled
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.tabs.KTabLayout
import io.github.kakaocup.kakao.text.KTextView
import junit.framework.AssertionFailedError

object ItemScreen : KScreen<ItemScreen>() {
  override val layoutId = R.layout.activity_item
  override val viewClass = ItemActivity::class.java

  val bookmark = KImageView {
    withId(R.id.bookmarked)
    isDescendantOfA { withId(R.id.header_card_view) }
  }

  /** Comments are the first tab; stories open on the article tab by default. */
  val tabs = KTabLayout { withId(R.id.tab_layout) }

  private val pager = KView { withId(R.id.view_pager) }

  /**
   * Switches to the comments tab and waits for the pager to finish scrolling there: the pager
   * swallows taps while it's still settling, so tapping a comment straight away can be lost.
   */
  fun showComments() {
    tabs.selectTab(0)
    pager.assert {
      ViewAssertion { view, noViewFound ->
        if (noViewFound != null) throw noViewFound
        val viewPager = view as ViewPager
        // the comments page is the one holding the comment list's swipe layout; it's settled once
        // its left edge lines up with the pager's scroll position, wherever that offset starts
        val commentsPage = viewPager.findViewById<View>(R.id.swipe_layout)
        if (viewPager.currentItem != 0 || commentsPage.left != viewPager.scrollX) {
          throw AssertionFailedError(
              "Pager still settling: page ${viewPager.currentItem}, " +
                  "comments at ${commentsPage.left}, scrolled to ${viewPager.scrollX}",
          )
        }
      }
    }
  }

  /** The body of the comment whose text is [text]. */
  fun comment(text: String) = KTextView {
    withId(R.id.text)
    withText(text)
    isDescendantOfA { withId(R.id.recycler_view) }
  }

  /** The author and age line of the comment whose text is [text]. */
  fun postedLineOf(text: String) = KTextView {
    withId(R.id.posted)
    inCommentRow(text)
  }

  /** The more-options button (vote, reply, share) on the comment whose text is [text]. */
  fun moreButtonOf(text: String) = KView {
    withId(R.id.button_more)
    inCommentRow(text)
  }

  private val commentList = KView {
    withId(R.id.recycler_view)
    isDescendantOfA { withId(R.id.view_pager) }
  }

  /** Taps [target] in the comment list once the list has stopped changing. */
  fun tapWhenSettled(target: KView) {
    target.isVisible()
    commentList.waitUntilSettled()
    target.click()
  }

  /** Collapses or expands the replies to the comment whose text is [text]. */
  fun repliesToggleOf(text: String) = KView {
    withId(R.id.button_toggle)
    inCommentRow(text)
  }

  /** In multiple-pages mode, opens the replies to the comment whose text is [text]. */
  fun repliesButtonOf(text: String) = KView {
    withId(R.id.comment)
    inCommentRow(text)
  }
}

/** Narrows to the list row, a direct child of the comment list, holding the comment [text]. */
private fun ViewBuilder.inCommentRow(text: String) = isDescendantOfA {
  withParent { withId(R.id.recycler_view) }
  withDescendant { withText(text) }
}
