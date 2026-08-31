package com.growse.android.io.github.hidroh.materialistic

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PreferencesThemeDefaultColorChoiceTest :
    StringSpec({
      // DynamicColors.isDynamicColorAvailable() can't be true in a plain JVM unit test (no real
      // Build.VERSION.SDK_INT), so this only exercises the "unavailable" fallback path - the
      // "system" path is covered by the instrumented DisplayPreferencesActivityTest instead.
      "falls back to the given default when dynamic color is unavailable" {
        Preferences.Theme.defaultColorChoice("purple") shouldBe "purple"
        Preferences.Theme.defaultColorChoice("red") shouldBe "red"
      }
    })
