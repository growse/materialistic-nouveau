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
package com.growse.android.io.github.hidroh.materialistic

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.text.TextUtils
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.growse.android.io.github.hidroh.materialistic.annotation.PublicApi
import com.growse.android.io.github.hidroh.materialistic.annotation.Synthetic
import com.growse.android.io.github.hidroh.materialistic.data.AlgoliaPopularClient
import com.growse.android.io.github.hidroh.materialistic.preference.ThemePreference
import com.growse.android.io.github.hidroh.materialistic.preference.ThemePreference.DayNightSpec
import com.growse.android.io.github.hidroh.materialistic.preference.ThemePreference.ThemeSpec
import java.util.Locale

@PublicApi
object Preferences {
  private const val DRAFT_PREFIX = "draft_%1\$s"
  private const val PREFERENCES_DRAFT = "_drafts"

  @VisibleForTesting var sReleaseNotesSeen: Boolean? = null

  private val PREF_MIGRATION: List<BoolToStringPref?> =
      listOf<BoolToStringPref>(
          BoolToStringPref(
              R.string.pref_item_click,
              false,
              R.string.pref_story_display,
              R.string.pref_story_display_value_comments,
          ),
          BoolToStringPref(
              R.string.pref_item_search_recent,
              true,
              R.string.pref_search_sort,
              R.string.pref_search_sort_value_default,
          ),
      )

  @JvmStatic
  fun sync(preferenceManager: PreferenceManager) {
    val map = preferenceManager.getSharedPreferences()!!.getAll()
    for (key in map.keys) {
      sync(preferenceManager, key)
    }
  }

  private fun sync(preferenceManager: PreferenceManager, key: String) {
    val pref = preferenceManager.findPreference<Preference?>(key)
    if (pref is ListPreference) {
      val listPref = pref
      pref.setSummary(listPref.getEntry())
    }
  }

  /**
   * Migrate from boolean preferences to string preferences. Should be called only once when
   * application is relaunched. If boolean preference has been set before, and value is not default,
   * migrate to the new corresponding string value If boolean preference has been set before, but
   * value is default, simply remove it
   *
   * @param context application context TODO remove once all users migrated
   */
  @JvmStatic
  fun migrate(context: Context) {
    val sp = PreferenceManager.getDefaultSharedPreferences(context)
    sp.edit {
      for (pref in PREF_MIGRATION) {
        if (pref!!.isChanged(context, sp)) {
          putString(context.getString(pref.newKey), context.getString(pref.newValue))
        }

        if (pref.hasOldValue(context, sp)) {
          remove(context.getString(pref.oldKey))
        }
      }
    }
  }

  @JvmStatic
  fun isListItemCardView(context: Context): Boolean {
    return get(context, R.string.pref_list_item_view, false)
  }

  @JvmStatic
  fun isSortByRecent(context: Context): Boolean {
    return (get(context, R.string.pref_search_sort, R.string.pref_search_sort_value_recent) ==
        context.getString(R.string.pref_search_sort_value_recent))
  }

  @JvmStatic
  fun setSortByRecent(context: Context, byRecent: Boolean) {
    set(
        context,
        R.string.pref_search_sort,
        context.getString(
            if (byRecent) R.string.pref_search_sort_value_recent
            else R.string.pref_search_sort_value_default
        ),
    )
  }

  @JvmStatic
  fun getDefaultStoryView(context: Context): StoryViewMode {
    val pref = get(context, R.string.pref_story_display, R.string.pref_story_display_value_article)
    if (TextUtils.equals(context.getString(R.string.pref_story_display_value_comments), pref)) {
      return StoryViewMode.Comment
    }
    if (TextUtils.equals(context.getString(R.string.pref_story_display_value_readability), pref)) {
      return StoryViewMode.Readability
    }
    return StoryViewMode.Article
  }

  @JvmStatic
  fun externalBrowserEnabled(context: Context): Boolean {
    return get(context, R.string.pref_external, false)
  }

