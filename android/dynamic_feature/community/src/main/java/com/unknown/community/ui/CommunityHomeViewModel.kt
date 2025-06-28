package com.unknown.community.ui

import android.graphics.Color
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postDifferentValueIfActive
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.TitleViewItem
import com.unknown.community.data.repositories.CommunityRepository
import com.unknown.community.entities.CommunityInvite
import kotlinx.coroutines.flow.firstOrNull

class CommunityHomeViewModel() : BaseViewModel() {

    @VisibleForTesting
    val community: LiveData<CommunityInvite> = mediatorLiveData {

        CommunityRepository.instance.getCommunitiesAsync().firstOrNull()?.firstOrNull()?.let {
            postDifferentValue(it)
        }
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSources(theme, translate, community) {

        val theme = theme.get()
        val translate = translate.get()
        val community = community.get()

        val viewItemList = arrayListOf<ViewItem>()

        viewItemList.add(TitleViewItem(community.deeplink.orEmpty(), text = community.title.orEmpty().with(ForegroundColor(Color.BLUE))))

        postDifferentValueIfActive(viewItemList)
    }
}