package com.phonetics.campaign.ui

import androidx.fragment.app.viewModels
import com.google.auto.service.AutoService
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.phonetics.ui.home.view.HomeView
import org.koin.androidx.viewmodel.ext.android.viewModel

@AutoService(HomeView::class)
class CampaignHomeView : HomeView {

    private lateinit var viewModel: HomeViewModel

    private lateinit var campaignHomeViewModel: CampaignHomeViewModel

    override fun setup(fragment: HomeFragment) {

        viewModel = fragment.viewModel<HomeViewModel>().value

        campaignHomeViewModel = fragment.viewModels<CampaignHomeViewModel>().value

        campaignHomeViewModel.viewItemList.observe(fragment.viewLifecycleOwner) {

            viewModel.updateTypeViewItemList(type = -1, it)
        }
    }
}