  @JvmStatic
  fun colorCodeEnabled(context: Context): Boolean {
    return get(context, R.string.pref_color_code, true)
  }

  @JvmStatic
  fun colorCodeOpacity(context: Context): Int {
    return getInt(context, R.string.pref_color_code_opacity, 100)
  }

  @JvmStatic
  fun smoothScrollEnabled(context: Context): Boolean {
    return get(context, R.string.pref_smooth_scroll, true)
  }

  @JvmStatic
  fun threadIndicatorEnabled(context: Context): Boolean {
    return get(context, R.string.pref_thread_indicator, true)
  }

  fun highlightUpdatedEnabled(context: Context): Boolean {
    return get(context, R.string.pref_highlight_updated, true)
  }

  fun autoMarkAsViewed(context: Context): Boolean {
    return get(context, R.string.pref_auto_viewed, false)
  }

  @JvmStatic
  fun navigationEnabled(context: Context): Boolean {
    return get(context, R.string.pref_navigation, false)
  }

  @JvmStatic
  fun navigationVibrationEnabled(context: Context): Boolean {
    return get(context, R.string.pref_navigation_vibrate, true)
  }

  @JvmStatic
  fun customTabsEnabled(context: Context): Boolean {
    return get(context, R.string.pref_custom_tab, true)
  }

  @JvmStatic
  fun isSinglePage(context: Context, displayOption: String?): Boolean {
    return !TextUtils.equals(
        displayOption,
        context.getString(R.string.pref_comment_display_value_multiple),
    )
  }

  @JvmStatic
  fun isAutoExpand(context: Context, displayOption: String?): Boolean {
    return TextUtils.equals(
        displayOption,
        context.getString(R.string.pref_comment_display_value_single),
    )
  }

  @JvmStatic
  fun getCommentDisplayOption(context: Context): String {
    return get(context, R.string.pref_comment_display, R.string.pref_comment_display_value_single)
  }

  @JvmStatic
  fun setPopularRange(context: Context, @AlgoliaPopularClient.Range range: String) {
    set(context, R.string.pref_popular_range, range)
  }

  @JvmStatic
  fun getPopularRange(context: Context): String {
    return get(context, R.string.pref_popular_range, AlgoliaPopularClient.LAST_24H)
  }

  @JvmStatic
  fun getCommentMaxLines(context: Context): Int {
    val maxLinesString = get(context, R.string.pref_max_lines, null)
    var maxLines = maxLinesString.toInt()
    if (maxLines < 0) {
      maxLines = Int.MAX_VALUE
    }
    return maxLines
  }

  @JvmStatic
  fun getLineHeight(context: Context): Float {
    return getFloatFromString(context, R.string.pref_line_height, 1.0f)
  }

  @JvmStatic
  fun getReadabilityLineHeight(context: Context): Float {
    return getFloatFromString(context, R.string.pref_readability_line_height, 1.0f)
  }

  @JvmStatic
  fun shouldLazyLoad(context: Context): Boolean {
    return get(context, R.string.pref_lazy_load, true)
  }

  @JvmStatic
  fun getUsername(context: Context): String {
    return get(context, R.string.pref_username, null)
  }

  @JvmStatic
  fun setUsername(context: Context, username: String?) {
    set(context, R.string.pref_username, username)
  }

  @JvmStatic
  fun getLaunchScreen(context: Context): String {
    return get(context, R.string.pref_launch_screen, R.string.pref_launch_screen_value_top)
  }

  @JvmStatic
  fun isLaunchScreenLast(context: Context): Boolean {
    return TextUtils.equals(
        context.getString(R.string.pref_launch_screen_value_last),
        getLaunchScreen(context),
    )
  }

  @JvmStatic
  fun adBlockEnabled(context: Context): Boolean {
    return get(context, R.string.pref_ad_block, true)
  }

