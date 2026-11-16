package com.growse.android.io.github.hidroh.materialistic.testing

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.rules.ExternalResource

/**
 * Launches activities for a test once it has set up the fakes they read on start, and afterwards
 * finishes every activity still open, including ones the app started itself, so none leaks into the
 * next test.
 */
class ScenarioRule : ExternalResource() {
  private val scenarios = mutableListOf<ActivityScenario<*>>()

  fun <A : Activity> launch(activity: Class<A>) =
      launch<A>(Intent(ApplicationProvider.getApplicationContext(), activity))

  /** Opens [activity] with [link], the way following that link from elsewhere would. */
  fun <A : Activity> launchLink(activity: Class<A>, link: String) =
      launch<A>(
          Intent(ApplicationProvider.getApplicationContext(), activity)
              .setAction(Intent.ACTION_VIEW)
              .setData(Uri.parse(link)),
      )

  fun <A : Activity> launch(intent: Intent): ActivityScenario<A> =
      ActivityScenario.launch<A>(intent).also { scenarios += it }

  override fun after() {
    scenarios.forEach { it.close() }
    scenarios.clear()
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      val monitor = ActivityLifecycleMonitorRegistry.getInstance()
      listOf(Stage.RESUMED, Stage.PAUSED, Stage.STOPPED, Stage.CREATED, Stage.STARTED)
          .flatMap { monitor.getActivitiesInStage(it) }
          .forEach { it.finish() }
    }
  }
}
