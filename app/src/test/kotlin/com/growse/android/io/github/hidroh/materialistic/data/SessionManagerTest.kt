package com.growse.android.io.github.hidroh.materialistic.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import rx.schedulers.Schedulers

class SessionManagerTest :
    StringSpec({
      lateinit var cache: LocalCache
      lateinit var sessionManager: SessionManager

      beforeTest {
        cache = mock()
        // Schedulers.immediate() keeps view() synchronous so the test can assert without waiting.
        sessionManager = SessionManager(Schedulers.immediate(), cache)
      }

      "a viewed item is reported as viewed" {
        whenever(cache.isViewed("1")).thenReturn(true)

        sessionManager.isViewed("1").toBlocking().single() shouldBe true
      }

      "an unviewed item is reported as not viewed" {
        whenever(cache.isViewed("1")).thenReturn(false)

        sessionManager.isViewed("1").toBlocking().single() shouldBe false
      }

      "a null item id is not viewed and is never looked up" {
        sessionManager.isViewed(null).toBlocking().single() shouldBe false

        verifyNoInteractions(cache)
      }

      "an empty item id is not viewed and is never looked up" {
        sessionManager.isViewed("").toBlocking().single() shouldBe false

        verifyNoInteractions(cache)
      }

      "viewing an item marks it in the cache" {
        sessionManager.view("1")

        verify(cache).setViewed("1")
      }

      "viewing a null item id is a no-op" {
        sessionManager.view(null)

        verify(cache, never()).setViewed(any())
      }

      "viewing an empty item id is a no-op" {
        sessionManager.view("")

        verify(cache, never()).setViewed(any())
      }
    })
