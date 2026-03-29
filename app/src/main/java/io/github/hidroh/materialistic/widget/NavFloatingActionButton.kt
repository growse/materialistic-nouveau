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

package io.github.hidroh.materialistic.widget

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Vibrator
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.Toast

import com.google.android.material.floatingactionbutton.FloatingActionButton

import java.util.Locale

import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GestureDetectorCompat
import io.github.hidroh.materialistic.AppUtils
import io.github.hidroh.materialistic.Navigable
import io.github.hidroh.materialistic.Preferences
import io.github.hidroh.materialistic.R
import io.github.hidroh.materialistic.annotation.Synthetic

open class NavFloatingActionButton : FloatingActionButton(), ViewTreeObserver.OnGlobalLayoutListener {
    private const val PREFERENCES_FAB: String = "_fab"
    private const val PREFERENCES_FAB_X: String = "%1$s_%2$d_%3$d_x"
    private const val PREFERENCES_FAB_Y: String = "%1$s_%2$d_%3$d_y"
    private const val VIBRATE_DURATION_MS: Long = 15
    private const val DOUBLE_TAP: Int = -1
    private static final int[] KONAMI_CODE = {
            Navigable.DIRECTION_UP,
            Navigable.DIRECTION_UP,
            Navigable.DIRECTION_DOWN,
            Navigable.DIRECTION_DOWN,
            Navigable.DIRECTION_LEFT,
            Navigable.DIRECTION_RIGHT,
            Navigable.DIRECTION_LEFT,
            Navigable.DIRECTION_RIGHT,
            DOUBLE_TAP
    }
    var mVibrator: Vibrator? = null
    private val mPreferenceObservable: Preferences.Observable = new Preferences.Observable()
    var mNavigable: Navigable? = null
    var mMoved: Boolean = false
    private var mNextKonamiCode: Int = 0
    private var mPreferences: SharedPreferences? = null
    private var mPreferenceY: String mPreferenceX,? = null
    var mVibrationEnabled: Boolean = false

    fun resetPosition(context: Context) {
        getSharedPreferences(context).edit().clear().apply()
    }

    constructor(context: Context) {
        this(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) {
        this(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) {
        super(context, attrs, defStyleAttr)
        bindNavigationPad()
        mVibrationEnabled = Preferences.navigationVibrationEnabled(context)
        if (!isInEditMode()) {
            mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)
        } else {
            mVibrator = null
        }
    }

    protected override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        getViewTreeObserver().addOnGlobalLayoutListener(this)
        mPreferenceObservable.subscribe(getContext(), (key, contextChanged) ->
                mVibrationEnabled = Preferences.navigationVibrationEnabled(getContext()),
                R.string.pref_navigation_vibrate)
    }

    protected override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopObservingViewTree()
        mPreferenceObservable.unsubscribe(getContext())
    }

    override fun setOnTouchListener(l: OnTouchListener) {
        throw UnsupportedOperationException()
    }

    fun setNavigable(navigable: Navigable) {
        mNavigable = navigable
    }

    @Synthetic
    fun bindNavigationPad() {
        GestureDetectorCompat detectorCompat = GestureDetectorCompat(getContext(),
                GestureDetector.SimpleOnGestureListener() {
                    public boolean onDown(MotionEvent e) {
                        return mNavigable != null
                    }

                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        Toast.makeText(getContext(), R.string.hint_nav_short,
                                Toast.LENGTH_LONG).show()
                        return true
                    }

                    public boolean onDoubleTap(MotionEvent e) {
                        trackKonami(DOUBLE_TAP)
                        return super.onDoubleTap(e)
                    }

                    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1,
                                           float velocityX, float velocityY) {
                        int direction
                        if (Math.abs(velocityX) > Math.abs(velocityY)) {
                            direction = velocityX < 0 ?
                                    Navigable.DIRECTION_LEFT : Navigable.DIRECTION_RIGHT
                        } else {
                            direction = velocityY < 0 ?
                                    Navigable.DIRECTION_UP : Navigable.DIRECTION_DOWN
                        }
                        mNavigable.onNavigate(direction)
                        if (mVibrationEnabled) {
                            mVibrator.vibrate(VIBRATE_DURATION_MS)
                        }
                        trackKonami(direction)
                        return false
                    }

                    public void onLongPress(MotionEvent e) {
                        if (mNavigable == null) {
                            return
                        }
                        startDrag(e.getX(), e.getY())
                    }
                })
        //noinspection Convert2Lambda
        super.setOnTouchListener(OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return detectorCompat.onTouchEvent(motionEvent)
            }
        })
    }

    @Synthetic
    fun startDrag(startX: Float, startY: Float) {
        if (mVibrationEnabled) {
            mVibrator.vibrate(VIBRATE_DURATION_MS * 2)
        }
        Toast.makeText(getContext(), R.string.hint_drag, Toast.LENGTH_SHORT).show()
        //noinspection Convert2Lambda
        super.setOnTouchListener(OnTouchListener() {
            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        mMoved = true
                        view.setX(motionEvent.getRawX() - startX); // TODO compensate shift
                        view.setY(motionEvent.getRawY() - startY)
                        break
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        bindNavigationPad()
                        if (mMoved) {
                            persistPosition()
                        }
                        break
                    default:
                        return false
                }
                return true
            }
        })
    }

    @Synthetic
    fun trackKonami(direction: Int): Boolean {
        if (KONAMI_CODE[mNextKonamiCode] != direction) {
            mNextKonamiCode = direction == KONAMI_CODE[0] ? 1 : 0
            return false
        } else if (mNextKonamiCode == KONAMI_CODE.length - 1) {
            mNextKonamiCode = 0
            if (mVibrationEnabled) {
                mVibrator.vibrate(new long[]{0, VIBRATE_DURATION_MS * 2,
                        100, VIBRATE_DURATION_MS * 2}, -1)
            }
            AlertDialog.Builder(getContext())
                    .setView(R.layout.dialog_konami)
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) ->
                            AppUtils.openPlayStore(getContext()))
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
        getViewTreeObserver().removeOnGlobalLayoutListener(this)
    }

    @SuppressLint("CommitPrefEdits")
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    fun persistPosition() {
        getPreferences()
                .edit()
                .putFloat(mPreferenceX, getX())
                .putFloat(mPreferenceY, getY())
                .apply()
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    private fun restorePosition() {
        setX(getPreferences().getFloat(mPreferenceX, getX()))
        setY(getPreferences().getFloat(mPreferenceY, getY()))
    }

    private fun getDisplayMetrics(): DisplayMetrics {
        DisplayMetrics metrics = DisplayMetrics()
        ((WindowManager) getContext().getSystemService(Activity.WINDOW_SERVICE))
                .getDefaultDisplay().getMetrics(metrics)
        return metrics
    }

    private fun getPreferences(): SharedPreferences {
        if (mPreferences == null) {
            mPreferences = getSharedPreferences(getContext())
            DisplayMetrics metrics = getDisplayMetrics()
            mPreferenceX = String.format(Locale.US, PREFERENCES_FAB_X,
                    getContext().getClass().getName(), metrics.widthPixels, metrics.heightPixels)
            mPreferenceY = String.format(Locale.US, PREFERENCES_FAB_Y,
                    getContext().getClass().getName(), metrics.widthPixels, metrics.heightPixels)
        }
        return mPreferences
    }

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(context.getPackageName() + PREFERENCES_FAB,
                Context.MODE_PRIVATE)
    }
}
