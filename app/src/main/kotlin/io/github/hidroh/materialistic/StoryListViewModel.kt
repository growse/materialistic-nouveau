package io.github.hidroh.materialistic

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.core.util.Pair

import io.github.hidroh.materialistic.data.Item
import io.github.hidroh.materialistic.data.ItemManager
import rx.Observable
import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers

open class StoryListViewModel : ViewModel() {
    private var mItemManager: ItemManager? = null
    private var mIoThreadScheduler: Scheduler? = null
    private MutableLiveData<Pair<Item[], Item[]>> mItems; // first = last updated, second = current

    fun inject(itemManager: ItemManager, ioThreadScheduler: Scheduler) {
        mItemManager = itemManager
        mIoThreadScheduler = ioThreadScheduler
    }

    fun getStories(filter: String, cacheMode: Int): LiveData<Pair<Array<Item>, Array<Item>>> {
        if (mItems == null) {
            mItems = MutableLiveData<>()
            Observable.fromCallable(() -> mItemManager.getStories(filter, cacheMode))
                    .subscribeOn(mIoThreadScheduler)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(items -> setItems(items))
        }
        return mItems
    }

    fun refreshStories(filter: String, cacheMode: Int) {
        if (mItems == null || mItems.getValue() == null) {
            return
        }
        Observable.fromCallable(() -> mItemManager.getStories(filter, cacheMode))
                .subscribeOn(mIoThreadScheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(items -> setItems(items))

    }

    fun setItems(items: Array<Item>) {
        mItems.setValue(Pair.create(mItems.getValue() != null ? mItems.getValue().second : null, items))
    }
}
