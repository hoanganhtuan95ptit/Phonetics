package com.phonetics.campaign.ui

import androidx.fragment.app.activityViewModels
import com.google.auto.service.AutoService
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.phonetics.ui.home.view.HomeView
import org.koin.androidx.viewmodel.ext.android.viewModel

@AutoService(HomeView::class)
class CampaignHomeView : HomeView {

    private lateinit var viewModel: CampaignHomeViewModel

    private lateinit var homeViewModel: HomeViewModel

    override fun setup(fragment: HomeFragment) {

        viewModel = fragment.activityViewModels<CampaignHomeViewModel>().value

        homeViewModel = fragment.viewModel<HomeViewModel>().value


        observeData(fragment)
    }

    private fun observeData(fragment: HomeFragment) = with(viewModel) {

        viewItemList.observe(fragment.viewLifecycleOwner) {

            homeViewModel.updateTypeViewItemList(type = -1, it)
        }
    }
}