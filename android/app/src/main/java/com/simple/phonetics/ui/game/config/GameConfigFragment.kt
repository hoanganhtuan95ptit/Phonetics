package com.simple.phonetics.ui.game.config

import android.content.ComponentCallbacks
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.JustifyContent
import com.simple.adapter.MultiAdapter
import com.simple.analytics.logAnalytics
import com.simple.core.utils.extentions.asObject
import com.simple.coreapp.databinding.ItemTextBinding
import com.simple.coreapp.ui.adapters.texts.ClickTextAdapter
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.ui.view.setMargin
import com.simple.coreapp.ui.view.setPadding
import com.simple.coreapp.ui.view.setSize
import com.simple.coreapp.ui.view.setTextStyle
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setText
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.extentions.observeQueue
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.event.sendEvent
import com.simple.image.setImage
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Id
import com.simple.phonetics.Param
import com.simple.phonetics.databinding.DialogListBinding
import com.simple.phonetics.databinding.LayoutActionConfirmGameBinding
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.fragments.BaseActionFragment
import com.simple.phonetics.ui.game.GameConfigViewModel
import com.simple.phonetics.utils.exts.createFlexboxLayoutManager
import com.simple.phonetics.utils.exts.getOrTransparent

class GameConfigFragment : BaseActionFragment<LayoutActionConfirmGameBinding, DialogListBinding, GameConfigViewModel>() {

    private var result: Int = 0

    override val viewModel: GameConfigViewModel by lazy {
        getViewModel(requireActivity(), GameConfigViewModel::class)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logAnalytics("game_config_show")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAction()
        setupRecyclerView()

        observeData()
    }

    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): DialogListBinding {

        return DialogListBinding.inflate(inflater, container, false)
    }

    override fun createBindingAction(): LayoutActionConfirmGameBinding {

        return LayoutActionConfirmGameBinding.inflate(LayoutInflater.from(requireContext()))
    }

    override fun onDestroy() {
        super.onDestroy()

        sendEvent(arguments?.getString(Param.KEY_REQUEST) ?: Param.KEY_REQUEST, result)
    }

    private fun setupAction() {

        val binding = bindingAction ?: return

        binding.root.setDebouncedClickListener {

            if (viewModel.resourceSelected.value == null) return@setDebouncedClickListener

            result = 1
            dismiss()
        }
    }

    private fun setupRecyclerView() {

        val binding = binding ?: return

        val clickTextAdapter = ClickTextAdapter { _, item ->

            if (item.id.startsWith(Id.RESOURCE, true)) {

                viewModel.updateResource(item.data.asObject<String>())
            }
        }

        MultiAdapter(clickTextAdapter).apply {

            binding.recyclerView.adapter = this
            binding.recyclerView.itemAnimator = null

            val layoutManager = createFlexboxLayoutManager(context = context)
            layoutManager.flexDirection = FlexDirection.ROW
            layoutManager.justifyContent = JustifyContent.FLEX_START
            binding.recyclerView.layoutManager = layoutManager
        }
    }

    private fun observeData() = with(viewModel) {

        theme.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe
            val bindingAction = bindingAction ?: return@observe

            binding.vAnchor.setBackground(Background(backgroundColor = it.getOrTransparent("colorDivider"), cornerRadius = DP.DP_100))

            binding.root.setBackground(Background(backgroundColor = it.getOrTransparent("colorBackground"), cornerRadius_TL = DP.DP_16, cornerRadius_TR = DP.DP_16))
            bindingAction.root.setBackground(Background(backgroundColor = it.getOrTransparent("colorBackground"), cornerRadius_TL = DP.DP_16, cornerRadius_TR = DP.DP_16))
        }

        buttonInfo.observe(viewLifecycleOwner) { item ->

            val binding = ItemTextBinding.bind(bindingAction?.frameActionConfirmGame ?: return@observe)

            binding.root.setSize(item.size)
            binding.root.setMargin(item.margin)
            binding.root.setPadding(item.padding)
            binding.root.setBackground(item.background)

            binding.tvTitle.setText(item.text)
            binding.tvTitle.setTextStyle(item.textStyle)
            binding.tvTitle.setSize(item.textSize)
            binding.tvTitle.setMargin(item.textMargin)
            binding.tvTitle.setPadding(item.textPadding)
            binding.tvTitle.setBackground(item.textBackground)

            binding.ivLeft.setVisible(item.imageLeft != null)
            binding.ivLeft.setImage(item.imageLeft ?: return@observe)
            binding.ivLeft.setSize(item.imageLeftSize)
            binding.ivLeft.setMargin(item.imageLeftMargin)
            binding.ivLeft.setPadding(item.imageLeftPadding)
            binding.ivLeft.setBackground(item.imageLeftBackground)

            binding.ivRight.setVisible(item.imageRight != null)
            binding.ivRight.setImage(item.imageRight ?: return@observe)
            binding.ivRight.setSize(item.imageRightSize)
            binding.ivRight.setMargin(item.imageRightMargin)
            binding.ivRight.setPadding(item.imageRightPadding)
            binding.ivRight.setBackground(item.imageRightBackground)
        }

        viewItemList.observeQueue(viewLifecycleOwner) {

            val binding = binding ?: return@observeQueue

            binding.recyclerView.submitListAwait(it)
        }
    }
}

@Deeplink
class ConfigDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return DeeplinkManager.GAME_CONFIG
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is MainActivity) return false

        val fragment = GameConfigFragment()
        fragment.arguments = bundleOf(*extras?.toList().orEmpty().toTypedArray())
        fragment.showOrAwaitDismiss(componentCallbacks.supportFragmentManager, tag = "")

        return true
    }
}