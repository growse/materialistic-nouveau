package com.growse.android.io.github.hidroh.materialistic

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private const val MINUTE = 60L * 1000L
private const val HOUR = 60L * MINUTE
private const val DAY = 24L * HOUR
private const val WEEK = 7L * DAY

// android.text.format.DateUtils defines a year as 52 weeks, which is what the abbreviation
// thresholds in AppUtils are measured against.
private const val YEAR = 52L * WEEK

class AppUtilsTimeSpanTest :
    StringSpec({
      fun spanAgo(millis: Long) = AppUtils.getAbbreviatedTimeSpan(System.currentTimeMillis() - millis)

      "a fresh timestamp reads as zero minutes" { spanAgo(0) shouldBe "0m" }

      "sub-hour spans read in minutes" {
        spanAgo(5 * MINUTE) shouldBe "5m"
        spanAgo(59 * MINUTE) shouldBe "59m"
      }

      "spans of an hour or more read in hours" {
        spanAgo(HOUR) shouldBe "1h"
        spanAgo(3 * HOUR) shouldBe "3h"
        spanAgo(23 * HOUR) shouldBe "23h"
      }

      "spans of a day or more read in days" {
        spanAgo(DAY) shouldBe "1d"
        spanAgo(6 * DAY) shouldBe "6d"
      }

      "spans of a week or more read in weeks" {
        spanAgo(WEEK) shouldBe "1w"
        spanAgo(51 * WEEK) shouldBe "51w"
      }

      "spans of a year or more read in years" {
        spanAgo(YEAR) shouldBe "1y"
        spanAgo(2 * YEAR) shouldBe "2y"
      }

      "future timestamps clamp to zero rather than going negative" {
        AppUtils.getAbbreviatedTimeSpan(System.currentTimeMillis() + DAY) shouldBe "0m"
      }
    })

class AppUtilsUrlEqualsTest :
    StringSpec({
      "a trailing slash is not significant" {
        AppUtils.urlEquals("http://example.com", "http://example.com/") shouldBe true
        AppUtils.urlEquals("http://example.com/", "http://example.com") shouldBe true
      }

      "identical urls are equal with or without a trailing slash" {
        AppUtils.urlEquals("http://example.com/a", "http://example.com/a") shouldBe true
        AppUtils.urlEquals("http://example.com/a/", "http://example.com/a/") shouldBe true
      }

      "different urls are not equal" {
        AppUtils.urlEquals("http://example.com", "http://example.org") shouldBe false
        AppUtils.urlEquals("http://example.com/a", "http://example.com/b") shouldBe false
      }

      "comparison is case and scheme sensitive" {
        AppUtils.urlEquals("http://example.com", "https://example.com") shouldBe false
        AppUtils.urlEquals("http://example.com/A", "http://example.com/a") shouldBe false
      }

      "a blank url never matches, even another blank one" {
        AppUtils.urlEquals(null, "http://example.com") shouldBe false
        AppUtils.urlEquals("http://example.com", null) shouldBe false
        AppUtils.urlEquals("", "") shouldBe false
        AppUtils.urlEquals(null, null) shouldBe false
      }
    })
