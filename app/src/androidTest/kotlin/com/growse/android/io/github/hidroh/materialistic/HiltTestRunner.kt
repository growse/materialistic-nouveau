package com.growse.android.io.github.hidroh.materialistic

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.CustomTestApplication

/**
 * Hilt's test application, built on [BaseApplication] so tests get the same startup as the app
 * (night mode, typeface, preference migrations) rather than a bare Application.
 */
@CustomTestApplication(BaseApplication::class) interface MaterialisticTestApplication

/**
 * Runs instrumented tests under [MaterialisticTestApplication], so test modules can be swapped in.
 */
class HiltTestRunner : AndroidJUnitRunner() {
  override fun newApplication(
      cl: ClassLoader?,
      className: String?,
      context: Context?,
  ): Application =
      super.newApplication(cl, MaterialisticTestApplication_Application::class.java.name, context)
}
