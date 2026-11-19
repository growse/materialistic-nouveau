package com.growse.android.io.github.hidroh.materialistic.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.platform.app.InstrumentationRegistry
import com.growse.android.io.github.hidroh.materialistic.ItemActivity
import com.growse.android.io.github.hidroh.materialistic.ListActivity
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.SubmitActivity
import com.growse.android.io.github.hidroh.materialistic.about.AboutActivity
import com.growse.android.io.github.hidroh.materialistic.item.ComposeActivity
import com.growse.android.io.github.hidroh.materialistic.screens.StoryListScreen
import com.growse.android.io.github.hidroh.materialistic.settings.PreferencesActivity
import com.growse.android.io.github.hidroh.materialistic.settings.SettingsActivity
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackendRule
import com.growse.android.io.github.hidroh.materialistic.testing.FakeHackerNews
import com.growse.android.io.github.hidroh.materialistic.testing.ScenarioRule
import com.growse.android.io.github.hidroh.materialistic.util.AppUtils
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.views.KView
import junit.framework.AssertionFailedError
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/**
 * Every screen's toolbar sits below a solid status bar in the theme's status bar color. Targeting
 * SDK 35+ makes the app draw edge-to-edge on Android 15 and later, so a screen that doesn't pad for
 * the status bar puts its toolbar, and its back button, underneath it. Only those devices show the
 * problem, and only with SystemUI running, so the checks are skipped on ATD images, which have no
 * status bar.
 */
