package com.simple.phonetics.ui.ipa.detail

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.JustifyContent
import com.simple.adapter.MultiAdapter
import com.simple.analytics.logAnalytics
import com.simple.coreapp.ui.adapters.texts.ClickTextAdapter
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.doOnChangeHeightStatusAndHeightNavigation
import com.simple.coreapp.utils.ext.getParcelableOrNull
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.Deeplink
import com.simple.phonetics.Id
import com.simple.phonetics.Param
import com.simple.phonetics.R
import com.simple.phonetics.databinding.FragmentListHeaderHorizontalBinding
import com.simple.phonetics.entities.Ipa
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.adapters.PhoneticsAdapter
import com.simple.phonetics.ui.base.fragments.BaseFragment
import com.simple.phonetics.ui.ipa.detail.adapters.IpaDetailAdapters
import com.simple.phonetics.utils.DeeplinkHandler
import com.simple.phonetics.utils.exts.ListPreviewAdapter
import com.simple.phonetics.utils.exts.collectWithLockTransitionIfCached
import com.simple.phonetics.utils.exts.createFlexboxLayoutManager
import com.simple.phonetics.utils.exts.submitListAwaitV2
import com.simple.phonetics.utils.sendDeeplink
import com.simple.state.toSuccess

class IpaDetailFragment : BaseFragment<FragmentListHeaderHorizontalBinding, IpaDetailViewModel>() {

    private val configViewModel: ConfigViewModel by lazy {
        getViewModel(requireActivity(), ConfigViewModel::class)
    }

    private var adapter by autoCleared<MultiAdapter>()


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
        observePhoneticsConfigData()
    }

    private fun setupRecyclerView() {

        val binding = binding ?: return

        val clickTextAdapter = ClickTextAdapter { view, item ->

            if (item.id.startsWith(Id.SENTENCE) && item.data is Sentence) {

                sendDeeplink(Deeplink.SPEAK, extras = bundleOf(Param.TEXT to (item.data as Sentence).text))
            }
        }

        val phoneticsAdapter = PhoneticsAdapter { view, item ->

            if (viewModel.isSupportSpeak.value == true) {

                sendDeeplink(Deeplink.SPEAK, extras = bundleOf(Param.TEXT to item.data.text))
            } else if (viewModel.isSupportListen.value == true) viewModel.startListen(
                text = item.data.text,

                voiceId = configViewModel.voiceSelect.value ?: 0,
                voiceSpeed = configViewModel.voiceSpeed.value ?: 1f
            )
        }

        val ipaDetailAdapters = IpaDetailAdapters { view, item ->

            viewModel.startListen(item.data)
        }

        adapter = MultiAdapter(clickTextAdapter, phoneticsAdapter, ipaDetailAdapters, *ListPreviewAdapter()).apply {

            binding.recyclerView.adapter = this
            binding.recyclerView.itemAnimator = null
            binding.recyclerView.setItemViewCacheSize(10)

            val layoutManager = createFlexboxLayoutManager(context = context) {

                logCrashlytics(
                    event = "IPA_DETAIL",
                    throwable = it,
                    "VIEW_ITEM_SIZE" to "${viewModel.viewItemList.value?.size}"
                )
            }
            layoutManager.flexDirection = FlexDirection.ROW
            layoutManager.justifyContent = JustifyContent.FLEX_START
            binding.recyclerView.layoutManager = layoutManager
        }
    }

    private fun observeData() = with(viewModel) {

        val fragment = this@IpaDetailFragment

        theme.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.root.setBackgroundColor(it.colorBackground)
        }

        title.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.frameHeader.tvTitle.text = it
        }

        viewItemList.collectWithLockTransitionIfCached(fragment = fragment, tag = "VIEW_ITEM_LIST") { data, isFirst ->

            val binding = binding ?: return@collectWithLockTransitionIfCached

            binding.recyclerView.submitListAwaitV2(viewItemList = data, isFirst = isFirst)
        }

        arguments?.getParcelableOrNull<Ipa>(Param.IPA)?.let {

            this.updateIpa(it)

            logAnalytics("ipa_detail_show_" + it.ipa.lowercase())
        }
    }

    private fun observePhoneticsConfigData() = with(configViewModel) {

        voiceState.observe(viewLifecycleOwner) {

            viewModel.updateSupportListen(it.toSuccess()?.data.orEmpty().isNotEmpty())
        }

        phoneticSelect.observe(viewLifecycleOwner) {

            viewModel.updatePhoneticCodeSelected(it)
        }
    }
}

@com.tuanha.deeplink.annotation.Deeplink
class IpaDetailDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return Deeplink.IPA_DETAIL
    }

    override suspend fun navigation(activity: ComponentActivity, deepLink: String, extras: Bundle?, sharedElement: Map<String, View>?): Boolean {

        if (activity !is MainActivity) return false

        val fragment = IpaDetailFragment()
        fragment.arguments = extras

        val fragmentTransaction = activity.supportFragmentManager
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