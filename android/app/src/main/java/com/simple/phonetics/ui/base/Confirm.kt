package com.simple.phonetics.ui.base

import android.content.ComponentCallbacks
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.extentions.asListOrNull
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.core.utils.extentions.orZero
import com.simple.coreapp.Param
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.base.dialogs.sheet.BaseViewModelSheetFragment
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.ButtonInfo
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.doOnHeightStatusAndHeightNavigationChange
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setText
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.observeLaunch
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.event.sendEvent
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.databinding.DialogListBinding
import com.simple.phonetics.databinding.LayoutActionVerticalBinding
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.colorDivider
import com.unknown.theme.utils.exts.colorBackground
import kotlinx.coroutines.flow.first
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.absoluteValue


class VerticalConfirmSheetFragment : BaseViewModelSheetFragment<DialogListBinding, VerticalConfirmViewModel>() {


    override val viewModel by lazy {
        viewModels<VerticalConfirmViewModel>().value
    }


    private var resultCode: Int = -1

    private var bindingConfirmAction by autoCleared<LayoutActionVerticalBinding>()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isCancelable = arguments?.getBoolean(Param.CANCEL) == true
        dialog?.setCancelable(isCancelable)
        dialog?.setCanceledOnTouchOutside(isCancelable)


        bindingConfirmAction = LayoutActionVerticalBinding.inflate(LayoutInflater.from(requireContext()))


        setupAction()
        setupRecyclerView()

        observeData()
        observeConfirmData()
    }

    override fun onDestroy() {
        super.onDestroy()
        sendEvent(arguments?.getString(Param.KEY_REQUEST).orEmpty(), resultCode)
    }

    private fun setupAction() {

        val binding = bindingConfirmAction ?: return

        val layoutParam = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        container?.addView(binding.root, layoutParam)

        binding.tvNegative.setDebouncedClickListener {

            resultCode = 0
            dismiss()
        }

        binding.tvPositive.setDebouncedClickListener {

            resultCode = 1
            dismiss()
        }

        behavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

            override fun onStateChanged(bottomSheet: View, newState: Int) {

            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

                val translateY = (1 + slideOffset) * this@VerticalConfirmSheetFragment.bottomSheet?.height.orZero() - viewModel.actionHeight.get()
                if (translateY < 0) binding.root.translationY = translateY.absoluteValue
            }
        })

        binding.root.viewTreeObserver.addOnGlobalLayoutListener {

            viewModel.updateActionHeight(binding.root.height)
        }

        doOnHeightStatusAndHeightNavigationChange { _, heightNavigationBar ->

            binding.root.updatePadding(top = DP.DP_24, left = DP.DP_24, right = DP.DP_24, bottom = heightNavigationBar + DP.DP_24)
        }
    }

    private fun setupRecyclerView() {

        val binding = binding ?: return

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun observeData() = with(viewModel) {

        viewItemList.observeLaunch(viewLifecycleOwner) {

            val binding = binding ?: return@observeLaunch

            binding.recyclerView.submitListAwait(it)
        }
    }

    private fun observeConfirmData() = with(activityViewModels<ConfirmViewModel>().value) {

        val id = arguments?.getString(Param.ID).orEmpty()
        val keyRequest = arguments?.getString(Param.KEY_REQUEST).orEmpty()

        infoMap.remove(ConfirmViewModel.Id(id, keyRequest))?.let {


            viewModel.updateViewItem(it.viewItem)


            val binding = binding ?: return@let

            val anchor = it.anchor
            binding.vAnchor.setBackground(anchor)

            val background = it.background
            binding.root.setBackground(background)
            bindingConfirmAction?.root?.setBackground(background)

            val bindingConfirmSpeak = bindingConfirmAction ?: return@let

            val negative = it.negative
            bindingConfirmSpeak.tvNegative.setVisible(negative != null && negative.text.text.isNotBlank())
            bindingConfirmSpeak.tvNegative.setText(negative?.text)
            bindingConfirmSpeak.tvNegative.setBackground(negative?.background)

            val positive = it.positive
            bindingConfirmSpeak.tvPositive.setVisible(positive != null && positive.text.text.isNotBlank())
            bindingConfirmSpeak.tvPositive.setText(positive?.text)
            bindingConfirmSpeak.tvPositive.setBackground(positive?.background)

            binding.root.setVisible(negative != null || positive != null)
        }
    }
}

