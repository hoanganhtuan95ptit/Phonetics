package com.simple.phonetics.ui.language

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.lifecycle.asFlow
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionSet
import com.simple.adapter.MultiAdapter
import com.simple.coreapp.ui.base.fragments.transition.TransitionFragment
import com.simple.coreapp.ui.view.round.setBackground
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.doOnChangeHeightStatusAndHeightNavigation
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setInvisible
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.extentions.beginTransitionAwait
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.phonetics.Deeplink
import com.simple.phonetics.Param
import com.simple.phonetics.R
import com.simple.phonetics.databinding.FragmentLanguageBinding
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.language.adapters.LanguageAdapter
import com.simple.phonetics.ui.language.adapters.LanguageLoadingAdapter
import com.simple.phonetics.ui.language.adapters.LanguageStateAdapter
import com.simple.phonetics.utils.DeeplinkHandler
import com.simple.phonetics.utils.exts.setImageDrawable
import com.simple.phonetics.utils.sendDeeplink
import com.simple.state.ResultState
import com.simple.state.isSuccess

class LanguageFragment : TransitionFragment<FragmentLanguageBinding, LanguageViewModel>() {

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

        binding.icBack.setInvisible(isFirst)
        binding.icBack.isClickable = !isFirst
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

        adapter = MultiAdapter(languageAdapter, LanguageStateAdapter(), LanguageLoadingAdapter()).apply {

            setRecyclerView(binding.recyclerView)
        }
    }

    private fun observeData() = with(viewModel) {

        lockTransition(TAG.THEME.name, TAG.HEADER.name, TAG.BUTTON_INFO.name)

        theme.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.icBack.setImageDrawable(requireActivity(), R.drawable.ic_arrow_left_on_surface, it.colorOnBackground)
            binding.root.setBackgroundColor(it.colorBackground)

            unlockTransition(TAG.THEME.name)
        }

        headerInfo.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.tvTitle.text = it.title
            binding.tvMessage.text = it.message

            unlockTransition(TAG.HEADER.name)
        }

        buttonInfo.asFlow().launchCollect(viewLifecycleOwner) {

            val binding = binding?.frameConfirm ?: return@launchCollect

            binding.btnConfirm.text = it.text
            binding.progress.setVisible(it.isShowLoading)

            binding.root.isClickable = it.isClickable
            binding.root.delegate.setBackground(it.background)

            unlockTransition(TAG.BUTTON_INFO.name)
        }

        languageViewItemList.asFlow().launchCollect(viewLifecycleOwner) { data ->

            val binding = binding ?: return@launchCollect

            awaitTransition()

            binding.recyclerView.submitListAwait(data)

            val transition = TransitionSet().addTransition(ChangeBounds().setDuration(350)).addTransition(Fade().setDuration(350))
            binding.recyclerView.beginTransitionAwait(transition)
        }

        changeLanguageState.asFlow().launchCollect(viewLifecycleOwner) {

            val binding = binding ?: return@launchCollect

            if (it.isSuccess()) if (arguments?.containsKey(Param.ROOT_TRANSITION_NAME) != true) sendDeeplink(
                Deeplink.PHONETICS,
                extras = bundleOf(
                    Param.ROOT_TRANSITION_NAME to ""
                ),
                sharedElement = mapOf(
                    binding.root.transitionName to binding.root
                )
            ) else {

                activity?.supportFragmentManager?.popBackStack()
            }

            if (it is ResultState.Failed) {

                showToast(it.cause.message.orEmpty(), it)
            }
        }
    }

    private enum class TAG {

        THEME,
        HEADER,
        BUTTON_INFO,
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
