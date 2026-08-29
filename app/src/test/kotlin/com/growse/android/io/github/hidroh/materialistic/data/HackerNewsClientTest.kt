package com.growse.android.io.github.hidroh.materialistic.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class HackerNewsClientTest :
    StringSpec({
      "the web item path is built from the base web url" {
        String.format(HackerNewsClient.WEB_ITEM_PATH, "1234") shouldBe
            "https://news.ycombinator.com/item?id=1234"
      }

      "the api host matches the base web url's provider" {
        HackerNewsClient.HOST shouldBe "hacker-news.firebaseio.com"
        HackerNewsClient.BASE_WEB_URL shouldBe "https://news.ycombinator.com"
      }
    })
