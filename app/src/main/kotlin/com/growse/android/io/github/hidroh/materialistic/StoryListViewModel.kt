package com.growse.android.io.github.hidroh.materialistic

import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.growse.android.io.github.hidroh.materialistic.data.Item
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager
import java.util.concurrent.Callable
import rx.Observable
import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Action1

class StoryListViewModel : ViewModel() {
  private var mItemManager: ItemManager? = null
  private var mIoThreadScheduler: Scheduler? = null
  private var mItems: MutableLiveData<Pair<Array<Item?>?, Array<Item?>?>?>? =
      null // first = last updated, second = current

  fun inject(itemManager: ItemManager, ioThreadScheduler: Scheduler?) {
    mItemManager = itemManager
    mIoThreadScheduler = ioThreadScheduler
  }

  fun getStories(
      filter: String,
      @ItemManager.CacheMode cacheMode: Int,
  ): LiveData<Pair<Array<Item?>?, Array<Item?>?>?>? {
    if (mItems == null) {
      mItems = MutableLiveData<Pair<Array<Item?>?, Array<Item?>?>?>()
      Observable.fromCallable(
              Callable {
                mItemManager!!.getStories(
                    filter,
                    cacheMode,
                )
              },
          )
          .subscribeOn(mIoThreadScheduler)
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(Action1 { items: Array<Item?>? -> setItems(items) })
    }
    return mItems
  }

  fun refreshStories(filter: String, @ItemManager.CacheMode cacheMode: Int) {
    if (mItems == null || mItems!!.getValue() == null) {
      return
    }
    Observable.fromCallable(
            Callable {
              mItemManager!!.getStories(
                  filter,
                  cacheMode,
              )
            },
        )
        .subscribeOn(mIoThreadScheduler)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(Action1 { items: Array<Item?>? -> setItems(items) })
  }

  fun setItems(items: Array<Item?>?) {
    mItems!!.value =
        Pair.create<Array<Item?>?, Array<Item?>?>(
            if (mItems!!.getValue() != null) mItems!!.getValue()!!.second else null,
            items,
        )
  }
}
