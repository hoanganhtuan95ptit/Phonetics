package com.simple.feature.campaign.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.simple.autobind.annotation.AutoBind
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.service.FragmentService
import org.koin.androidx.viewmodel.ext.android.viewModel

@AutoBind(HomeFragment::class)
class CampaignHomeView : FragmentService {

    private lateinit var viewModel: CampaignHomeViewModel

    private lateinit var homeViewModel: HomeViewModel

    override suspend fun setup(fragment: Fragment) {

        if (fragment !is HomeFragment) return

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