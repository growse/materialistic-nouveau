package com.growse.android.io.github.hidroh.materialistic.testing

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.ViewAssertion
import io.github.kakaocup.kakao.common.views.KView
import junit.framework.AssertionFailedError

/**
 * Waits until this RecyclerView has no pending adapter updates or running item animations.
 *
 * When a row's item finishes loading, RecyclerView rebinds it into a new view and briefly keeps the
 * outgoing one on top while it swaps them. Espresso doesn't wait for that, so a tap aimed at the
 * new row can land on the outgoing view and be lost. Kaspresso retries this check until it passes.
 */
fun KView.waitUntilSettled() = assert {
  ViewAssertion { view, noViewFound ->
    if (noViewFound != null) throw noViewFound
    val recyclerView = view as RecyclerView
    if (recyclerView.isAnimating || recyclerView.hasPendingAdapterUpdates()) {
      throw AssertionFailedError("List still settling")
    }
  }
}
