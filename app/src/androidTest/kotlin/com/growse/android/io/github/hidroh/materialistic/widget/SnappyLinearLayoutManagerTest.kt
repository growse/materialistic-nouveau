package com.growse.android.io.github.hidroh.materialistic.widget

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnappyLinearLayoutManagerTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val rowHeight = context.resources.displayMetrics.heightPixels / ROWS_PER_SCREEN

  @Test
  fun bindsOnlyVisibleRowsWithoutPreload() {
    assertEquals(ROWS_PER_SCREEN, rowsBoundByFirstLayout(preloadScreens = 0))
  }

  @Test
  fun bindsRowsForEachPreloadedScreen() {
    val bound = rowsBoundByFirstLayout(preloadScreens = 3)

    assertTrue("bound $bound rows", bound >= ROWS_PER_SCREEN * 4)
    assertTrue("bound $bound rows", bound <= ROWS_PER_SCREEN * 4 + 1)
  }

  private fun rowsBoundByFirstLayout(preloadScreens: Int): Int {
    val bound = mutableSetOf<Int>()
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      RecyclerView(context).apply {
        layoutManager = SnappyLinearLayoutManager(context, preloadScreens)
        adapter = RowAdapter(bound)
        val height = rowHeight * ROWS_PER_SCREEN
        measure(
            View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        layout(0, 0, WIDTH, height)
      }
    }
    return bound.size
  }

  private inner class RowAdapter(private val bound: MutableSet<Int>) :
      RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun getItemCount() = ROWS_PER_SCREEN * 20

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        object :
            RecyclerView.ViewHolder(
                View(context).apply {
                  layoutParams =
                      RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowHeight)
                },
            ) {}

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
      bound.add(position)
    }
  }

  private companion object {
    const val ROWS_PER_SCREEN = 10
    const val WIDTH = 100
  }
}
