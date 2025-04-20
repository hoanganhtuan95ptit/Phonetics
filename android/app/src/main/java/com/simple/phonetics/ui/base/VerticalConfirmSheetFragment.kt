package com.simple.phonetics.ui.base

import android.content.ComponentCallbacks
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.simple.adapter.SpaceViewItem
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.extentions.asListOrNull
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.core.utils.extentions.orZero
import com.simple.coreapp.Param
import com.simple.coreapp.ui.adapters.ImageViewItem
import com.simple.coreapp.ui.adapters.texts.NoneTextViewItem
import com.simple.coreapp.ui.base.dialogs.sheet.BaseViewModelSheetFragment
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.ButtonInfo
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.doOnHeightStatusAndHeightNavigationChange
import com.simple.coreapp.utils.ext.getParcelableOrNull
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.observeLaunch
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.EventName
import com.simple.phonetics.databinding.DialogListBinding
import com.simple.phonetics.databinding.LayoutActionVerticalBinding
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.sendEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.math.absoluteValue

private val verticalConfirmItemList by lazy {

    MutableSharedFlow<List<ViewItem>>(replay = 1, extraBufferCapacity = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.SUSPEND)
}

class VerticalConfirmSheetFragment : BaseViewModelSheetFragment<DialogListBinding, VerticalConfirmViewModel>() {


    private var resultCode: Int = 0

    private var bindingConfirmSpeak by autoCleared<LayoutActionVerticalBinding>()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isCancelable = arguments?.getBoolean(Param.CANCEL) == true
        dialog?.setCancelable(isCancelable)
        dialog?.setCanceledOnTouchOutside(isCancelable)


        bindingConfirmSpeak = LayoutActionVerticalBinding.inflate(LayoutInflater.from(requireContext()))


        val binding = binding ?: return

        val anchor = arguments?.getParcelableOrNull<Background>(Param.ANCHOR)
        binding.vAnchor.delegate.setBackground(anchor)

        val background = arguments?.getParcelableOrNull<Background>(Param.BACKGROUND)
        binding.root.delegate.setBackground(background)


        setupAction()
        setupRecyclerView()

        observeData()
    }

    override fun onDestroy() {
        super.onDestroy()
        sendEvent(arguments?.getString(Param.KEY_REQUEST).orEmpty(), resultCode)
        sendEvent(EventName.DISMISS, bundleOf(arguments?.getString(Param.KEY_REQUEST).orEmpty() to resultCode))
    }

    private fun setupAction() {

        val binding = bindingConfirmSpeak ?: return

        val layoutParam = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        container?.addView(binding.root, layoutParam)

        val negative = arguments?.getParcelableOrNull<ButtonInfo>(Param.NEGATIVE)
        binding.tvNegative.setVisible(negative != null)
        binding.tvNegative.text = negative?.text
        binding.tvNegative.delegate.setBackground(negative?.background)
        binding.tvNegative.setDebouncedClickListener {
            resultCode = 0
            dismiss()
        }

        val positive = arguments?.getParcelableOrNull<ButtonInfo>(Param.POSITIVE)
        binding.tvPositive.setVisible(positive != null)
        binding.tvPositive.text = positive?.text
        binding.tvPositive.delegate.setBackground(positive?.background)
        binding.tvPositive.setDebouncedClickListener {
            resultCode = 1
            dismiss()
        }

        binding.root.setVisible(negative != null || positive != null)

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

        doOnHeightStatusAndHeightNavigationChange { heightStatusBar, heightNavigationBar ->

            binding.root.updatePadding(top = DP.DP_24, left = DP.DP_24, right = DP.DP_24, bottom = heightNavigationBar + DP.DP_24)
        }
    }

    private fun setupRecyclerView() {

        val binding = binding ?: return

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun observeData() = with(viewModel) {

        theme.observe(viewLifecycleOwner) {

            if (arguments?.getBoolean(Param.CANCEL) == true) Background(
                backgroundColor = it.colorDivider,
                cornerRadius = DP.DP_100,
            ).let {

                binding?.vAnchor?.delegate?.setBackground(it)
            }


            val background = Background(
                backgroundColor = it.colorBackground,
                cornerRadius_TL = DP.DP_24,
                cornerRadius_TR = DP.DP_24
            )

            binding?.root?.delegate?.setBackground(background)
            bindingConfirmSpeak?.root?.delegate?.setBackground(background)
        }

        viewItemList.observeLaunch(viewLifecycleOwner) {

            val binding = binding ?: return@observeLaunch

            binding.recyclerView.submitListAwait(it)
        }
    }
}

class VerticalConfirmViewModel : BaseViewModel() {

    @VisibleForTesting
    val itemList: LiveData<List<ViewItem>> = mediatorLiveData {

        verticalConfirmItemList.collect {

            postValue(it)
        }
    }

    @VisibleForTesting
    val actionHeight: LiveData<Int> = mediatorLiveData {

    }

    val viewItemList: LiveData<List<ViewItem>> = combineSources(size, theme, itemList, actionHeight) {

        val list = arrayListOf<ViewItem>()

        list.addAll(itemList.getOrEmpty())

        list.add(SpaceViewItem(id = "1", height = actionHeight.get()))

        postDifferentValue(list)
    }

    fun updateActionHeight(height: Int) {

        actionHeight.postDifferentValue(height)
    }
}


@Deeplink(queue = "Confirm")
class ConfirmDeeplinkHandler : DeeplinkHandler {

    override fun getDeeplink(): String {
        return DeeplinkManager.CONFIRM
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is FragmentActivity) {
            return false
        }


        verticalConfirmItemList.resetReplayCache()
        verticalConfirmItemList.emit(getViewItemList(extras))


        val fragment = VerticalConfirmSheetFragment()

        fragment.arguments = extras.orEmpty().filter {

            it !is ViewItem
        }.let {

            bundleOf(*it.toList().toTypedArray())
        }

        fragment.showOrAwaitDismiss(componentCallbacks.supportFragmentManager, "")


        return true
    }

    private fun getViewItemList(extras: Map<String, Any?>?): List<ViewItem> = arrayListOf<ViewItem>().apply {

        extras.orEmpty()[Param.ANIM]?.asObjectOrNull<Int>()?.let {

            ImageViewItem(id = "", anim = it)
        }?.let {

            add(it)
        }

        extras.orEmpty()[Param.TITLE]?.asObjectOrNull<CharSequence>()?.let {

            NoneTextViewItem(id = "TITLE", text = it)
        }?.let {

            add(it)
            add(SpaceViewItem(id = "SPACE_TITLE_MESSAGE", height = DP.DP_24))
        }

        extras.orEmpty()[Param.MESSAGE]?.asObjectOrNull<CharSequence>()?.let {

            NoneTextViewItem(id = "MESSAGE", text = it)
        }?.let {

            add(it)
            add(SpaceViewItem(id = "SPACE_MESSAGE", height = DP.DP_24))
        }

        addAll(extras.orEmpty()[com.simple.phonetics.Param.VIEW_ITEM_LIST].asListOrNull<ViewItem>().orEmpty())
    }
}