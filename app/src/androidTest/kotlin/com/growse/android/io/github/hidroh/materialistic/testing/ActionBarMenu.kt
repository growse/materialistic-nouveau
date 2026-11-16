package com.growse.android.io.github.hidroh.materialistic.testing

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry

/** Drives action bar menus, whose items sit in the bar or its overflow depending on width. */
object ActionBarMenu {
  /** Picks [option] from the submenu [menuId], titled [menuTitle], wherever the bar put it. */
  fun choose(menuId: Int, menuTitle: String, option: String) {
    try {
      onView(withId(menuId)).perform(click())
    } catch (_: NoMatchingViewException) {
      openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
      onView(withText(menuTitle)).inRoot(isPlatformPopup()).perform(click())
    }
    onView(withText(option)).inRoot(isPlatformPopup()).perform(click())
  }
}
