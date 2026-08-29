package com.growse.android.io.github.hidroh.materialistic

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * [AndroidUtils.TextUtils] is a hand-rolled copy of the platform helper so it can be exercised off
 * device — these tests pin it to the platform semantics it claims to reproduce.
 */
class AndroidUtilsTextUtilsTest :
    StringSpec({
      "isEmpty treats null and the empty string as empty" {
        AndroidUtils.TextUtils.isEmpty(null) shouldBe true
        AndroidUtils.TextUtils.isEmpty("") shouldBe true
      }

      "isEmpty treats whitespace as non-empty" {
        AndroidUtils.TextUtils.isEmpty(" ") shouldBe false
        AndroidUtils.TextUtils.isEmpty("a") shouldBe false
      }

      "equals matches two nulls" { AndroidUtils.TextUtils.equals(null, null) shouldBe true }

      "equals rejects a null against a value" {
        AndroidUtils.TextUtils.equals(null, "a") shouldBe false
        AndroidUtils.TextUtils.equals("a", null) shouldBe false
      }

      "equals compares strings by content" {
        AndroidUtils.TextUtils.equals("abc", "abc") shouldBe true
        AndroidUtils.TextUtils.equals("abc", "abd") shouldBe false
        AndroidUtils.TextUtils.equals("abc", "ab") shouldBe false
      }

      "equals compares non-string char sequences character by character" {
        AndroidUtils.TextUtils.equals(StringBuilder("abc"), StringBuilder("abc")) shouldBe true
        AndroidUtils.TextUtils.equals(StringBuilder("abc"), StringBuilder("abd")) shouldBe false
      }

      "equals short-circuits on identity" {
        val sequence: CharSequence = StringBuilder("abc")
        AndroidUtils.TextUtils.equals(sequence, sequence) shouldBe true
      }
    })
