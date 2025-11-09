package com.simple.feature.campaign.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.simple.autobind.annotation.AutoBind
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.phonetics.ui.home.services.HomeService
import org.koin.androidx.viewmodel.ext.android.viewModel

@AutoBind(HomeFragment::class)
class CampaignHomeView : HomeService {

    private lateinit var viewModel: CampaignHomeViewModel

    private lateinit var homeViewModel: HomeViewModel

    override fun setup(homeFragment: HomeFragment) {

        viewModel = homeFragment.activityViewModels<CampaignHomeViewModel>().value
        homeViewModel = homeFragment.viewModel<HomeViewModel>().value


        observeData(homeFragment)
    }

    private fun observeData(fragment: HomeFragment) = with(viewModel) {

        viewItemList.observe(fragment.viewLifecycleOwner) {

            homeViewModel.updateTypeViewItemList(type = -1, it)
        }
    }
}