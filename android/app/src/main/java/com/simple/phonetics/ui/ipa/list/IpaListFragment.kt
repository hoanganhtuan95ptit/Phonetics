package com.simple.phonetics.ui.ipa.list

import android.content.ComponentCallbacks
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import com.simple.adapter.MultiAdapter
import com.simple.analytics.logAnalytics
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.doOnChangeHeightStatusAndHeightNavigation
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.R
import com.simple.phonetics.databinding.FragmentListHeaderHorizontalBinding
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.adapters.IpaAdapters
import com.simple.phonetics.ui.base.fragments.BaseFragment
import com.simple.phonetics.utils.exts.ListPreviewAdapter
import com.simple.phonetics.utils.exts.collectWithLockTransitionIfCached
import com.simple.phonetics.utils.exts.submitListAwaitV2
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.deeplink.sendDeeplink
import com.simple.phonetics.utils.exts.getOrTransparent

class IpaListFragment : BaseFragment<FragmentListHeaderHorizontalBinding, IpaListViewModel>() {


    private var adapter by autoCleared<MultiAdapter>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logAnalytics("ipa_list_show")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.root.doOnChangeHeightStatusAndHeightNavigation(viewLifecycleOwner) { heightStatusBar: Int, heightNavigationBar: Int ->

            binding.root.updatePadding(top = heightStatusBar)
            binding.recyclerView.updatePadding(left = DP.DP_12, right = DP.DP_12, bottom = heightNavigationBar + DP.DP_24)
        }

        binding.frameHeader.icBack.setDebouncedClickListener {

            activity?.supportFragmentManager?.popBackStack()
        }

        setupRecyclerView()

        observeData()
    }

    private fun setupRecyclerView() {

        val binding = binding ?: return

        val ipaAdapter = IpaAdapters { view, item ->

            val transitionName = view.transitionName ?: item.id

            sendDeeplink(
                deepLink = DeeplinkManager.IPA_DETAIL,
                extras = mapOf(Param.IPA to item.data, Param.ROOT_TRANSITION_NAME to transitionName),
                sharedElement = mapOf(transitionName to view)
            )
        }

        adapter = MultiAdapter(ipaAdapter, *ListPreviewAdapter()).apply {

            binding.recyclerView.adapter = this
            binding.recyclerView.itemAnimator = null

            binding.recyclerView.layoutManager = GridLayoutManager(context, 3)
        }
    }

    private fun observeData() = with(viewModel) {

        val fragment = this@IpaListFragment

        theme.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.root.setBackgroundColor(it.getOrTransparent("colorBackground"))
        }

        title.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.frameHeader.tvTitle.text = it
        }

        viewItemList.collectWithLockTransitionIfCached(fragment = fragment, tag = "VIEW_ITEM_LIST") { data, isFirst ->

            val binding = binding ?: return@collectWithLockTransitionIfCached

            binding.recyclerView.submitListAwaitV2(viewItemList = data, isFirst = isFirst)
        }
    }
}

@Deeplink
class IpaListDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return DeeplinkManager.IPA_LIST
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is MainActivity) return false

        val fragment = IpaListFragment()
        fragment.arguments = bundleOf(*extras?.toList().orEmpty().toTypedArray())

        val fragmentTransaction = componentCallbacks.supportFragmentManager
            .beginTransaction()

        sharedElement?.forEach { (t, u) ->

            fragmentTransaction.addSharedElement(u, t)
        }

        fragmentTransaction
            .replace(R.id.fragment_container, fragment, "")
            .addToBackStack("")
            .commit()

        return true
    }
}