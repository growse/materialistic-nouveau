package com.growse.android.io.github.hidroh.materialistic.ktx

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.Closeable
import java.io.IOException

class ExtensionsTest :
    StringSpec({
      "closeQuietly closes the resource" {
        var closed = false

        Closeable { closed = true }.closeQuietly()

        closed shouldBe true
      }

      "closeQuietly swallows a failure while closing" {
        var attempted = false

        Closeable {
              attempted = true
              throw IOException("boom")
            }
            .closeQuietly()

        attempted shouldBe true
      }
    })
