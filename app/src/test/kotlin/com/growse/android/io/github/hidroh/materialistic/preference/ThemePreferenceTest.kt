package com.growse.android.io.github.hidroh.materialistic.preference

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ThemePreferenceIsSystemThemeTest :
    StringSpec({
      "the literal system value is the system theme" {
        ThemePreference.isSystemTheme("system") shouldBe true
      }

      "an empty value is the system theme, since System is the default" {
        ThemePreference.isSystemTheme("") shouldBe true
      }

      "a null value is the system theme, since System is the default" {
        ThemePreference.isSystemTheme(null) shouldBe true
      }

      "any other theme value is not the system theme" {
        ThemePreference.isSystemTheme("light") shouldBe false
        ThemePreference.isSystemTheme("dark") shouldBe false
        ThemePreference.isSystemTheme("sepia") shouldBe false
      }
    })
