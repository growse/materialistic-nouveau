package com.growse.android.io.github.hidroh.materialistic

import com.growse.android.io.github.hidroh.materialistic.screens.UserScreen
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackendRule
import com.growse.android.io.github.hidroh.materialistic.testing.FakeHackerNews
import com.growse.android.io.github.hidroh.materialistic.testing.ScenarioRule
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class UserProfileTest : TestCase() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val backend = FakeBackendRule()

  @get:Rule(order = 2) val scenarios = ScenarioRule()

  @Test
  fun profileShowsKarmaAboutAndSubmissions() = run {
    step("Serve a user who has posted a story and a comment") {
      FakeHackerNews.user(
          USERNAME,
          karma = 42,
          about = "Writes tests",
          submitted = listOf(5002, 5001),
      )
      FakeHackerNews.story(5001, "A story by $USERNAME", by = USERNAME)
      FakeHackerNews.comment(5002, parent = 5001, text = "A comment by $USERNAME", by = USERNAME)
    }
    openProfile()
    step("Verify the name, karma and about text are shown") {
      UserScreen {
        title.hasText("$USERNAME (42)")
        about.hasText("Writes tests")
      }
    }
    step("Expand the profile sheet") { UserScreen { expandSheet() } }
    step("Verify both submissions are listed") {
      UserScreen {
        submissionsTab("2 submissions").isVisible()
        submittedStory("A story by $USERNAME").isVisible()
        submittedComment("A comment by $USERNAME").isVisible()
      }
    }
  }

  @Test
  fun unknownUserShowsEmptyProfile() = run {
    step("Serve no profile for the user") { FakeHackerNews.noSuchUser(USERNAME) }
    openProfile()
    step("Verify the empty state is shown under their name") {
      UserScreen {
        title.hasText(USERNAME)
        empty.isVisible()
      }
    }
  }

  private fun openProfile() =
      scenarios.launchLink(
          UserActivity::class.java,
          "https://news.ycombinator.com/user?id=$USERNAME",
      )

  private companion object {
    const val USERNAME = "alice"
  }
}
