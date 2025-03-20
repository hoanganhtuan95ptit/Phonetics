package com.simple.phonetics.ui.language

import android.os.Bundle
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.lifecycle.asFlow
import androidx.recyclerview.widget.LinearLayoutManager
import com.simple.adapter.MultiAdapter
import com.simple.analytics.logAnalytics
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.doOnChangeHeightStatusAndHeightNavigation
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setInvisible
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.ext.with
import com.simple.phonetics.Deeplink
import com.simple.phonetics.Param
import com.simple.phonetics.R
import com.simple.phonetics.databinding.FragmentListHeaderVerticalBinding
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.fragments.BaseFragment
import com.simple.phonetics.ui.language.adapters.LanguageAdapter
import com.simple.phonetics.utils.DeeplinkHandler
import com.simple.phonetics.utils.exts.ListPreviewAdapter
import com.simple.phonetics.utils.exts.collectWithLockTransitionIfCached
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.simple.phonetics.utils.exts.submitListAwaitV2
import com.simple.phonetics.utils.sendDeeplink
import com.simple.phonetics.utils.sendToast
import com.simple.state.ResultState

class LanguageFragment : BaseFragment<FragmentListHeaderVerticalBinding, LanguageViewModel>() {

    private var adapter by autoCleared<MultiAdapter>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {

            override fun handleOnBackPressed() {

                if (arguments?.getString(Param.ROOT_TRANSITION_NAME) == null) activity?.finish()
                else activity?.supportFragmentManager?.popBackStack()
            }
        })

        val binding = binding ?: return

        binding.root.doOnChangeHeightStatusAndHeightNavigation(viewLifecycleOwner) { heightStatusBar: Int, heightNavigationBar: Int ->

            binding.root.updatePadding(top = heightStatusBar, bottom = heightNavigationBar)
        }


        val isFirst = arguments?.getBoolean(Param.FIRST) == true

        binding.frameHeader.icBack.setInvisible(isFirst)
        binding.frameHeader.icBack.isClickable = !isFirst
        binding.frameHeader.icBack.setDebouncedClickListener {

            activity?.supportFragmentManager?.popBackStack()
        }

        binding.frameConfirm.rootLayoutConfirm.setDebouncedClickListener {

            viewModel.changeLanguageInput()
        }

        setupRecyclerView()

        observeData()
    }

    private fun setupRecyclerView() {

        val binding = binding ?: return

        val languageAdapter = LanguageAdapter { view, item ->

            viewModel.updateLanguageSelected(item.data)

            logAnalytics("ipa_${item.data.id}")
        }

        adapter = MultiAdapter(languageAdapter, *ListPreviewAdapter()).apply {

            binding.recyclerView.adapter = this
            binding.recyclerView.itemAnimator = null
            binding.recyclerView.setItemViewCacheSize(10)

            binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeData() = with(viewModel) {

        val fragment = this@LanguageFragment

        theme.collectWithLockTransitionUntilData(fragment = fragment, tag = "THEME") {

            val binding = binding ?: return@collectWithLockTransitionUntilData

            binding.root.setBackgroundColor(it.colorBackground)
            binding.frameHeader.icBack.setColorFilter(it.colorOnBackground)
        }

        headerInfo.collectWithLockTransitionUntilData(fragment = fragment, tag = "HEADER") {

            val binding = binding ?: return@collectWithLockTransitionUntilData

            binding.frameHeader.tvTitle.text = it.title
            binding.frameHeader.tvMessage.text = it.message
        }

        buttonInfo.collectWithLockTransitionUntilData(fragment = fragment, tag = "BUTTON_INFO") {

            val binding = binding?.frameConfirm ?: return@collectWithLockTransitionUntilData

            binding.btnConfirm.text = it.text
            binding.progress.setVisible(it.isShowLoading)

            binding.root.isClickable = it.isClickable
            binding.root.delegate.setBackground(it.background)
        }

        languageViewItemList.collectWithLockTransitionIfCached(fragment = fragment, tag = "VIEW_ITEM_LIST") { data, isFirst ->

            val binding = binding ?: return@collectWithLockTransitionIfCached

            binding.recyclerView.submitListAwaitV2(viewItemList = data, isFirst = isFirst)
        }

        changeLanguageState.asFlow().launchCollect(viewLifecycleOwner) {

            val theme = theme.value ?: return@launchCollect
            val binding = binding ?: return@launchCollect

            if (it is ResultState.Success) if (arguments?.containsKey(Param.ROOT_TRANSITION_NAME) != true) sendDeeplink(
                deepLink = Deeplink.PHONETICS,
                extras = bundleOf(
                    Param.ROOT_TRANSITION_NAME to ""
                ),
                sharedElement = mapOf(
                    binding.root.transitionName to binding.root
                )
            ) else {

                activity?.supportFragmentManager?.popBackStack()
            }

            if (it is ResultState.Failed) sendToast(
                extras = bundleOf(
                    com.simple.coreapp.Param.MESSAGE to it.cause.message.orEmpty().with(ForegroundColorSpan(theme.colorOnErrorVariant)),
                    com.simple.coreapp.Param.BACKGROUND to Background(
                        backgroundColor = theme.colorErrorVariant,
                        cornerRadius = DP.DP_16,
                    )
                )
            )
        }
    }
}

@com.tuanha.deeplink.annotation.Deeplink
class LanguageDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return Deeplink.LANGUAGE
    }

    override suspend fun navigation(activity: ComponentActivity, deepLink: String, extras: Bundle?, sharedElement: Map<String, View>?): Boolean {

        if (activity !is MainActivity) return false

        val fragment = LanguageFragment()
        fragment.arguments = extras

        val fragmentTransaction = activity.supportFragmentManager
            .beginTransaction()

        sharedElement?.forEach { (t, u) ->

            fragmentTransaction.addSharedElement(u, t)
        }

        fragmentTransaction
            .replace(R.id.fragment_container, fragment, "")
            .addToBackStack("")
            .commitAllowingStateLoss()

        return true
    }
}
