package com.simple.feature.subscription.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.simple.adapter.MultiAdapter
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.ext.doOnChangeHeightStatusAndHeightNavigation
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setText
import com.simple.coreapp.utils.ext.setVisible
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.deeplink.sendDeeplink
import com.simple.feature.subscription.ui.adapters.SubscriptionPlanAdapter
import com.simple.image.setImage
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.databinding.FragmentListHeaderVerticalBinding
import com.simple.phonetics.ui.base.fragments.BaseFragment
import com.simple.phonetics.utils.exts.collectWithLockTransitionIfCached
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.simple.phonetics.utils.exts.replace
import com.simple.phonetics.utils.exts.submitListAndAwait
import com.simple.state.doSuccess
import com.unknown.coroutines.launchCollect
import com.unknown.theme.utils.exts.colorBackground

class SubscriptionFragment : BaseFragment<FragmentListHeaderVerticalBinding, SubscriptionViewModel>() {

    override val viewModel: SubscriptionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.root.doOnChangeHeightStatusAndHeightNavigation(viewLifecycleOwner) { heightStatusBar: Int, heightNavigationBar: Int ->

            binding.root.updatePadding(top = heightStatusBar, bottom = heightNavigationBar)
        }

        setupHeader()
        setupConfirm()
        setupRecyclerView()

        observeViewModel()
    }

    private fun setupHeader() {

        val binding = binding ?: return

        binding.frameHeader.icBack.setDebouncedClickListener {

            dismiss()
        }
    }

    private fun setupConfirm() {

        val binding = binding ?: return

        binding.frameConfirm.rootLayoutConfirm.setDebouncedClickListener {

            viewModel.changeSubscriptionPlan()
        }
    }

    private fun setupRecyclerView() {

        val binding = binding ?: return

        val subscriptionPlanAdapter = SubscriptionPlanAdapter { item ->

            viewModel.updateSubscriptionPlanSelected(item.data)
        }

        MultiAdapter(subscriptionPlanAdapter).apply {

            binding.recyclerView.adapter = this
            binding.recyclerView.itemAnimator = null

            binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() = with(viewModel) {

        val fragment = this@SubscriptionFragment

        themes.collectWithLockTransitionUntilData(fragment = fragment, tag = "THEME") {

            val binding = binding ?: return@collectWithLockTransitionUntilData

            binding.root.setBackgroundColor(it.colorBackground)
        }

        headerInfo.collectWithLockTransitionUntilData(fragment = fragment, tag = "HEADER") {

            val binding = binding ?: return@collectWithLockTransitionUntilData

            binding.frameHeader.icBack.setImage(it.back)

            binding.frameHeader.tvTitle.setText(it.title)
            binding.frameHeader.tvMessage.setText(it.message)
        }

        confirmInfo.collectWithLockTransitionUntilData(fragment = fragment, tag = "CONFIRM_INFO") {

            val binding = binding?.frameConfirm ?: return@collectWithLockTransitionUntilData

            binding.btnConfirm.setText(it.text)
            binding.progress.setVisible(it.isShowLoading)

            binding.root.isClickable = it.isClickable
            binding.root.setBackground(it.background)
        }

        confirmState.launchCollect(viewLifecycleOwner) {

            it.doSuccess {

                doSuccess(it)
            }
        }

        subscriptionPlanViewItemList.collectWithLockTransitionIfCached(fragment = fragment, tag = "VIEW_ITEM_LIST") { data, isFirst ->

            val binding = binding ?: return@collectWithLockTransitionIfCached

            binding.recyclerView.submitListAndAwait(viewItemList = data, isAnimation = isFirst)
        }
    }

    private fun doSuccess(action: SubscriptionViewModel.ResultInfo) {

        val extras = mapOf(

            com.simple.coreapp.Param.CANCEL to false,
            com.simple.coreapp.Param.POSITIVE to action.positive,
            com.simple.coreapp.Param.BACKGROUND to action.background,

            Param.VIEW_ITEM_LIST to action.viewItemList
        )

        sendDeeplink(DeeplinkManager.CONFIRM, extras = extras)
    }
}


@Deeplink("subscription")
class SubscriptionDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return DeeplinkManager.SUBSCRIPTION
    }

    override suspend fun navigation(activity: AppCompatActivity, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        activity.replace(fragment = SubscriptionFragment(), extras = extras, sharedElement = sharedElement)

        return true
    }
}