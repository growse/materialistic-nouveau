package com.growse.android.io.github.hidroh.materialistic.screens

import android.graphics.Rect
import android.view.View
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.UserActivity
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher

object UserScreen : KScreen<UserScreen>() {
  override val layoutId = R.layout.activity_user
  override val viewClass = UserActivity::class.java

  /** The top of the sheet holding the profile, where a user drags it open. */
  private val sheetHeader = KView {
    withId(R.id.header_card_view)
    isDescendantOfA { withId(R.id.bottom_sheet) }
  }

  /**
   * Drags the profile sheet open, as a finger would: slowly, from its header to near the top of the
   * screen. The submissions list only lays out once the sheet is expanded, and a short flick can be
   * taken by the header's app bar instead of the sheet.
   */
  fun expandSheet() = sheetHeader.act {
    val drag =
        GeneralSwipeAction(
            Swipe.SLOW,
            { view -> visibleCenter(view) },
            { view -> floatArrayOf(visibleCenter(view)[0], view.rootView.height * 0.1f) },
            Press.FINGER,
        )
    // Espresso won't swipe a view less than 90% on screen, but only the top of the collapsed
    // sheet shows on taller screens, and that's where a finger would grab it anyway
    object : ViewAction by drag {
      override fun getConstraints(): Matcher<View> = isDisplayed()
    }
  }

  private fun visibleCenter(view: View): FloatArray {
    val visible = Rect().also { view.getGlobalVisibleRect(it) }
    return floatArrayOf(visible.exactCenterX(), visible.exactCenterY())
  }

  /** The username, followed by karma in brackets once the profile has loaded. */
  val title = KTextView {
    withId(R.id.title)
    isDescendantOfA { withId(R.id.header_card_view) }
  }

  val about = KTextView { withId(R.id.about) }

  /** The single tab, labelled with the number of submissions. */
  fun submissionsTab(label: String) = KTextView {
    withText(label)
    isDescendantOfA { withId(R.id.tab_layout) }
  }

  /** The title of a submitted story. */
  fun submittedStory(title: String) = KTextView {
    withId(R.id.title)
    withText(title)
    isDescendantOfA { withId(R.id.recycler_view) }
  }

  /** The body of a submitted comment. */
  fun submittedComment(text: String) = KTextView {
    withId(R.id.text)
    withText(text)
    isDescendantOfA { withId(R.id.recycler_view) }
  }

  /** Shown when the profile couldn't be loaded. */
  val empty = KView { withId(R.id.empty) }
}
