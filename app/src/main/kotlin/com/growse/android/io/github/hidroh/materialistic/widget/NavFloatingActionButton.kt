/*
 * Copyright (c) 2016 Ha Duy Trung
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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Vibrator
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GestureDetectorCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.growse.android.io.github.hidroh.materialistic.AppUtils.openPlayStore
import com.growse.android.io.github.hidroh.materialistic.Navigable
import com.growse.android.io.github.hidroh.materialistic.Preferences
import com.growse.android.io.github.hidroh.materialistic.Preferences.navigationVibrationEnabled
import com.growse.android.io.github.hidroh.materialistic.R
import java.util.Locale
import kotlin.math.abs

class NavFloatingActionButton
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FloatingActionButton(context, attrs, defStyleAttr), OnGlobalLayoutListener {
  val mVibrator: Vibrator?
  private val mPreferenceObservable = Preferences.Observable()
  var mNavigable: Navigable? = null
  var mMoved: Boolean = false
  private var mNextKonamiCode = 0
  private var mPreferences: SharedPreferences? = null
  private var mPreferenceX: String? = null
  private var mPreferenceY: String? = null
  var mVibrationEnabled: Boolean

  init {
    bindNavigationPad()
    mVibrationEnabled = navigationVibrationEnabled(context)
    if (!isInEditMode) {
      mVibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
    } else {
      mVibrator = null
    }
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    viewTreeObserver.addOnGlobalLayoutListener(this)
    mPreferenceObservable.subscribe(
        context,
        Preferences.Observer { key: Int, contextChanged: Boolean ->
          mVibrationEnabled = navigationVibrationEnabled(context)
        },
        R.string.pref_navigation_vibrate,
    )
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    stopObservingViewTree()
    mPreferenceObservable.unsubscribe(context)
  }

  override fun setOnTouchListener(l: OnTouchListener?) {
    throw UnsupportedOperationException()
  }

  fun setNavigable(navigable: Navigable?) {
    mNavigable = navigable
  }

  @SuppressLint("ClickableViewAccessibility")
  fun bindNavigationPad() {
    val detectorCompat =
        GestureDetectorCompat(
            context,
            object : SimpleOnGestureListener() {
              override fun onDown(e: MotionEvent): Boolean {
                return mNavigable != null
              }

              override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                Toast.makeText(
                        context,
                        R.string.hint_nav_short,
                        Toast.LENGTH_LONG,
                    )
                    .show()
                return true
              }

              override fun onDoubleTap(e: MotionEvent): Boolean {
                trackKonami(DOUBLE_TAP)
                return super.onDoubleTap(e)
              }

              override fun onFling(
                  motionEvent: MotionEvent?,
                  motionEvent1: MotionEvent,
                  velocityX: Float,
                  velocityY: Float,
              ): Boolean {
                val direction: Int =
                    if (abs(velocityX) > abs(velocityY)) {
                      if (velocityX < 0) Navigable.DIRECTION_LEFT else Navigable.DIRECTION_RIGHT
                    } else {
                      if (velocityY < 0) Navigable.DIRECTION_UP else Navigable.DIRECTION_DOWN
                    }
                mNavigable!!.onNavigate(direction)
                if (mVibrationEnabled) {
                  mVibrator!!.vibrate(VIBRATE_DURATION_MS)
                }
                trackKonami(direction)
                return false
              }

              override fun onLongPress(e: MotionEvent) {
                if (mNavigable == null) {
                  return
                }
                startDrag(e.x, e.y)
              }
            },
        )
    super.setOnTouchListener(
        object : OnTouchListener {
          override fun onTouch(view: View?, motionEvent: MotionEvent): Boolean {
            return detectorCompat.onTouchEvent(motionEvent)
          }
        }
    )
  }

  @SuppressLint("ClickableViewAccessibility")
  fun startDrag(startX: Float, startY: Float) {
    if (mVibrationEnabled) {
      mVibrator!!.vibrate(VIBRATE_DURATION_MS * 2)
    }
    Toast.makeText(context, R.string.hint_drag, Toast.LENGTH_SHORT).show()
    super.setOnTouchListener(
        object : OnTouchListener {
          override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            when (motionEvent.action) {
              MotionEvent.ACTION_MOVE -> {
                mMoved = true
                view.x = motionEvent.rawX - startX // TODO compensate shift
                view.y = motionEvent.rawY - startY
              }

              MotionEvent.ACTION_CANCEL,
              MotionEvent.ACTION_UP -> {
                bindNavigationPad()
                if (mMoved) {
                  persistPosition()
                } else {
                  view.performClick()
                }
              }

              else -> return false
            }
            return true
          }
        }
    )
  }

  fun trackKonami(direction: Int): Boolean {
    if (KONAMI_CODE[mNextKonamiCode] != direction) {
      mNextKonamiCode = if (direction == KONAMI_CODE[0]) 1 else 0
      return false
    } else if (mNextKonamiCode == KONAMI_CODE.size - 1) {
      mNextKonamiCode = 0
      if (mVibrationEnabled) {
        mVibrator!!.vibrate(
            longArrayOf(
                0,
                VIBRATE_DURATION_MS * 2,
                100,
                VIBRATE_DURATION_MS * 2,
            ),
            -1,
        )
      }
      AlertDialog.Builder(context)
          .setView(R.layout.dialog_konami)
          .setPositiveButton(
              android.R.string.ok,
              DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i: Int ->
                openPlayStore(context)
              },
          )
          .create()
          .show()
      return true
    } else {
      mNextKonamiCode++
      return true
    }
  }

  override fun onGlobalLayout() {
    restorePosition()
    stopObservingViewTree()
  }

  private fun stopObservingViewTree() {
    viewTreeObserver.removeOnGlobalLayoutListener(this)
  }

  @SuppressLint("CommitPrefEdits")
  fun persistPosition() {
    this.preferences!!.edit().putFloat(mPreferenceX, x).putFloat(mPreferenceY, y).apply()
  }

  private fun restorePosition() {
    x = this.preferences!!.getFloat(mPreferenceX, x)
    y = this.preferences!!.getFloat(mPreferenceY, y)
  }

  private val displayMetrics: DisplayMetrics
    get() {
      val metrics = DisplayMetrics()
      (context.getSystemService(Activity.WINDOW_SERVICE) as WindowManager)
          .defaultDisplay
          .getMetrics(metrics)
      return metrics
    }

  private val preferences: SharedPreferences?
    get() {
      if (mPreferences == null) {
        mPreferences = getSharedPreferences(context)
        val metrics = this.displayMetrics
        mPreferenceX =
            String.format(
                Locale.US,
                PREFERENCES_FAB_X,
                context.javaClass.name,
                metrics.widthPixels,
                metrics.heightPixels,
            )
        mPreferenceY =
            String.format(
                Locale.US,
                PREFERENCES_FAB_Y,
                context.javaClass.name,
                metrics.widthPixels,
                metrics.heightPixels,
            )
      }
      return mPreferences
    }

  companion object {
    private const val PREFERENCES_FAB = "_fab"
    private const val PREFERENCES_FAB_X = "%1\$s_%2\$d_%3\$d_x"
    private const val PREFERENCES_FAB_Y = "%1\$s_%2\$d_%3\$d_y"
    private const val VIBRATE_DURATION_MS: Long = 15
    private val DOUBLE_TAP = -1
    private val KONAMI_CODE =
        intArrayOf(
            Navigable.DIRECTION_UP,
            Navigable.DIRECTION_UP,
            Navigable.DIRECTION_DOWN,
            Navigable.DIRECTION_DOWN,
            Navigable.DIRECTION_LEFT,
            Navigable.DIRECTION_RIGHT,
            Navigable.DIRECTION_LEFT,
            Navigable.DIRECTION_RIGHT,
            DOUBLE_TAP,
        )

    fun resetPosition(context: Context) {
      getSharedPreferences(context)!!.edit().clear().apply()
    }

    private fun getSharedPreferences(context: Context): SharedPreferences? {
      return context.getSharedPreferences(
          context.packageName + PREFERENCES_FAB,
          Context.MODE_PRIVATE,
      )
    }
  }
}
