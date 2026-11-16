package com.growse.android.io.github.hidroh.materialistic.data

import androidx.test.core.app.ApplicationProvider
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackend
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackendRule
import com.growse.android.io.github.hidroh.materialistic.testing.FakeHackerNews
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Saving a story for offline reading: what a sync fetches, stores and skips. */
@HiltAndroidTest
class SyncDelegateTest {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val backend = FakeBackendRule()

  @Inject lateinit var factory: RestServiceFactory

  @Inject lateinit var readabilityClient: ReadabilityClient

  @Inject lateinit var localCache: LocalCache

  @Before
  fun inject() {
    hiltRule.inject()
  }

  @Test
  fun storesTheStoryAndItsWholeCommentTree() {
    FakeHackerNews.story(STORY, "Saved story", kids = listOf(2001, 2002))
    FakeHackerNews.comment(2001, parent = STORY, text = "First", kids = listOf(3001))
    FakeHackerNews.comment(2002, parent = STORY, text = "Second")
    FakeHackerNews.comment(3001, parent = 2001, text = "Reply")

    sync(job())

    assertEquals("Saved story", cached(STORY)?.title)
    assertEquals("First", cached(2001)?.text)
    assertEquals("Second", cached(2002)?.text)
    assertEquals("Reply", cached(3001)?.text)
  }

  @Test
  fun withoutCommentsOnlyTheStoryIsFetched() {
    FakeHackerNews.story(STORY, "Saved story", kids = listOf(2001))
    FakeHackerNews.comment(2001, parent = STORY, text = "Not wanted")

    sync(job(comments = false))

    assertNotNull(cached(STORY))
    assertNull(cached(2001))
    assertFalse(requested(2001))
  }

  @Test
  fun freshlyStoredItemsAreNotFetchedAgain() {
    store(STORY, "Stored story", kids = listOf(2001), ageMillis = MINUTE)
    FakeHackerNews.comment(2001, parent = STORY, text = "Comment")

    sync(job())

    assertFalse("a fresh story should be reused", requested(STORY))
    assertTrue("its comments should still be walked", requested(2001))
    assertEquals("Comment", cached(2001)?.text)
  }

  @Test
  fun staleStoredItemsAreFetchedAgain() {
    store(STORY, "Old title", ageMillis = 31 * MINUTE)
    FakeHackerNews.story(STORY, "New title")

    sync(job())

    assertTrue(requested(STORY))
    assertEquals("New title", cached(STORY)?.title)
  }

  @Test
  fun aRefreshFetchesEvenFreshItemsToFindNewReplies() {
    store(STORY, "Stored story", ageMillis = MINUTE)
    FakeHackerNews.story(STORY, "Stored story", kids = listOf(2001))
    FakeHackerNews.comment(2001, parent = STORY, text = "Posted since")

    sync(job(refresh = true))

    assertTrue(requested(STORY))
    assertEquals("Posted since", cached(2001)?.text)
  }

  @Test
  fun aCommentThatFailsToLoadDoesNotStopTheSyncFinishing() {
    FakeHackerNews.story(STORY, "Saved story", kids = listOf(2001, 2002))
    FakeHackerNews.comment(2002, parent = STORY, text = "Loads fine")
    // 2001 is left unserved, so it fails with a 404

    sync(job())

    assertNull(cached(2001))
    assertEquals("Loads fine", cached(2002)?.text)
  }

  @Test
  fun withReadabilityTheArticleTextIsStored() {
    FakeHackerNews.story(STORY, "Saved story", url = "https://example.com/article")
    FakeBackend.respond(
        "https://${ReadabilityClient.HOST}/parser",
        """{"content":"<p>The article, made readable</p>"}""",
    )

    sync(job(readability = true))

    assertEquals("<p>The article, made readable</p>", localCache.getReadability(STORY.toString()))
    val parse = FakeBackend.requests.single { it.host == ReadabilityClient.HOST }
    assertEquals("https://example.com/article", parse.queryParameter("url"))
  }

  private fun job(
      comments: Boolean = true,
      refresh: Boolean = false,
      readability: Boolean = false,
  ) =
      SyncDelegate.Job(STORY.toString()).apply {
        commentsEnabled = comments
        readabilityEnabled = readability
        this.refresh = refresh
      }

  /** Runs [job] and waits for it to report it's done. */
  private fun sync(job: SyncDelegate.Job) {
    val done = CountDownLatch(1)
    SyncDelegate(
            ApplicationProvider.getApplicationContext(),
            factory,
            readabilityClient,
            localCache,
        )
        .apply { subscribe { done.countDown() } }
        .performSync(job)
    assertTrue("sync didn't finish", done.await(10, TimeUnit.SECONDS))
  }

  private fun store(id: Long, title: String, kids: List<Long> = emptyList(), ageMillis: Long) =
      localCache.putCachedItem(
          MaterialisticDatabase.CachedItem(id.toString()).apply {
            type = "story"
            this.title = title
            this.kids = kids.joinToString(",")
            fetchedAt = System.currentTimeMillis() - ageMillis
          },
      )

  private fun cached(id: Long) = localCache.getCachedItem(id.toString())

  private fun requested(id: Long) =
      FakeBackend.requests.any { it.encodedPath.endsWith("/item/$id.json") }

  private companion object {
    const val STORY = 1001L
    const val MINUTE = 60_000L
  }
}
