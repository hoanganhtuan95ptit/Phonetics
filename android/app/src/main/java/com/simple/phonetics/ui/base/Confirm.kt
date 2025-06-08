package com.simple.phonetics.ui.base

import android.content.ComponentCallbacks
import android.os.Bundle
import android.os.Parcelable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.extentions.asListOrNull
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
import com.simple.event.sendEvent
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.databinding.DialogListBinding
import com.simple.phonetics.databinding.LayoutActionVerticalBinding
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.getOrTransparent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.Serializable
import kotlin.math.absoluteValue

private val verticalConfirmItemList by lazy {

    MutableSharedFlow<List<ViewItem>>(replay = 1, extraBufferCapacity = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.SUSPEND)
}

class VerticalConfirmSheetFragment : BaseViewModelSheetFragment<DialogListBinding, VerticalConfirmViewModel>() {


    private var resultCode: Int = -1

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

        doOnHeightStatusAndHeightNavigationChange { _, heightNavigationBar ->

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
                backgroundColor = it.getOrTransparent("colorDivider"),
                cornerRadius = DP.DP_100,
            ).let {

                binding?.vAnchor?.delegate?.setBackground(it)
            }

            val background = Background(
                backgroundColor = arguments?.getInt(com.simple.phonetics.Param.BACKGROUND_COLOR).takeIf { it != 0 } ?: it.getOrTransparent("colorBackground"),
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

    companion object {

        suspend fun showOrAwaitDismiss(fragmentManager: FragmentManager, extras: Map<String, Any?>?) {

            verticalConfirmItemList.resetReplayCache()
            verticalConfirmItemList.emit(extras.orEmpty()[com.simple.phonetics.Param.VIEW_ITEM_LIST].asListOrNull<ViewItem>().orEmpty().toList())


            val fragment = VerticalConfirmSheetFragment()

            fragment.arguments = extras.orEmpty().filter {

                it.value !is List<*>
            }.filter {

                it.value is CharSequence || it.value is Int || it.value is Long || it.value is Float || it.value is Double || it.value is Serializable || it.value is Parcelable
            }.let {

                bundleOf(*it.toList().toTypedArray())
            }

            fragment.showOrAwaitDismiss(fragmentManager, "")
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

    override suspend fun acceptDeeplink(deepLink: String): Boolean {
        return deepLink.startsWith(DeeplinkManager.CONFIRM)
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is FragmentActivity) {
            return false
        }

        VerticalConfirmSheetFragment.showOrAwaitDismiss(componentCallbacks.supportFragmentManager, extras)

        return true
    }
}