@HiltAndroidTest
class EdgeToEdgeTest : TestCase() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val backend = FakeBackendRule()

  @get:Rule(order = 2) val scenarios = ScenarioRule()

  private val context
    get() = device.targetContext

  @Test fun settings() = run { check(SettingsActivity::class.java) }

  @Test
  fun settingsSubpages() = run {
    listOf(
            R.string.display to R.xml.preferences_display,
            R.string.offline to R.xml.preferences_offline,
            R.string.list_display_options to R.xml.preferences_list,
            R.string.comments to R.xml.preferences_comments,
            R.string.readability to R.xml.preferences_readability,
        )
        .forEach { (title, preferences) ->
          check(
              Intent(context, PreferencesActivity::class.java)
                  .putExtra(PreferencesActivity.EXTRA_TITLE, title)
                  .putExtra(PreferencesActivity.EXTRA_PREFERENCES, preferences),
          )
        }
  }

  @Test fun about() = run { check(AboutActivity::class.java) }

  @Test fun submit() = run { check(SubmitActivity::class.java) }

  @Test
  fun compose() = run {
    check(
        Intent(context, ComposeActivity::class.java).putExtra(ComposeActivity.EXTRA_PARENT_ID, "1"),
    )
  }

  @Test
  fun storyList() = run {
    FakeHackerNews.topStories()
    check(ListActivity::class.java)
  }

  @Test
  fun story() = run {
    FakeHackerNews.story(1001, "Story")
    check(
        Intent(context, ItemActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse("https://news.ycombinator.com/item?id=1001")),
    )
  }

  @Test
  fun scrollingAStoryListKeepsTheStatusBarSolid() = run {
    var themeColor = 0
    val ids = (1L..40L).map { 4000 + it }
    step("Open a story list long enough to scroll") {
      FakeHackerNews.topStories(*ids.toLongArray())
      ids.forEach { FakeHackerNews.story(it, "Story $it") }
      scenarios.launch<ListActivity>(Intent(context, ListActivity::class.java)).onActivity {
        themeColor = statusBarColorOf(it)
      }
      StoryListScreen { storyTitle("Story 4001").isVisible() }
    }
    step("Scroll down until the toolbar has gone") {
      KView { withId(R.id.recycler_view) }
          .act {
            GeneralSwipeAction(
                Swipe.FAST,
                GeneralLocation.CENTER,
                GeneralLocation.TOP_CENTER,
                Press.FINGER,
            )
          }
      KView { withId(R.id.toolbar) }.assert { scrolledAboveStatusBar() }
    }
    step("Verify the status bar is still solid, not the list scrolling behind it") {
      assertStatusBarIs(themeColor)
    }
  }

  private fun TestContext<Unit>.check(activity: Class<out Activity>) =
      check(Intent(context, activity))

  private fun TestContext<Unit>.check(intent: Intent) {
    val name = intent.component?.shortClassName
    var themeColor = 0
    step("Open $name") {
      scenarios.launch<Activity>(intent).onActivity { themeColor = statusBarColorOf(it) }
    }
    step("Verify $name's toolbar is below the status bar") {
      KView { withId(R.id.toolbar) }.assert { belowStatusBar() }
    }
    step("Verify $name's status bar is solid in the theme's color") {
      assertStatusBarIs(themeColor)
    }
  }

  /**
   * Checks most of the status bar's strip on screen is [color]; the clock, status icons and any
   * camera cutout cover the rest.
   */
  private fun TestContext<Unit>.assertStatusBarIs(color: Int) {
    // only Android 15+ leaves the status bar to the app; earlier versions paint it themselves,
    // some screens with their own tint
    assumeTrue(
        "the system paints the status bar",
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM,
    )
    val statusBar = statusBarHeight()
    assumeTrue("device has no status bar", statusBar > 0)
    flakySafely {
      val screen = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
      val matching = fractionOfColor(screen, color, rows = 4 until statusBar - 4)
      assertTrue(
          "only ${(matching * 100).toInt()}% of the status bar is the theme's color",
          matching > 0.5,
      )
    }
  }

  private fun statusBarHeight(): Int {
    var height = 0
    KView { withId(R.id.toolbar) }
        .assert {
          ViewAssertion { view, noViewFound ->
            if (noViewFound != null) throw noViewFound
            height =
                ViewCompat.getRootWindowInsets(view)
                    ?.getInsets(WindowInsetsCompat.Type.statusBars())
                    ?.top ?: 0
          }
        }
    return height
  }

  /** The color the app paints the status bar on Android 15+: the theme's status bar color. */
  private fun statusBarColorOf(activity: Activity) =
      ContextCompat.getColor(activity, AppUtils.getThemedResId(activity, R.attr.colorPrimaryDark))

  /** Fails until the toolbar has scrolled entirely up behind the status bar. */
  private fun scrolledAboveStatusBar() = ViewAssertion { view, noViewFound ->
    if (noViewFound != null) throw noViewFound
    val statusBar =
        ViewCompat.getRootWindowInsets(view)?.getInsets(WindowInsetsCompat.Type.statusBars())?.top
            ?: 0
    val bottom = IntArray(2).also { view.getLocationOnScreen(it) }[1] + view.height
    if (bottom > statusBar) {
      throw AssertionFailedError("toolbar still shows down to y=$bottom, below y=$statusBar")
    }
  }

  /** The share of pixels across [rows] of [bitmap] that are (nearly) [color]. */
  private fun fractionOfColor(bitmap: Bitmap, color: Int, rows: IntRange): Double {
    var matching = 0
    var total = 0
    for (y in rows) {
      for (x in 0 until bitmap.width) {
        val pixel = bitmap.getPixel(x, y)
        total++
        if (
            abs(Color.red(pixel) - Color.red(color)) <= 8 &&
                abs(Color.green(pixel) - Color.green(color)) <= 8 &&
                abs(Color.blue(pixel) - Color.blue(color)) <= 8
        ) {
          matching++
        }
      }
    }
    return matching.toDouble() / total
  }

  /** Fails unless the view's top edge on screen is at or below the bottom of the status bar. */
  private fun belowStatusBar() = ViewAssertion { view, noViewFound ->
    if (noViewFound != null) throw noViewFound
    val statusBar =
        ViewCompat.getRootWindowInsets(view)?.getInsets(WindowInsetsCompat.Type.statusBars())?.top
            ?: 0
    // ATD images drop SystemUI, so there's no status bar to clear and nothing to check
    assumeTrue("device has no status bar", statusBar > 0)
    val top = IntArray(2).also { view.getLocationOnScreen(it) }[1] + view.paddingTop
    if (top < statusBar) {
      throw AssertionFailedError(
          "${view.context.javaClass.simpleName}: toolbar content starts at y=$top, " +
              "under the status bar, which ends at y=$statusBar",
      )
    }
  }
}
