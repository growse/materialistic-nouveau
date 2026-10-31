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

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog

/**
 * Injectable alert dialog builder, allowing swapping between [AlertDialog.Builder] and
 * [android.app.AlertDialog.Builder]
 *
 * @param <T> type of created alert dialog, extends from [Dialog] </T>
 */
interface AlertDialogBuilder<T : Dialog> {
  /**
   * Construct the wrapped dialog builder object. This must be called before any other methods.
   *
   * @param context activity context
   * @return This Builder object to allow for chaining of calls to set methods
   */
  fun init(context: Context): AlertDialogBuilder<*>

  /**
   * Set the title using the given resource id.
   *
   * @return This Builder object to allow for chaining of calls to set methods
   */
  fun setTitle(titleId: Int): AlertDialogBuilder<*>

  /**
   * Set the message to display using the given resource id.
   *
   * @return This Builder object to allow for chaining of calls to set methods
   */
  fun setMessage(@StringRes messageId: Int): AlertDialogBuilder<*>

  /**
   * Sets a custom view to be the contents of the alert dialog.
   *
   * When using a pre-Holo theme, if the supplied view is an instance of a [ListView] then the light
   * background will be used.
   *
   * **Note:** To ensure consistent styling, the custom view should be inflated or constructed using
   * the alert dialog's themed context
   *
   * @param view the view to use as the contents of the alert dialog
   * @return this Builder object to allow for chaining of calls to set methods
   */
  fun setView(view: View?): AlertDialogBuilder<*>

  /**
   * Set a list of items to be displayed in the dialog as the content, you will be notified of the
   * selected item via the supplied listener. The list will have a check mark displayed to the right
   * of the text for the checked item. Clicking on an item in the list will not dismiss the dialog.
   * Clicking on a button will dismiss the dialog.
   *
   * @param items the items to be displayed.
   * @param checkedItem specifies which item is checked. If -1 no items are checked.
   * @param listener notified when an item on the list is clicked. The dialog will not be dismissed
   *   when an item is clicked. It will only be dismissed if clicked on a button, if no buttons are
   *   supplied it's up to the user to dismiss the dialog.
   * @return This Builder object to allow for chaining of calls to set methods
   */
  fun setSingleChoiceItems(
      items: Array<CharSequence?>?,
      checkedItem: Int,
      listener: DialogInterface.OnClickListener?,
  ): AlertDialogBuilder<*>

  /**
   * Set a listener to be invoked when the negative button of the dialog is pressed.
   *
   * @param textId The resource id of the text to display in the negative button
   * @param listener The [DialogInterface.OnClickListener] to use.
   * @return This Builder object to allow for chaining of calls to set methods
   */
  fun setNegativeButton(
      @StringRes textId: Int,
      listener: DialogInterface.OnClickListener?,
  ): AlertDialogBuilder<*>

  /**
   * Set a listener to be invoked when the positive button of the dialog is pressed.
   *
   * @param textId The resource id of the text to display in the positive button
   * @param listener The [DialogInterface.OnClickListener] to use.
   * @return This Builder object to allow for chaining of calls to set methods
   */
  fun setPositiveButton(
      @StringRes textId: Int,
      listener: DialogInterface.OnClickListener?,
  ): AlertDialogBuilder<*>

  /**
   * Set a listener to be invoked when the neutral button of the dialog is pressed.
   *
   * @param textId The resource id of the text to display in the neutral button
   * @param listener The [DialogInterface.OnClickListener] to use.
   * @return This Builder object to allow for chaining of calls to set methods
   */
  fun setNeutralButton(
      @StringRes textId: Int,
      listener: DialogInterface.OnClickListener?,
  ): AlertDialogBuilder<*>

  /**
   * Creates a [Dialog] with the arguments supplied to this builder. It does not [Dialog.show] the
   * dialog. This allows the user to do any extra processing before displaying the dialog. Use
   * [.show] if you don't have any other processing to do and want this to be created and displayed.
   */
  fun create(): T

  /**
   * Creates a [Dialog] with the arguments supplied to this builder and [Dialog.show]'s the dialog.
   */
  fun show(): T

  /** [AlertDialog.Builder] wrapper */
  class Impl : AlertDialogBuilder<AlertDialog> {
    private var builder: AlertDialog.Builder? = null

    override fun init(context: Context): AlertDialogBuilder<*> {
      builder = AlertDialog.Builder(context)
      return this
    }

    override fun setTitle(titleId: Int): AlertDialogBuilder<*> {
      builder!!.setTitle(titleId)
      return this
    }

    override fun setMessage(@StringRes messageId: Int): AlertDialogBuilder<*> {
      builder!!.setMessage(messageId)
      return this
    }

    override fun setView(view: View?): AlertDialogBuilder<*> {
      builder!!.setView(view)
      return this
    }

    override fun setSingleChoiceItems(
        items: Array<CharSequence?>?,
        checkedItem: Int,
        listener: DialogInterface.OnClickListener?,
    ): AlertDialogBuilder<*> {
      builder!!.setSingleChoiceItems(items, checkedItem, listener)
      return this
    }

    override fun setNegativeButton(
        @StringRes textId: Int,
        listener: DialogInterface.OnClickListener?,
    ): AlertDialogBuilder<*> {
      builder!!.setNegativeButton(textId, listener)
      return this
    }

    override fun setPositiveButton(
        @StringRes textId: Int,
        listener: DialogInterface.OnClickListener?,
    ): AlertDialogBuilder<*> {
      builder!!.setPositiveButton(textId, listener)
      return this
    }

    override fun setNeutralButton(
        @StringRes textId: Int,
        listener: DialogInterface.OnClickListener?,
    ): AlertDialogBuilder<*> {
      builder!!.setNeutralButton(textId, listener)
      return this
    }

    override fun create(): AlertDialog {
      return builder!!.create()
    }

    override fun show(): AlertDialog {
      return builder!!.show()
    }
  }
}
