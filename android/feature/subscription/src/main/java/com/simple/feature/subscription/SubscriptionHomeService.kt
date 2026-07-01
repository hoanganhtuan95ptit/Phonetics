package com.simple.feature.subscription

import androidx.fragment.app.activityViewModels
import com.simple.autobind.annotation.AutoBind
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.deeplink.sendDeeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.services.HomeService
import com.simple.phonetics.ui.main.services.ads.AdsViewModel
import com.simple.ui.precompute.image.setImage
import com.simple.ui.precompute.image.build
import com.simple.ui.precompute.image.toBuilder
import com.unknown.coroutines.launchCollect

@AutoBind(HomeFragment::class)
class SubscriptionHomeService : HomeService {

    private lateinit var viewModel: SubscriptionHomeViewModel

    private lateinit var adsViewModel: AdsViewModel

    override fun setup(homeFragment: HomeFragment) {

        viewModel = homeFragment.activityViewModels<SubscriptionHomeViewModel>().value
        adsViewModel = homeFragment.activityViewModels<AdsViewModel>().value

        setupView(homeFragment)

        collectData(homeFragment)
    }

    private fun setupView(homeFragment: HomeFragment) {

        val binding = homeFragment.binding ?: return

        binding.ivPremium.setDebouncedClickListener {

            val transitionName = binding.ivPremium.transitionName

            sendDeeplink(DeeplinkManager.SUBSCRIPTION, extras = mapOf(Param.ROOT_TRANSITION_NAME to transitionName), sharedElement = mapOf(transitionName to binding.ivPremium))
        }

        binding.ivPremium.setImage(R.drawable.ic_subs_vip_24dp.toBuilder().build())
    }

    private fun collectData(homeFragment: HomeFragment) = with(viewModel) {

        subscriptionIdOld.launchCollect(homeFragment.viewLifecycleOwner) {

            adsViewModel.lockAds("Subscription", it.isNotEmpty())
        }

        subscriptionPlanViewItemList.launchCollect(homeFragment.viewLifecycleOwner) {

            val binding = homeFragment.binding ?: return@launchCollect

            binding.ivPremium.setVisible(it.isNotEmpty())
        }
    }
}
