package com.simple.phonetics.ui.language

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.view.updatePadding
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionSet
import com.simple.adapter.MultiAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.doOnChangeHeightStatusAndHeightNavigation
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.extentions.beginTransitionAwait
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.phonetics.Deeplink
import com.simple.phonetics.Param
import com.simple.phonetics.R
import com.simple.phonetics.databinding.FragmentLanguageBinding
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.TransitionFragment
import com.simple.phonetics.ui.language.adapters.LanguageAdapter
import com.simple.phonetics.utils.DeeplinkHandler
import com.simple.phonetics.utils.exts.launchCollect
import com.simple.phonetics.utils.sendDeeplink
import com.simple.state.ResultState
import com.simple.state.isStart
import com.simple.state.isSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LanguageFragment : TransitionFragment<FragmentLanguageBinding, LanguageViewModel>() {

    private var adapter by autoCleared<MultiAdapter>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.root.doOnChangeHeightStatusAndHeightNavigation(viewLifecycleOwner) { heightStatusBar: Int, heightNavigationBar: Int ->

            binding.root.updatePadding(top = heightStatusBar, bottom = heightNavigationBar)
        }

        binding.icBack.setDebouncedClickListener {

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
        }

        adapter = MultiAdapter(languageAdapter).apply {

            setRecyclerView(binding.recyclerView)
        }
    }

    private fun observeData() = with(viewModel) {

        lockTransition(TAG_MESSAGE, TAG_BUTTON_INFO, TAG_LANGUAGE_VIEW_ITEM_LIST_EVENT)

        message.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.tvMessage.text = it

            unlockTransition(TAG_MESSAGE)
        }

        buttonInfo.asFlow().launchCollect(viewLifecycleOwner) {

            val binding = binding?.frameConfirm ?: return@launchCollect

            binding.btnConfirm.text = it.text
            binding.progress.setVisible(it.isShowLoading)

            binding.root.isSelected = it.isSelected
            binding.btnConfirm.isSelected = it.isSelected

            binding.root.isClickable = it.isClickable

            unlockTransition(TAG_BUTTON_INFO)
        }

        languageViewItemListEvent.launchCollect(viewLifecycleOwner) { isFromCache, event ->

            val binding = binding ?: return@launchCollect

            if (event == null) {

                unlockTransition(TAG_LANGUAGE_VIEW_ITEM_LIST_EVENT)
                return@launchCollect
            }

            val data = if (isFromCache) {
                event.peekContent()
            } else {
                event.getContentIfNotHandled() ?: return@launchCollect
            }

            if (!isFromCache) {

                viewModel.awaitTransition()
            } else {

                unlockTransition(TAG_LANGUAGE_VIEW_ITEM_LIST_EVENT)
            }

            binding.recyclerView.submitListAwait(data)

            val isNeedAnim = !isFromCache

            if (isNeedAnim) {

                val transition = TransitionSet().addTransition(ChangeBounds().setDuration(350)).addTransition(Fade().setDuration(350))
                binding.recyclerView.beginTransitionAwait(transition)
            }
        }

        changeLanguageState.asFlow().launchCollect(viewLifecycleOwner) {

            val binding = binding ?: return@launchCollect

            binding.frameConfirm.progress.setVisible(it.isStart())

            if (it.isSuccess() && arguments?.getBoolean(Param.FIRST) == true) {

                sendDeeplink(Deeplink.PHONETICS).join()
            } else if (it.isSuccess()) {

                activity?.supportFragmentManager?.popBackStack()
            }

            if (it is ResultState.Failed) {

                showToast(it.cause.message.orEmpty(), it)
            }
        }
    }

    companion object {

        private const val TAG_MESSAGE = "MESSAGE"
        private const val TAG_BUTTON_INFO = "BUTTON_INFO"
        private const val TAG_LANGUAGE_VIEW_ITEM_LIST_EVENT = "LANGUAGE_VIEW_ITEM_LIST_EVENT"
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
            .commit()

        return true
    }
}
