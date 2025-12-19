package com.simple.phonetics.ui.language

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updatePadding
import androidx.lifecycle.asFlow
import androidx.recyclerview.widget.LinearLayoutManager
import com.simple.adapter.MultiAdapter
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.doOnChangeHeightStatusAndHeightNavigation
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setInvisible
import com.simple.coreapp.utils.ext.setText
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.ext.with
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.deeplink.sendDeeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.ErrorCode
import com.simple.phonetics.Param
import com.simple.phonetics.databinding.FragmentListHeaderVerticalBinding
import com.simple.phonetics.ui.base.fragments.BaseFragment
import com.simple.phonetics.ui.language.adapters.LanguageAdapter
import com.simple.phonetics.utils.exts.awaitResume
import com.simple.phonetics.utils.exts.collectWithLockTransitionIfCached
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.simple.phonetics.utils.exts.colorErrorVariant
import com.simple.phonetics.utils.exts.colorOnErrorVariant
import com.simple.phonetics.utils.exts.replace
import com.simple.phonetics.utils.exts.submitListAwaitV2
import com.simple.state.ResultState
import com.unknown.coroutines.launchCollect
import com.unknown.theme.utils.exts.colorBackground
import com.unknown.theme.utils.exts.colorOnBackground

class LanguageFragment : BaseFragment<FragmentListHeaderVerticalBinding, LanguageViewModel>() {

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

        val languageAdapter = LanguageAdapter { _, item ->

            viewModel.updateLanguageSelected(item.data)
        }

        MultiAdapter(languageAdapter).apply {

            binding.recyclerView.adapter = this
            binding.recyclerView.itemAnimator = null

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

            binding.frameHeader.tvTitle.setText(it.title)
            binding.frameHeader.tvMessage.setText(it.message)
        }

        buttonInfo.collectWithLockTransitionUntilData(fragment = fragment, tag = "BUTTON_INFO") {

            val binding = binding?.frameConfirm ?: return@collectWithLockTransitionUntilData

            binding.btnConfirm.setText(it.text)
            binding.progress.setVisible(it.isShowLoading)

            binding.root.isClickable = it.isClickable
            binding.root.setBackground(it.background)
        }

        languageViewItemList.collectWithLockTransitionIfCached(fragment = fragment, tag = "VIEW_ITEM_LIST") { data, isFirst ->

            val binding = binding ?: return@collectWithLockTransitionIfCached

            binding.recyclerView.submitListAwaitV2(viewItemList = data, isFromCache = isFirst)
        }

        changeLanguageState.asFlow().launchCollect(viewLifecycleOwner) {

            binding ?: return@launchCollect

            fragment.awaitResume()

            if (it is ResultState.Success) {

                handleSuccess()
            } else if (it is ResultState.Failed) {

                handleFailed(it)
            }
        }
    }

    private fun handleSuccess() {

        val binding = binding ?: return

        if (binding.root.transitionName == null) {

            binding.root.transitionName = "select_language"
        }

        if (arguments?.containsKey(Param.ROOT_TRANSITION_NAME) != true) sendDeeplink(
            deepLink = DeeplinkManager.PHONETICS,
            extras = mapOf(
                Param.ROOT_TRANSITION_NAME to "1"
            ),
            sharedElement = mapOf(
                binding.root.transitionName to binding.root
            )
        ) else {

            activity?.supportFragmentManager?.popBackStack()
        }
    }

    private fun handleFailed(it: ResultState.Failed) {

        val theme = viewModel.theme.value ?: return
        val translate = viewModel.translate.value ?: return

        val message = if (it.cause is java.io.IOException && translate.containsKey("message_error_io_exception")) {
            translate["message_error_io_exception"]
        } else {
            it.cause.message
        }

        val extras = mapOf(
            com.simple.coreapp.Param.MESSAGE to message.orEmpty().with(ForegroundColor(theme.colorOnErrorVariant)),
            com.simple.coreapp.Param.BACKGROUND to Background(
                backgroundColor = theme.colorErrorVariant,
                cornerRadius = DP.DP_16,
            )
        )

        sendDeeplink(
            deepLink = DeeplinkManager.TOAST + "?code:${ErrorCode.NOT_INTERNET}",
            extras = extras
        )
    }
}

@Deeplink
class LanguageDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return DeeplinkManager.LANGUAGE
    }

    override suspend fun navigation(activity: AppCompatActivity, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        activity.supportFragmentManager.replace(fragment = LanguageFragment(), extras = extras, sharedElement = sharedElement)

        return true
    }
}
