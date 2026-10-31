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

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.SuperscriptSpan
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.widget.Checkable
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.ViewSwitcher
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.growse.android.io.github.hidroh.materialistic.AppUtils
import com.growse.android.io.github.hidroh.materialistic.AppUtils.getThemedResId
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.data.Item
import com.growse.android.io.github.hidroh.materialistic.data.WebItem
import java.util.Locale

class StoryView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : RelativeLayout(context, attrs, defStyle), Checkable {
  private val mBackgroundColor: Int
  private val mHighlightColor: Int
  private val mTertiaryTextColorResId: Int
  private val mSecondaryTextColorResId: Int
  private val mPromotedColorResId: Int
  private val mHotColorResId: Int
  private val mAccentColorResId: Int
  private val mRankTextView: TextView
  val mScoreTextView: TextView
  private val mBookmarked: View
  private val mPostedTextView: TextView
  private val mTitleTextView: TextView
  private val mSourceTextView: TextView
  private val mCommentButton: TextView
  private val mIsLocal: Boolean
  val mVoteSwitcher: ViewSwitcher
  val moreOptions: View
  private val mCommentDrawable: Drawable
  private val mBackground: View
  private var mChecked = false

  init {
    val ta = context.obtainStyledAttributes(attrs, R.styleable.StoryView)
    mIsLocal = ta.getBoolean(R.styleable.StoryView_local, false)
    val a =
        context.theme.obtainStyledAttributes(
            intArrayOf(
                android.R.attr.textColorTertiary,
                android.R.attr.textColorSecondary,
                R.attr.colorCardBackground,
                R.attr.colorCardHighlight,
            )
        )
    mTertiaryTextColorResId = ContextCompat.getColor(context, a.getResourceId(0, 0))
    mSecondaryTextColorResId = ContextCompat.getColor(context, a.getResourceId(1, 0))
    mBackgroundColor = ContextCompat.getColor(context, a.getResourceId(2, 0))
    mHighlightColor = ContextCompat.getColor(context, a.getResourceId(3, 0))
    mPromotedColorResId = ContextCompat.getColor(context, R.color.greenA700)
    mHotColorResId = ContextCompat.getColor(context, R.color.orange500)
    mAccentColorResId =
        ContextCompat.getColor(
            getContext(),
            getThemedResId(getContext(), R.attr.colorAccent),
        )
    mCommentDrawable =
        DrawableCompat.wrap(
            ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_comment_white_24dp,
                )!!
                .mutate()
        )
    DrawableCompat.setTint(mCommentDrawable, mAccentColorResId)
    inflate(context, if (mIsLocal) R.layout.local_story_view else R.layout.story_view, this)
    mBackground = findViewById(R.id.background)
    mBackground.setBackgroundColor(mBackgroundColor)
    mVoteSwitcher = findViewById(R.id.vote_switcher)
    mRankTextView = findViewById(R.id.rank)
    mScoreTextView = findViewById(R.id.score)
    mBookmarked = findViewById(R.id.bookmarked)
    mPostedTextView = findViewById(R.id.posted)
    mTitleTextView = findViewById(R.id.title)
    mSourceTextView = findViewById(R.id.source)
    mCommentButton = findViewById(R.id.comment)
    mCommentButton.setCompoundDrawablesWithIntrinsicBounds(mCommentDrawable, null, null, null)
    this.moreOptions = findViewById(R.id.button_more)
    // replace with bounded ripple as unbounded ripple requires container bg
    // http://b.android.com/155880
    moreOptions.setBackgroundResource(
        getThemedResId(
            context,
            R.attr.selectableItemBackground,
        )
    )
    ta.recycle()
    a.recycle()
  }

  override fun setChecked(checked: Boolean) {
    if (mChecked == checked) {
      return
    }
    mChecked = checked
    mBackground.setBackgroundColor(if (mChecked) mHighlightColor else mBackgroundColor)
  }

  override fun isChecked(): Boolean {
    return mChecked
  }

  override fun toggle() {
    setChecked(!mChecked)
  }

  fun setStory(story: WebItem, hotThreshold: Int) {
    if (!mIsLocal && story is Item) {
      var hot = story.score >= hotThreshold * AppUtils.HOT_FACTOR
      mScoreTextView.setTextColor(if (hot) mHotColorResId else mSecondaryTextColorResId)
      mRankTextView.text = story.rank.toString()
      mScoreTextView.setCompoundDrawablesWithIntrinsicBounds(
          0,
          0,
          0,
          if (hot) R.drawable.ic_whatshot_orange500_18dp else 0,
      )
      mScoreTextView.text =
          context.resources.getQuantityString(R.plurals.score, story.score, story.score)
      if (story.kidCount > 0) {
        hot = story.kidCount >= hotThreshold
        mCommentButton.setTextColor(if (hot) mHotColorResId else mAccentColorResId)
        if (hot) {
          mCommentButton.setCompoundDrawablesWithIntrinsicBounds(
              R.drawable.ic_whatshot_orange500_24dp,
              0,
              0,
              0,
          )
        } else {
          mCommentButton.setCompoundDrawablesWithIntrinsicBounds(
              mCommentDrawable,
              null,
              null,
              null,
          )
        }
        mCommentButton.text = story.kidCount.toString()
      } else {
        mCommentButton.setTextColor(mAccentColorResId)
        mCommentButton.setText(null)
        mCommentButton.setCompoundDrawablesWithIntrinsicBounds(
            mCommentDrawable,
            null,
            null,
            null,
        )
      }
    }
    mCommentButton.visibility = VISIBLE
    mTitleTextView.text = context.getString(R.string.loading_text)
    mTitleTextView.setText(story.displayedTitle)
    mPostedTextView.text = story.getDisplayedTime(context)
    mPostedTextView.append(story.getDisplayedAuthor(context, false, 0))
    when (story.type) {
      WebItem.JOB_TYPE -> {
        mSourceTextView.setText(null)
        mSourceTextView.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_work_white_18dp,
            0,
            0,
            0,
        )
      }

      WebItem.POLL_TYPE -> {
        mSourceTextView.setText(null)
        mSourceTextView.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_poll_white_18dp,
            0,
            0,
            0,
        )
      }

      else -> {
        mSourceTextView.setText(story.source)
        mSourceTextView.setCompoundDrawables(null, null, null, null)
      }
    }
  }

  fun reset() {
    if (!mIsLocal) {
      mRankTextView.setText(R.string.loading_text)
      mScoreTextView.setText(R.string.loading_text)
      mScoreTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
      mBookmarked.visibility = INVISIBLE
    }
    mTitleTextView.text = context.getString(R.string.loading_text)
    mPostedTextView.setText(R.string.loading_text)
    mSourceTextView.setText(R.string.loading_text)
    mSourceTextView.setCompoundDrawables(null, null, null, null)
    mCommentButton.visibility = INVISIBLE
  }

  fun setViewed(isViewed: Boolean) {
    if (mIsLocal) {
      return // local always means viewed, do not decorate
    }
    mTitleTextView.setTextColor(if (isViewed) mSecondaryTextColorResId else mTertiaryTextColorResId)
  }

  fun setPromoted(change: Int) {
    val spannable = SpannableString(String.format(Locale.US, PROMOTED, change))
    spannable.setSpan(
        SuperscriptSpan(),
        0,
        spannable.length,
        Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
    )
    spannable.setSpan(
        RelativeSizeSpan(0.6f),
        0,
        spannable.length,
        Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
    )
    spannable.setSpan(
        ForegroundColorSpan(mPromotedColorResId),
        0,
        spannable.length,
        Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
    )
    mRankTextView.append(spannable)
  }

  fun setFavorite(isFavorite: Boolean) {
    if (mIsLocal) {
      return // local item must be favorite, do not decorate
    }
    mBookmarked.visibility = if (isFavorite) VISIBLE else INVISIBLE
  }

  fun setOnCommentClickListener(listener: OnClickListener?) {
    mCommentButton.setOnClickListener(listener)
  }

  fun setUpdated(story: Item, updated: Boolean, change: Int) {
    if (mIsLocal) {
      return // local items do not change
    }
    if (updated) {
      mRankTextView.append(decorateUpdated())
    } else if (change > 0) {
      setPromoted(change)
    }
    if (story.kidCount > 0 && story.hasNewKids()) {
      mCommentButton.append(decorateUpdated())
    }
  }

  fun animateVote(newScore: Int) {
    if (mIsLocal) {
      return
    }
    mVoteSwitcher.inAnimation.setAnimationListener(
        object : Animation.AnimationListener {
          override fun onAnimationStart(animation: Animation?) {
            // no op
          }

          override fun onAnimationEnd(animation: Animation?) {
            Handler()
                .postDelayed(
                    Runnable { mVoteSwitcher.showNext() },
                    VOTE_DELAY_MILLIS.toLong(),
                )
            mScoreTextView.text =
                context.resources.getQuantityString(R.plurals.score, newScore, newScore)
            mVoteSwitcher.inAnimation.setAnimationListener(null)
          }

          override fun onAnimationRepeat(animation: Animation?) {
            // no op
          }
        }
    )
    mVoteSwitcher.showNext()
  }

  private fun decorateUpdated(): Spannable {
    val sb = SpannableStringBuilder("*")
    sb.setSpan(
        AsteriskSpan(context),
        sb.length - 1,
        sb.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
    return sb
  }

  companion object {
    private const val VOTE_DELAY_MILLIS = 500
    private const val PROMOTED = "+%1\$d"
  }
}
