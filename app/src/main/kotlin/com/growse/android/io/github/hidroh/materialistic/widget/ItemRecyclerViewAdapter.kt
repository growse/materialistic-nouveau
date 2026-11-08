/*
 * Copyright (c) 2015 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.growse.android.io.github.hidroh.materialistic.widget

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.core.content.withStyledAttributes
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.growse.android.io.github.hidroh.materialistic.AlertDialogBuilder
import com.growse.android.io.github.hidroh.materialistic.AppUtils
import com.growse.android.io.github.hidroh.materialistic.ComposeActivity
import com.growse.android.io.github.hidroh.materialistic.Navigable
import com.growse.android.io.github.hidroh.materialistic.Preferences
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServices
import com.growse.android.io.github.hidroh.materialistic.data.Item
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager
import com.growse.android.io.github.hidroh.materialistic.data.ResponseListener
import java.lang.ref.WeakReference

abstract class ItemRecyclerViewAdapter<VH : ItemRecyclerViewAdapter.ItemViewHolder>(
    private val itemManager: ItemManager,
    private val userServices: UserServices,
    private val popupMenu: PopupMenu,
    private val alertDialogBuilder: AlertDialogBuilder<*>?,
) : RecyclerViewAdapter<VH>() {
  lateinit var layoutInflater: LayoutInflater
    protected set

  var cacheMode = ItemManager.MODE_DEFAULT

  private var tertiaryTextColor = 0
  private var secondaryTextColor = 0
  private var cardBackgroundColor = 0
  private var cardHighlightColor = 0
  private var contentMaxLines = Int.MAX_VALUE
  private var username: String? = null
  private var lineHeight = 1.0f
  private val lineCounted = mutableMapOf<String, Int>()
  private val failedAt = mutableMapOf<String, Long>()

  private val isAttached: Boolean
    get() = context != null

  fun interface PositionCallback {
    fun onPosition(position: Int)
  }

  override fun attach(context: Context, recyclerView: RecyclerView) {
    super.attach(context, recyclerView)
    layoutInflater = AppUtils.createLayoutInflater(context)
    context.withStyledAttributes(
        null,
        intArrayOf(
            android.R.attr.textColorTertiary,
            android.R.attr.textColorSecondary,
            R.attr.colorCardBackground,
            R.attr.colorCardHighlight,
        ),
    ) {
      tertiaryTextColor = getInt(0, 0)
      secondaryTextColor = getInt(1, 0)
      cardBackgroundColor = getInt(2, 0)
      cardHighlightColor = getInt(3, 0)
    }
  }

  override fun onBindViewHolder(holder: VH, position: Int) {
    val item = getItem(position) ?: return
    clear(holder)
    val revision = item.localRevision
    when {
      revision == REVISION_FAILED && shouldRetry(item) -> load(holder.absoluteAdapterPosition, item)
      revision == REVISION_FAILED -> bindError(holder, item)
      revision < REVISION_LOADING -> load(holder.absoluteAdapterPosition, item)
      revision > REVISION_LOADING -> bind(holder, item)
    }
  }

  override fun getItemId(position: Int): Long = getItem(position)?.longId ?: RecyclerView.NO_ID

  open fun initDisplayOptions(context: Context) {
    contentMaxLines = Preferences.getCommentMaxLines(context)
    username = Preferences.getUsername(context)
    lineHeight = Preferences.getLineHeight(context)
  }

  open fun getNextPosition(position: Int, direction: Int, callback: PositionCallback) {
    when (direction) {
      Navigable.DIRECTION_UP -> callback.onPosition(position - 1)
      Navigable.DIRECTION_DOWN -> callback.onPosition(position + 1)
    }
  }

  open fun lockBinding(lock: IntArray?) {}

  protected abstract fun getItem(position: Int): Item?

  override fun onViewRecycled(holder: VH) {
    super.onViewRecycled(holder)
    holder.stopLoadingPulse()
  }

  @CallSuper
  protected open fun bind(holder: VH, item: Item) {
    holder.stopLoadingPulse()
    holder.contentView.setBackgroundColor(
        if (!username.isNullOrEmpty() && username == item.by) cardHighlightColor
        else cardBackgroundColor,
    )
    holder.contentTextView.setTextColor(if (item.isDead) secondaryTextColor else tertiaryTextColor)
    holder.contentTextView.setLineSpacing(0f, lineHeight)
    AppUtils.setTextWithLinks(holder.contentTextView, item.displayedText)
    val lineCount = lineCounted[item.id]
    if (lineCount != null && lineCount > 0) {
      toggleCollapsibleContent(holder, item, lineCount)
    } else {
      holder.contentTextView.post {
        if (!isAttached) {
          return@post
        }
        val count = holder.contentTextView.lineCount
        lineCounted[item.id] = count
        toggleCollapsibleContent(holder, item, count)
      }
    }
    bindActions(holder, item)
  }

  protected open fun clear(holder: VH) {
    holder.commentButton.isGone = true
    holder.postedTextView.setOnClickListener(null)
    // drop any retry listener from a recycled row, and undo bindError()'s selection change
    holder.contentView.setOnClickListener(null)
    holder.contentTextView.setOnClickListener(null)
    holder.contentTextView.setTextIsSelectable(true)
    holder.postedTextView.setText(R.string.loading_text)
    holder.contentTextView.setText(R.string.loading_text)
    holder.startLoadingPulse()
    holder.readMoreTextView.isGone = true
    // an unbound row has no item to act on, and a recycled holder would otherwise keep the
    // previous item's menu until bindActions() runs
    holder.moreButton.isInvisible = true
  }

  protected open fun onItemLoaded(position: Int, item: Item) {
    if (position < itemCount) {
      notifyItemChanged(position)
    }
  }

  /**
   * Leaves a failed item on the loading placeholder set by [clear], rather than an error, but makes
   * it tappable so a comment lost to a transient network failure can be recovered without scrolling
   * it out of view and back.
   */
  private fun bindError(holder: VH, item: Item) {
    // a selectable text view consumes the tap without ever performing a click, and posted stays
    // clickable once a previous bind gave it a listener - so drop selection and wire all three
    holder.contentTextView.setTextIsSelectable(false)
    val retry = View.OnClickListener {
      item.localRevision = REVISION_NONE
      val position = holder.absoluteAdapterPosition
      if (position != RecyclerView.NO_POSITION) {
        notifyItemChanged(position)
      }
    }
    holder.contentView.setOnClickListener(retry)
    holder.postedTextView.setOnClickListener(retry)
    holder.contentTextView.setOnClickListener(retry)
  }

  /**
   * Whether a failed item should be refetched as its row binds, so that a comment lost while
   * offline loads once it is viewed again with a connection. The failure's own rebind, or any other
   * within [RETRY_BACKOFF_MILLIS], is left alone so a failing request can't loop.
   */
  private fun shouldRetry(item: Item): Boolean {
    val context = context ?: return false
    val failedAt = failedAt[item.id]
    if (failedAt != null && SystemClock.elapsedRealtime() - failedAt < RETRY_BACKOFF_MILLIS) {
      return false
    }
    return AppUtils.hasConnection(context)
  }

  private fun load(adapterPosition: Int, item: Item) {
    item.localRevision = REVISION_LOADING
    itemManager.getItem(item.id, cacheMode, ItemResponseListener(this, adapterPosition, item))
  }

  private fun onItemLoadFailed(position: Int, item: Item) {
    item.localRevision = REVISION_FAILED
    failedAt[item.id] = SystemClock.elapsedRealtime()
    if (position < itemCount) {
      notifyItemChanged(position)
    }
  }

  private fun toggleCollapsibleContent(holder: VH, item: Item, lineCount: Int) {
    if (item.isContentExpanded || lineCount <= contentMaxLines) {
      holder.contentTextView.maxLines = Int.MAX_VALUE
      holder.readMoreTextView.isGone = true
      return
    }
    holder.contentTextView.maxLines = contentMaxLines
    holder.readMoreTextView.apply {
      isVisible = true
      text = context.getString(R.string.read_more, lineCount)
      setOnClickListener { v ->
        item.isContentExpanded = true
        v.isGone = true
        ObjectAnimator.ofInt(holder.contentTextView, PROPERTY_MAX_LINES, lineCount)
            .setDuration(((lineCount - contentMaxLines) * DURATION_PER_LINE_MILLIS).toLong())
            .start()
      }
    }
  }

  private fun bindActions(holder: VH, item: Item) {
    if (item.isDead || item.isDeleted) {
      holder.moreButton.isInvisible = true
      return
    }
    holder.moreButton.isVisible = true
    holder.moreButton.setOnClickListener {
      val context = context ?: return@setOnClickListener
      popupMenu
          .create(context, holder.moreButton, Gravity.NO_GRAVITY)
          .inflate(R.menu.menu_contextual_comment)
          .setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
              R.id.menu_contextual_vote -> userServices.voteUp(context, item.id, VoteCallback(this))
              R.id.menu_contextual_comment ->
                  context.startActivity(
                      Intent(context, ComposeActivity::class.java)
                          .putExtra(ComposeActivity.EXTRA_PARENT_ID, item.id)
                          .putExtra(ComposeActivity.EXTRA_PARENT_TEXT, item.text),
                  )
              R.id.menu_contextual_share ->
                  AppUtils.share(
                      context,
                      if (item.isStoryType) item.displayedTitle else null,
                      if (item.isStoryType) item.url else item.displayedText?.toString(),
                  )
              else -> return@setOnMenuItemClickListener false
            }
            true
          }
          .show()
    }
  }

  private fun onVoted(successful: Boolean?) {
    when (successful) {
      null -> Toast.makeText(context, R.string.vote_failed, Toast.LENGTH_SHORT).show()
      true -> Toast.makeText(context, R.string.voted, Toast.LENGTH_SHORT).show()
      false -> {
        val context = context ?: return
        AppUtils.showLogin(context, alertDialogBuilder ?: return)
      }
    }
  }

  open class ItemViewHolder private constructor(itemView: View, val isFooter: Boolean) :
      RecyclerView.ViewHolder(itemView) {
    lateinit var postedTextView: TextView
    lateinit var contentTextView: TextView
    lateinit var readMoreTextView: TextView
    lateinit var commentButton: TextView
    lateinit var moreButton: View
    lateinit var contentView: View
    private var loadingPulse: ValueAnimator? = null

    /** Gently pulses the placeholder text so a pending row reads as in progress. */
    internal fun startLoadingPulse() {
      if (loadingPulse != null) {
        return
      }
      loadingPulse =
          ValueAnimator.ofFloat(1f, LOADING_PULSE_MIN_ALPHA).apply {
            duration = LOADING_PULSE_MILLIS
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
              val alpha = it.animatedValue as Float
              postedTextView.alpha = alpha
              contentTextView.alpha = alpha
            }
            start()
          }
    }

    internal fun stopLoadingPulse() {
      loadingPulse?.cancel()
      loadingPulse = null
      if (!isFooter) {
        postedTextView.alpha = 1f
        contentTextView.alpha = 1f
      }
    }

    constructor(itemView: View) : this(itemView, isFooter = false) {
      postedTextView = itemView.findViewById(R.id.posted)
      postedTextView.movementMethod = LinkMovementMethod.getInstance()
      contentTextView = itemView.findViewById(R.id.text)
      readMoreTextView = itemView.findViewById(R.id.more)
      commentButton = itemView.findViewById(R.id.comment)
      commentButton.isGone = true
      moreButton = itemView.findViewById(R.id.button_more)
      contentView = itemView.findViewById(R.id.content)
    }

    /** Creates a footer holder, which has none of the item views. */
    @Suppress("UNUSED_PARAMETER")
    constructor(itemView: View, payload: Any?) : this(itemView, isFooter = true)
  }

  private class ItemResponseListener(
      adapter: ItemRecyclerViewAdapter<*>,
      private val position: Int,
      private val partialItem: Item,
  ) : ResponseListener<Item?> {
    private val adapter = WeakReference(adapter)

    override fun onResponse(response: Item?) {
      if (response == null) {
        // a missing body leaves the item unpopulated, so treat it as a failure rather
        // than stranding the row on the loading placeholder forever
        onError(null)
        return
      }
      // populate regardless of whether the adapter is still attached, otherwise a detached
      // adapter leaves the item stuck mid-load and it is never rebound or retried
      partialItem.populate(response)
      adapter.get()?.takeIf { it.isAttached }?.onItemLoaded(position, partialItem)
    }

    override fun onError(errorMessage: String?) {
      val adapter = adapter.get()?.takeIf { it.isAttached }
      if (adapter != null) {
        adapter.onItemLoadFailed(position, partialItem)
      } else {
        partialItem.localRevision = REVISION_FAILED
      }
    }
  }

  private class VoteCallback(adapter: ItemRecyclerViewAdapter<*>) : UserServices.Callback() {
    private val adapter = WeakReference(adapter)

    override fun onDone(successful: Boolean) {
      adapter.get()?.takeIf { it.isAttached }?.onVoted(successful)
    }

    override fun onError(throwable: Throwable?) {
      adapter.get()?.takeIf { it.isAttached }?.onVoted(null)
    }
  }

  companion object {
    private const val PROPERTY_MAX_LINES = "maxLines"
    private const val DURATION_PER_LINE_MILLIS = 20
    private const val RETRY_BACKOFF_MILLIS = 3_000L
    private const val LOADING_PULSE_MILLIS = 800L
    private const val LOADING_PULSE_MIN_ALPHA = 0.35f

    /**
     * Local revision states of an [Item]. Anything above [REVISION_LOADING] has been populated and
     * can be bound; [REVISION_FAILED] is distinct from [REVISION_NONE] so that a failed row renders
     * an error instead of immediately refetching on every rebind.
     */
    private const val REVISION_FAILED = -2
    private const val REVISION_NONE = -1
    private const val REVISION_LOADING = 0
  }
}
