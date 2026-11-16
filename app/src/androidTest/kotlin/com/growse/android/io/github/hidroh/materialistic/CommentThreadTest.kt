package com.growse.android.io.github.hidroh.materialistic

import com.growse.android.io.github.hidroh.materialistic.screens.ItemScreen
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackendRule
import com.growse.android.io.github.hidroh.materialistic.testing.FakeHackerNews
import com.growse.android.io.github.hidroh.materialistic.testing.PreferencesRule
import com.growse.android.io.github.hidroh.materialistic.testing.ScenarioRule
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class CommentThreadTest : TestCase() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val backend = FakeBackendRule()

  @get:Rule(order = 2) val scenarios = ScenarioRule()

  @get:Rule(order = 3) val preferences = PreferencesRule(R.string.pref_comment_display)

  @Test
  fun topLevelCommentsAreShown() = run {
    step("Serve a story with two comments") {
      FakeHackerNews.story(STORY_ID, "Story", kids = listOf(2001, 2002))
      FakeHackerNews.comment(2001, parent = STORY_ID, text = "First comment")
      FakeHackerNews.comment(2002, parent = STORY_ID, text = "Second comment")
    }
    openComments()
    step("Verify both comments are shown") {
      ItemScreen {
        comment("First comment").isVisible()
        comment("Second comment").isVisible()
      }
    }
  }

  @Test
  fun repliesAreShown() = run {
    serveCommentWithReply()
    openComments()
    step("Verify the comment and its reply are shown") {
      ItemScreen {
        comment(PARENT).isVisible()
        comment(REPLY).isVisible()
      }
    }
  }

  @Test
  fun collapsingACommentHidesItsReplies() = run {
    serveCommentWithReply()
    openComments()
    step("Collapse the comment") { ItemScreen { tapWhenSettled(repliesToggleOf(PARENT)) } }
    step("Verify its reply is hidden") { ItemScreen { comment(REPLY).doesNotExist() } }
    step("Expand it again") { ItemScreen { tapWhenSettled(repliesToggleOf(PARENT)) } }
    step("Verify its reply is back") { ItemScreen { comment(REPLY).isVisible() } }
  }

  @Test
  fun collapsedDisplayHidesRepliesUntilExpanded() = run {
    step("Choose the collapsed thread display") {
      preferences.set(
          R.string.pref_comment_display,
          device.targetContext.getString(R.string.pref_comment_display_value_collapsed),
      )
    }
    serveCommentWithReply()
    openComments()
    step("Verify the comment is shown without its reply") {
      ItemScreen {
        comment(PARENT).isVisible()
        comment(REPLY).doesNotExist()
      }
    }
    step("Expand the comment") { ItemScreen { tapWhenSettled(repliesToggleOf(PARENT)) } }
    step("Verify its reply is shown") { ItemScreen { comment(REPLY).isVisible() } }
  }

  @Test
  fun multiplePagesDisplayShowsRepliesOnTheirOwnPage() = run {
    step("Choose the multiple pages thread display") {
      preferences.set(
          R.string.pref_comment_display,
          device.targetContext.getString(R.string.pref_comment_display_value_multiple),
      )
    }
    serveCommentWithReply()
    openComments()
    step("Verify only the top-level comment is shown") {
      ItemScreen {
        comment(PARENT).isVisible()
        comment(REPLY).doesNotExist()
      }
    }
    step("Open the comment's replies") { ItemScreen { tapWhenSettled(repliesButtonOf(PARENT)) } }
    step("Verify its reply is shown") { ItemScreen { comment(REPLY).isVisible() } }
  }

  @Test
  fun deadCommentsAreMarkedAndCannotBeActedOn() = run {
    step("Serve a story with a dead comment") {
      FakeHackerNews.story(STORY_ID, "Story", kids = listOf(2001))
      FakeHackerNews.comment(2001, parent = STORY_ID, text = "Flagged comment", dead = true)
    }
    openComments()
    step("Verify it's marked dead and has no more-options button") {
      ItemScreen {
        postedLineOf("Flagged comment")
            .startsWithText(
                device.targetContext.getString(R.string.dead_prefix),
            )
        moreButtonOf("Flagged comment").isInvisible()
      }
    }
  }

  @Test
  fun repliesToADeletedCommentAreStillShown() = run {
    step("Serve a story whose only comment was deleted after being replied to") {
      FakeHackerNews.story(STORY_ID, "Story", kids = listOf(2001))
      FakeHackerNews.deletedComment(2001, parent = STORY_ID, kids = listOf(3001))
      FakeHackerNews.comment(3001, parent = 2001, text = "Reply to a deleted comment")
    }
    openComments()
    step("Verify the reply is shown") {
      ItemScreen { comment("Reply to a deleted comment").isVisible() }
    }
  }

  private fun serveCommentWithReply() {
    FakeHackerNews.story(STORY_ID, "Story", kids = listOf(2001))
    FakeHackerNews.comment(2001, parent = STORY_ID, text = PARENT, kids = listOf(3001))
    FakeHackerNews.comment(3001, parent = 2001, text = REPLY)
  }

  /**
   * Opens [STORY_ID] the way a news.ycombinator.com link does, so it's fetched by id, then switches
   * from the article tab it opens on to its comments.
   */
  private fun TestContext<Unit>.openComments() {
    step("Open the story from a link") {
      scenarios.launchLink(
          ItemActivity::class.java,
          "https://news.ycombinator.com/item?id=$STORY_ID",
      )
    }
    step("Switch to the comments tab") { ItemScreen { showComments() } }
  }

  private companion object {
    const val STORY_ID = 1001L
    const val PARENT = "Parent comment"
    const val REPLY = "A reply"
  }
}