class VerticalConfirmViewModel : BaseViewModel() {

    @VisibleForTesting
    val itemList: LiveData<List<ViewItem>> = MediatorLiveData()

    @VisibleForTesting
    val actionHeight: LiveData<Int> = MediatorLiveData()

    val viewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff(size, theme, itemList, actionHeight) {

        val list = arrayListOf<ViewItem>()

        list.addAll(itemList.getOrEmpty())

        list.add(SpaceViewItem(id = "1", height = actionHeight.get()))

        postValue(list)
    }

    fun updateViewItem(list: List<ViewItem>) {

        itemList.postValue(list)
    }

    fun updateActionHeight(height: Int) {

        actionHeight.postValue(height)
    }
}

class ConfirmViewModel : BaseViewModel() {

    @VisibleForTesting
    val infoMap = ConcurrentHashMap<Id, ConfirmInfo>()

    fun updateInfo(
        id: String,
        keyRequest: String,

        viewItem: List<ViewItem> = emptyList(),

        anchor: Background? = null,
        background: Background? = null,

        negative: ButtonInfo? = null,
        positive: ButtonInfo? = null
    ) {

        infoMap[Id(id, keyRequest)] = ConfirmInfo(

            viewItem = viewItem,

            anchor = anchor,
            background = background,

            negative = negative,
            positive = positive
        )
    }

    data class Id(
        val id: String,
        val keyRequest: String
    )

    data class ConfirmInfo(

        val viewItem: List<ViewItem> = emptyList(),

        val anchor: Background? = null,
        val background: Background? = null,

        val negative: ButtonInfo? = null,
        val positive: ButtonInfo? = null
    )
}

@Deeplink(queue = "Confirm")
class ConfirmDeeplinkHandler : DeeplinkHandler {

    override suspend fun acceptDeeplink(deepLink: String): Boolean {
        return deepLink.startsWith(DeeplinkManager.CONFIRM)
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is FragmentActivity) {
            return false
        }

        val viewModel by componentCallbacks.viewModels<ConfirmViewModel>()


        val theme = viewModel.theme.asFlow().first()

        val cancel = extras?.get(Param.CANCEL).asObjectOrNull<Boolean>() ?: true

        val anchor = extras.orEmpty()[Param.ANCHOR].asObjectOrNull<Background>() ?: Background(
            cornerRadius = DP.DP_100,
            backgroundColor = theme.colorDivider,
        )

        val background = extras.orEmpty()[Param.BACKGROUND].asObjectOrNull<Background>() ?: Background(
            cornerRadius_TL = DP.DP_24,
            cornerRadius_TR = DP.DP_24,
            backgroundColor = theme.colorBackground
        )


        val id = UUID.randomUUID().toString()
        val keyRequest = extras.orEmpty().get(Param.KEY_REQUEST).asObjectOrNull<String>().orEmpty()

        viewModel.updateInfo(
            id = id,
            keyRequest = keyRequest,

            viewItem = extras.orEmpty()[com.simple.phonetics.Param.VIEW_ITEM_LIST].asListOrNull<ViewItem>().orEmpty().toList(),

            anchor = if (cancel) anchor else null,
            background = background,

            negative = extras.orEmpty().get(Param.NEGATIVE).asObjectOrNull<ButtonInfo>(),
            positive = extras.orEmpty().get(Param.POSITIVE).asObjectOrNull<ButtonInfo>(),
        )


        val fragment = VerticalConfirmSheetFragment()
        fragment.arguments = bundleOf(
            Param.ID to id,
            Param.CANCEL to cancel,
            Param.KEY_REQUEST to keyRequest,
        )
        fragment.showOrAwaitDismiss(componentCallbacks.supportFragmentManager, "")

        return true
    }
}