package com.simple.phonetics.ui.base.fragments.list

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.utils.JobQueue
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.postValueIfActive
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

abstract class ListViewModel : BaseViewModel() {

    @VisibleForTesting
    val jobQueue = JobQueue()


    @VisibleForTesting
    val typeViewItemList: LiveData<HashMap<Int, List<ViewItem>>> = MutableLiveData(hashMapOf())


    val viewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff(typeViewItemList) {

        val typeViewItemList = typeViewItemList.get().toMutableMap()

        val list = arrayListOf<ViewItem>()

        typeViewItemList.toList().sortedBy {

            it.first
        }.forEach {

            list.addAll(it.second)
        }

        postValueIfActive(list)
    }

    fun updateTypeViewItemList(type: Int, it: List<ViewItem>) = jobQueue.submit(handler + Dispatchers.IO) {

        val map = typeViewItemList.asFlow().first()

        map[type] = it

        typeViewItemList.postValue(map)
    }
}