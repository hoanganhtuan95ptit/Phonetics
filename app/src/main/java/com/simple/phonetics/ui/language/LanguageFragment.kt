package com.simple.phonetics.ui.language

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.view.updatePadding
import androidx.lifecycle.asFlow
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionSet
import com.simple.adapter.MultiAdapter
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.doOnChangeHeightStatusAndHeightNavigation
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.extentions.beginTransitionAwait
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.phonetics.Deeplink
import com.simple.phonetics.R
import com.simple.phonetics.databinding.FragmentLanguageBinding
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.TransitionFragment
import com.simple.phonetics.ui.language.adapters.LanguageAdapter
import com.simple.phonetics.utils.DeeplinkHandler
import com.simple.state.isStart

class LanguageFragment : TransitionFragment<FragmentLanguageBinding, LanguageViewModel>() {

    private var adapter by autoCleared<MultiAdapter>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.root.doOnChangeHeightStatusAndHeightNavigation(viewLifecycleOwner) { heightStatusBar: Int, heightNavigationBar: Int ->

            binding.root.updatePadding(top = heightStatusBar, bottom = heightNavigationBar)
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

        message.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.tvMessage.text = it
        }

        languageViewItemList.asFlow().launchCollect(viewLifecycleOwner) {

            val binding = binding ?: return@launchCollect

            binding.recyclerView.submitListAwait(it)

            val transition = TransitionSet().addTransition(ChangeBounds().setDuration(350)).addTransition(Fade().setDuration(350))
            binding.recyclerView.beginTransitionAwait(transition)
        }

        changeLanguageState.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.frameConfirm.progress.setVisible(it.isStart())
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

        fragmentTransaction.replace(R.id.fragment_container, fragment, "")
            .addToBackStack("")
            .commit()

        return true
    }
}