  @JvmStatic
  fun saveDraft(context: Context, parentId: String, draft: String?) {
    context
        .getSharedPreferences(context.getPackageName() + PREFERENCES_DRAFT, Context.MODE_PRIVATE)
        .edit { putString(String.format(Locale.US, DRAFT_PREFIX, parentId), draft) }
  }

  @JvmStatic
  fun getDraft(context: Context, parentId: String): String? {
    return context
        .getSharedPreferences(context.getPackageName() + PREFERENCES_DRAFT, Context.MODE_PRIVATE)
        .getString(String.format(Locale.US, DRAFT_PREFIX, parentId), null)
  }

  @JvmStatic
  fun deleteDraft(context: Context, parentId: String) {
    context
        .getSharedPreferences(context.getPackageName() + PREFERENCES_DRAFT, Context.MODE_PRIVATE)
        .edit { remove(String.format(Locale.US, DRAFT_PREFIX, parentId)) }
  }

  @JvmStatic
  fun clearDrafts(context: Context) {
    context
        .getSharedPreferences(context.getPackageName() + PREFERENCES_DRAFT, Context.MODE_PRIVATE)
        .edit { clear() }
  }

  @JvmStatic
  fun isReleaseNotesSeen(context: Context): Boolean {
    if (sReleaseNotesSeen == null) {
      var info: PackageInfo? = null
      try {
        info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0)
      } catch (_: PackageManager.NameNotFoundException) {
        // no op
      }
      // considered seen if first time install or last seen release is up to date
      if (info != null && info.firstInstallTime == info.lastUpdateTime) {
        setReleaseNotesSeen(context)
      } else {
        sReleaseNotesSeen =
            getInt(context, R.string.pref_latest_release, 0) >= BuildConfig.LATEST_RELEASE
      }
    }
    return sReleaseNotesSeen!!
  }

  @JvmStatic
  fun setReleaseNotesSeen(context: Context) {
    sReleaseNotesSeen = true
    setInt(context, R.string.pref_latest_release, BuildConfig.LATEST_RELEASE)
  }

  @JvmStatic
  fun multiWindowEnabled(context: Context): Boolean {
    return !TextUtils.equals(
        context.getString(R.string.pref_multi_window_value_none),
        get(context, R.string.pref_multi_window, R.string.pref_multi_window_value_none),
    )
  }

  fun getListSwipePreferences(context: Context): List<SwipeAction> {
    val left = get(context, R.string.pref_list_swipe_left, R.string.swipe_save)
    val right = get(context, R.string.pref_list_swipe_right, R.string.swipe_vote)
    return listOf(parseSwipeAction(left), parseSwipeAction(right))
  }

  @JvmStatic
  fun reset(context: Context) {
    PreferenceManager.getDefaultSharedPreferences(context).edit { clear() }
  }

  private fun parseSwipeAction(value: String?): SwipeAction {
    return try {
      SwipeAction.valueOf(value!!)
    } catch (_: IllegalArgumentException) {
      SwipeAction.None
    } catch (_: NullPointerException) {
      SwipeAction.None
    }
  }

  @Synthetic
  fun get(context: Context, @StringRes key: Int, defaultValue: Boolean): Boolean {
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getBoolean(context.getString(key), defaultValue)
  }

  private fun getInt(context: Context, @StringRes key: Int, defaultValue: Int): Int {
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getInt(context.getString(key), defaultValue)
  }

  private fun getFloatFromString(
      context: Context,
      @StringRes key: Int,
      defaultValue: Float,
  ): Float {
    val floatValue = get(context, key, null)
    try {
      return floatValue.toFloat()
    } catch (_: NumberFormatException) {
      return defaultValue
    } catch (_: NullPointerException) {
      return defaultValue
    }
  }

  @Synthetic
  fun get(context: Context, @StringRes key: Int, defaultValue: String?): String {
    return get(context, context.getString(key), defaultValue)
  }

  private fun get(context: Context, @StringRes key: Int, @StringRes defaultValue: Int): String {
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getString(context.getString(key), context.getString(defaultValue))!!
  }

  private fun get(context: Context, key: String?, defaultValue: String?): String {
    return PreferenceManager.getDefaultSharedPreferences(context).getString(key, defaultValue)!!
  }

  private fun setInt(context: Context, @StringRes key: Int, value: Int) {
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putInt(context.getString(key), value)
    }
  }

  private fun set(context: Context, @StringRes key: Int, value: String?) {
    set(context, context.getString(key), value)
  }

  private fun set(context: Context, key: String?, value: String?) {
    PreferenceManager.getDefaultSharedPreferences(context).edit { putString(key, value) }
  }

  @Synthetic
  fun set(context: Context, @StringRes key: Int, value: Boolean) {
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(context.getString(key), value)
    }
  }

  enum class SwipeAction {
    None,
    Vote,
    Save,
    Refresh,
    Share,
  }

  enum class StoryViewMode {
    Comment,
    Article,
    Readability,
  }

  class BoolToStringPref
  @Synthetic
  internal constructor(
      @field:Synthetic @param:StringRes val oldKey: Int,
      private val oldDefault: Boolean,
      @field:Synthetic @param:StringRes val newKey: Int,
      @field:Synthetic @param:StringRes val newValue: Int,
  ) {
    @Synthetic
    fun isChanged(context: Context, sp: SharedPreferences): Boolean {
      return hasOldValue(context, sp) &&
          sp.getBoolean(context.getString(oldKey), oldDefault) != oldDefault
    }

    @Synthetic
    fun hasOldValue(context: Context, sp: SharedPreferences): Boolean {
      return sp.contains(context.getString(oldKey))
    }
  }

  @PublicApi
  object Theme {
    @JvmStatic
    fun apply(context: Context, dialogTheme: Boolean, isTranslucent: Boolean) {
      val themeSpec = getTheme(context, isTranslucent)
      context.setTheme(themeSpec.theme)
      if (themeSpec.themeOverrides >= 0) {
        context.getTheme().applyStyle(themeSpec.themeOverrides, true)
      }
      if (dialogTheme) {
        context.setTheme(AppUtils.getThemedResId(context, R.attr.alertDialogTheme))
      }
    }

    @JvmStatic
    fun getTypeface(context: Context): String {
      return get(context, R.string.pref_font, null)
    }

    @JvmStatic
    fun getReadabilityTypeface(context: Context): String {
      val typefaceName = get(context, R.string.pref_readability_font, null)
      if (TextUtils.isEmpty(typefaceName)) {
        return getTypeface(context)
      }
      return typefaceName
    }

    @JvmStatic
    @StyleRes
    fun resolveTextSize(choice: String): Int {
      when (choice.toInt()) {
        -1 -> return R.style.AppTextSize_XSmall
        0 -> return R.style.AppTextSize
        1 -> return R.style.AppTextSize_Medium
        2 -> return R.style.AppTextSize_Large
        3 -> return R.style.AppTextSize_XLarge
        else -> return R.style.AppTextSize
      }
    }

    @JvmStatic
    @StyleRes
    fun resolvePreferredTextSize(context: Context): Int {
      return resolveTextSize(getPreferredTextSize(context))
    }

    @JvmStatic
    @StyleRes
    fun resolvePreferredReadabilityTextSize(context: Context): Int {
      return resolveTextSize(getPreferredReadabilityTextSize(context))
    }

    @JvmStatic
    fun getAutoDayNightMode(context: Context): Int {
      return if (
          getTheme(context, false) is DayNightSpec &&
              get(context, R.string.pref_daynight_auto, false)
      )
          AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
      else AppCompatDelegate.MODE_NIGHT_NO
    }

    @JvmStatic
    fun disableAutoDayNight(context: Context) {
      set(context, R.string.pref_daynight_auto, false)
    }

    private fun getPreferredReadabilityTextSize(context: Context): String {
      val choice = get(context, R.string.pref_readability_text_size, null)
      if (TextUtils.isEmpty(choice)) {
        return getPreferredTextSize(context)
      }
      return choice
    }

    private fun getPreferredTextSize(context: Context): String {
      return get(context, R.string.pref_text_size, 0.toString())
    }

    private fun getTheme(context: Context, isTransulcent: Boolean): ThemeSpec {
      return ThemePreference.getTheme(get(context, R.string.pref_theme, null), isTransulcent)
    }
  }

  @PublicApi
  object Offline {
    @JvmStatic
    fun isEnabled(context: Context): Boolean {
      return get(context, R.string.pref_saved_item_sync, false)
    }

    @JvmStatic
    fun isCommentsEnabled(context: Context): Boolean {
      return isEnabled(context) && get(context, R.string.pref_offline_comments, true)
    }

    @JvmStatic
    fun isArticleEnabled(context: Context): Boolean {
      return isEnabled(context) && get(context, R.string.pref_offline_article, true)
    }

    @JvmStatic
    fun isReadabilityEnabled(context: Context): Boolean {
      return isEnabled(context) && get(context, R.string.pref_offline_readability, true)
    }

    @JvmStatic
    fun currentConnectionEnabled(context: Context): Boolean {
      return !isWifiOnly(context) || AppUtils.isOnWiFi(context)
    }

    @JvmStatic
    fun isNotificationEnabled(context: Context): Boolean {
      return get(context, R.string.pref_offline_notification, false)
    }

    @JvmStatic
    fun isWifiOnly(context: Context): Boolean {
      val wifiValue = context.getString(R.string.offline_data_wifi)
      return TextUtils.equals(wifiValue, get(context, R.string.pref_offline_data, wifiValue))
    }
  }

  class Observable {
    private val mSubscribedKeys: MutableMap<String?, Int?> = HashMap()
    private val mListener =
        OnSharedPreferenceChangeListener { _: SharedPreferences?, key: String? ->
          if (mSubscribedKeys.containsKey(key)) {
            notifyChanged(mSubscribedKeys.get(key)!!, CONTEXT_KEYS!!.contains(key))
          }
        }
    private var mObserver: Observer? = null

    fun subscribe(context: Context, observer: Observer, vararg preferenceKeys: Int) {
      ensureContextKeys(context)
      setSubscription(context, preferenceKeys)
      mObserver = observer
      PreferenceManager.getDefaultSharedPreferences(context)
          .registerOnSharedPreferenceChangeListener(mListener)
    }

    fun unsubscribe(context: Context) {
      PreferenceManager.getDefaultSharedPreferences(context)
          .unregisterOnSharedPreferenceChangeListener(mListener)
    }

    private fun setSubscription(context: Context, preferenceKeys: IntArray) {
      mSubscribedKeys.clear()
      for (key in preferenceKeys) {
        mSubscribedKeys.put(context.getString(key), key)
      }
    }

    private fun notifyChanged(key: Int, contextChanged: Boolean) {
      if (mObserver != null) {
        mObserver!!.onPreferenceChanged(key, contextChanged)
      }
    }

    @SuppressLint("UseSparseArrays")
    private fun ensureContextKeys(context: Context) {
      if (CONTEXT_KEYS != null) {
        return
      }
      CONTEXT_KEYS = HashSet<String?>()
      CONTEXT_KEYS!!.add(context.getString(R.string.pref_theme))
      CONTEXT_KEYS!!.add(context.getString(R.string.pref_text_size))
      CONTEXT_KEYS!!.add(context.getString(R.string.pref_font))
      CONTEXT_KEYS!!.add(context.getString(R.string.pref_daynight_auto))
    }

    companion object {
      private var CONTEXT_KEYS: MutableSet<String?>? = null
    }
  }

  fun interface Observer {
    fun onPreferenceChanged(@StringRes key: Int, contextChanged: Boolean)
  }
}
