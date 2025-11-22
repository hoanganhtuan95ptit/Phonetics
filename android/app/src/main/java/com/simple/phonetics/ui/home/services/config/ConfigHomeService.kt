package com.simple.phonetics.ui.home.services.config

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simple.adapter.MultiAdapter
import com.simple.autobind.annotation.AutoBind
import com.simple.coreapp.ui.adapters.texts.ClickTextAdapter
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.deeplink.sendDeeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.services.HomeService
import com.simple.phonetics.utils.exts.collectWithLockTransitionIfCached
import com.simple.phonetics.utils.exts.submitListAndAwait

@AutoBind(HomeFragment::class)
class ConfigHomeService : HomeService {

    private lateinit var configViewModel: ConfigViewModel

    override fun setup(homeFragment: HomeFragment) {

        configViewModel = homeFragment.getViewModel(homeFragment.requireActivity(), ConfigViewModel::class)

        setupRecyclerView(homeFragment = homeFragment)

        observeConfigData(homeFragment = homeFragment)
    }

    private fun setupRecyclerView(homeFragment: HomeFragment) {

        val binding = homeFragment.binding ?: return

        val textAdapter = ClickTextAdapter { _, _ ->

            sendDeeplink(DeeplinkManager.CONFIG)
        }

        MultiAdapter(textAdapter).apply {

            binding.recFilter.adapter = this
            binding.recFilter.itemAnimator = null

            binding.recFilter.layoutManager = LinearLayoutManager(homeFragment.requireContext(), RecyclerView.HORIZONTAL, false)
        }
    }

    private fun observeConfigData(homeFragment: HomeFragment) = with(configViewModel) {

        listConfig.collectWithLockTransitionIfCached(fragment = homeFragment, tag = "CONFIG_VIEW_ITEM_LIST") { data, isFromCache ->

            val binding = homeFragment.binding ?: return@collectWithLockTransitionIfCached

            binding.recFilter.submitListAndAwait(viewItemList = data, isAnimation = !isFromCache)
        }
    }
}