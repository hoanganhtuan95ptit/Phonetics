package com.simple.phonetics.ui.base.fragments.header

import android.app.Activity
import android.content.ComponentCallbacks
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.asFlow
import com.hoanganhtuan95ptit.autobind.annotation.AutoBind
import com.simple.coreapp.ui.base.activities.BaseViewModelActivity
import com.simple.coreapp.ui.base.fragments.BaseViewModelFragment
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setText
import com.simple.phonetics.databinding.FragmentListHeaderHorizontalBinding
import com.simple.phonetics.databinding.FragmentListHeaderVerticalBinding
import com.simple.phonetics.ui.base.fragments.exts.binding
import com.simple.phonetics.ui.base.fragments.exts.viewLifecycleOwner
import com.simple.phonetics.ui.base.screen.ComponentService

data class HeaderViewBinding(
    val tvTitle: TextView,

    val ivBack: ImageView? = null,
    val tvCaption: TextView? = null
)

interface HeaderView {

    fun getHeaderViewBinding(): HeaderViewBinding? {

        return when (val binding = binding) {

            is FragmentListHeaderVerticalBinding -> binding.frameHeader.let {
                HeaderViewBinding(tvTitle = it.tvTitle, tvCaption = it.tvMessage, ivBack = it.icBack)
            }

            is FragmentListHeaderHorizontalBinding -> binding.frameHeader.let {
                HeaderViewBinding(tvTitle = it.tvTitle, tvCaption = null, ivBack = it.icBack)
            }

            else -> {
                null
            }
        }
    }
}

@AutoBind(ComponentService::class)
class HeaderService : ComponentService {

    override fun setup(component: ComponentCallbacks) {

        if (component !is HeaderView) return

        setupBack(component)

        observeData(component)
    }

    private fun setupBack(component: HeaderView) {

        component.getHeaderViewBinding()?.ivBack?.setDebouncedClickListener {

            if (component is Activity) {
                component.finish()
            } else if (component is Fragment) {

                component.activity?.supportFragmentManager?.popBackStack()
            }
        }
    }

    private fun observeData(component: HeaderView) = with(component.headerViewModel) {

        val headerViewModel = this ?: return@with
        val viewLifecycleOwner = component.viewLifecycleOwner ?: return@with

        headerViewModel.headerInfo.asFlow().launchCollect(viewLifecycleOwner) {

            val headerView = component.getHeaderViewBinding() ?: return@launchCollect

            headerView.tvTitle.setText(it.title)
            headerView.tvCaption?.setText(it.message)
        }
    }
}

private val HeaderView.headerViewModel: HeaderViewModel?
    get() = when (this) {

        is BaseViewModelActivity<*, *> -> {
            viewModel as? HeaderViewModel
        }

        is BaseViewModelFragment<*, *> -> {
            viewModel as? HeaderViewModel
        }

        else -> {
            null
        }
    }
