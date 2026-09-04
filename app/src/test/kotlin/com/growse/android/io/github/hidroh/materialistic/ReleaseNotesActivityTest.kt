package com.growse.android.io.github.hidroh.materialistic

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ReleaseNotesActivityTest :
    StringSpec({
      "a single line of notes is wrapped in a paragraph" {
        ReleaseNotesActivity.buildNotesBody("Fixed a bug.", "unused") shouldBe "<p>Fixed a bug.</p>"
      }

      "multiple lines each become their own paragraph" {
        ReleaseNotesActivity.buildNotesBody("First change.\nSecond change.", "unused") shouldBe
            "<p>First change.</p><p>Second change.</p>"
      }

      "blank lines between real lines are dropped" {
        ReleaseNotesActivity.buildNotesBody("First change.\n\n\nSecond change.", "unused") shouldBe
            "<p>First change.</p><p>Second change.</p>"
      }

      "empty notes fall back to the unavailable message" {
        ReleaseNotesActivity.buildNotesBody("", "No release notes available.") shouldBe
            "<p>No release notes available.</p>"
      }

      "blank (whitespace-only) notes also fall back to the unavailable message" {
        ReleaseNotesActivity.buildNotesBody("   \n  ", "No release notes available.") shouldBe
            "<p>No release notes available.</p>"
      }
    })
