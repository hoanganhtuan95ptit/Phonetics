package com.simple.phonetics.ui.base.fragments.button

import android.content.ComponentCallbacks
import android.view.View
import android.widget.TextView
import androidx.lifecycle.asFlow
import com.hoanganhtuan95ptit.autobind.annotation.AutoBind
import com.simple.coreapp.ui.base.activities.BaseViewModelActivity
import com.simple.coreapp.ui.base.fragments.BaseViewModelFragment
import com.simple.coreapp.ui.base.fragments.transition.TransitionFragment
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.setText
import com.simple.coreapp.utils.ext.setVisible
import com.simple.phonetics.databinding.FragmentListHeaderHorizontalBinding
import com.simple.phonetics.databinding.FragmentListHeaderVerticalBinding
import com.simple.phonetics.ui.base.fragments.exts.binding
import com.simple.phonetics.ui.base.fragments.exts.viewLifecycleOwner
import com.simple.phonetics.ui.base.screen.ComponentService
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData

data class ButtonViewBinding(
    val btnConfirm: TextView,

    val root: View? = null,
    val progress: View? = null
)

interface ButtonView {

    fun getButtonViewBinding(): ButtonViewBinding? {

        return when (val binding = binding) {

            is FragmentListHeaderVerticalBinding -> binding.frameConfirm.let {
                ButtonViewBinding(btnConfirm = it.btnConfirm, progress = it.progress, root = it.root)
            }

            is FragmentListHeaderHorizontalBinding -> binding.frameConfirm.let {
                ButtonViewBinding(btnConfirm = it.btnConfirm, progress = it.progress, root = it.root)
            }

            else -> {
                null
            }
        }
    }
}

@AutoBind(ComponentService::class)
class ButtonService : ComponentService {

    override fun setup(component: ComponentCallbacks) {

        if (component !is ButtonView) return

        observeData(component)
    }

    private fun observeData(component: ButtonView) = with(component.buttonViewModel) {

        this ?: return@with
        val viewLifecycleOwner = component.viewLifecycleOwner ?: return@with

        if (component is TransitionFragment<*, *>) buttonInfo.collectWithLockTransitionUntilData(fragment = component, tag = "BUTTON_INFO") {

            bindingData(it, component.getButtonViewBinding() ?: return@collectWithLockTransitionUntilData)
        } else buttonInfo.asFlow().launchCollect(viewLifecycleOwner) {

            bindingData(it, component.getButtonViewBinding() ?: return@launchCollect)
        }
    }

    private fun bindingData(it: ButtonViewModel.ButtonInfo, binding: ButtonViewBinding) {

        binding.btnConfirm.setText(it.text)
        binding.progress?.setVisible(it.isShowLoading)

        binding.root?.isClickable = it.isClickable
        binding.root?.setBackground(it.background)
    }
}

private val ButtonView.buttonViewModel: ButtonViewModel?
    get() = when (this) {

        is BaseViewModelActivity<*, *> -> {
            viewModel as? ButtonViewModel
        }

        is BaseViewModelFragment<*, *> -> {
            viewModel as? ButtonViewModel
        }

        else -> {
            null
        }
    }
