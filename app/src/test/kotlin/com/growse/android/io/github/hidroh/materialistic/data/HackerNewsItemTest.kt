package com.growse.android.io.github.hidroh.materialistic.data

import com.google.gson.Gson
import com.growse.android.io.github.hidroh.materialistic.Navigable
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class HackerNewsItemTest :
    StringSpec({
      fun parse(json: String) = Gson().fromJson(json, HackerNewsItem::class.java)

      "parses every API field, including the ones renamed in Kotlin" {
        val item =
            parse(
                """
                {"id": 8863, "type": "job", "by": "dhouston", "time": 1175714200,
                 "text": "<p>hi</p>", "url": "https://example.com/", "title": "My YC app",
                 "score": 111, "descendants": 71, "parent": 8000, "kids": [8952, 9224],
                 "dead": true, "deleted": true}
                """
            )

        item.longId shouldBe 8863L
        item.id shouldBe "8863"
        item.rawType shouldBe "job"
        item.type shouldBe WebItem.JOB_TYPE
        item.by shouldBe "dhouston"
        item.time shouldBe 1175714200L
        item.text shouldBe "<p>hi</p>"
        item.rawUrl shouldBe "https://example.com/"
        item.title shouldBe "My YC app"
        item.score shouldBe 111
        item.descendants shouldBe 71
        item.parent shouldBe "8000"
        item.kids!!.toList() shouldBe listOf(8952L, 9224L)
        item.isDead shouldBe true
        item.isDeleted shouldBe true
      }

      "defaults to a story when the type is missing" {
        val item = parse("""{"id": 1, "title": "Untyped"}""")

        item.type shouldBe WebItem.STORY_TYPE
        item.isStoryType shouldBe true
        item.displayedTitle shouldBe "Untyped"
      }

      "comments link to their HN page and show their text as the title" {
        val item = parse("""{"id": 2, "type": "comment", "text": "A comment"}""")

        item.isStoryType shouldBe false
        item.displayedTitle shouldBe "A comment"
        item.url shouldBe "https://news.ycombinator.com/item?id=2"
      }

      "stories without a url fall back to their HN page" {
        parse("""{"id": 3, "type": "story"}""").url shouldBe
            "https://news.ycombinator.com/item?id=3"
        parse("""{"id": 4, "type": "story", "url": "https://a.b/"}""").url shouldBe "https://a.b/"
      }

      "kid items are ranked one level deeper and linked to their siblings" {
        val item = parse("""{"id": 10, "kids": [11, 12, 13]}""")

        val kids = item.kidItems
        kids.map { it.longId } shouldBe listOf(11L, 12L, 13L)
        kids.map { it.rank } shouldBe listOf(1, 2, 3)
        kids.map { it.level } shouldBe listOf(1, 1, 1)
        kids[1].getNeighbour(Navigable.DIRECTION_UP) shouldBe 11L
        kids[1].getNeighbour(Navigable.DIRECTION_DOWN) shouldBe 13L
        parse("""{"id": 20}""").kidItems.shouldBeEmpty()
      }

      "survives a round trip through the local cache" {
        val item =
            parse(
                """
                {"id": 5, "type": "story", "by": "pg", "time": 100, "title": "T",
                 "url": "https://a.b/", "score": 7, "descendants": 2, "parent": 1,
                 "kids": [6, 7], "dead": true}
                """
            )

        val restored = HackerNewsItem.fromCachedItem(HackerNewsItem.toCachedItem(item))

        restored.longId shouldBe 5L
        restored.rawType shouldBe "story"
        restored.by shouldBe "pg"
        restored.time shouldBe 100L
        restored.title shouldBe "T"
        restored.rawUrl shouldBe "https://a.b/"
        restored.score shouldBe 7
        restored.descendants shouldBe 2
        restored.parent shouldBe "1"
        restored.kids!!.toList() shouldBe listOf(6L, 7L)
        restored.isDead shouldBe true
        restored.localRevision shouldBe 1
      }
    })
