package com.simple.phonetics.ui.base.fragments.list

import android.content.ComponentCallbacks
import androidx.lifecycle.asFlow
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.JustifyContent
import com.hoanganhtuan95ptit.autobind.annotation.AutoBind
import com.simple.adapter.MultiAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.base.activities.BaseViewModelActivity
import com.simple.coreapp.ui.base.fragments.BaseViewModelFragment
import com.simple.coreapp.ui.base.fragments.transition.TransitionFragment
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.R
import com.simple.phonetics.databinding.FragmentListHeaderHorizontalBinding
import com.simple.phonetics.databinding.FragmentListHeaderVerticalBinding
import com.simple.phonetics.ui.base.fragments.exts.binding
import com.simple.phonetics.ui.base.fragments.exts.context
import com.simple.phonetics.ui.base.fragments.exts.viewLifecycleOwner
import com.simple.phonetics.ui.base.screen.ComponentService
import com.simple.phonetics.utils.exts.collectWithLockTransitionIfCached
import com.simple.phonetics.utils.exts.createFlexboxLayoutManager
import com.simple.phonetics.utils.exts.submitListAwaitV2

interface ListView {

    fun getRecyclerView(): RecyclerView? {

        return when (val binding = binding) {

            is FragmentListHeaderVerticalBinding -> {
                binding.recyclerView
            }

            is FragmentListHeaderHorizontalBinding -> {
                binding.recyclerView
            }

            else -> {
                binding?.root?.findViewById(R.id.recycler_view)
            }
        }
    }
}

@AutoBind(ComponentService::class)
class ListService : ComponentService {

    override fun setup(component: ComponentCallbacks) {

        if (component !is ListView || component.getRecyclerView() == null) return

        setupRecyclerView(component)

        observeData(component)
    }

    private fun setupRecyclerView(component: ListView) {

        val context = component.context ?: return
        val recyclerView = component.getRecyclerView() ?: return

        val layoutManager = createFlexboxLayoutManager(context = context) {

            logCrashlytics(
                event = "IPA_DETAIL",
                throwable = it,
                "VIEW_ITEM_SIZE" to "${component.listViewModel?.viewItemList?.value?.size}"
            )
        }
        layoutManager.flexDirection = FlexDirection.ROW
        layoutManager.justifyContent = JustifyContent.FLEX_START


        recyclerView.adapter = MultiAdapter()
        recyclerView.itemAnimator = null
        recyclerView.layoutManager = layoutManager
    }

    private fun observeData(component: ListView) = with(component.listViewModel) {

        val listViewModel = this ?: return@with
        val viewLifecycleOwner = component.viewLifecycleOwner ?: return@with

        if (component is TransitionFragment<*, *>) listViewModel.viewItemList.collectWithLockTransitionIfCached(fragment = component as TransitionFragment<*, *>, tag = "VIEW_ITEM_LIST") { data, isFirst ->

            bindingData(viewItemList = data, isFirst = isFirst, recyclerView = component.getRecyclerView() ?: return@collectWithLockTransitionIfCached)
        } else listViewModel.viewItemList.asFlow().launchCollect(viewLifecycleOwner) {

            bindingData(viewItemList = it, recyclerView = component.getRecyclerView() ?: return@launchCollect)
        }
    }

    private suspend fun bindingData(viewItemList: List<ViewItem>, isFirst: Boolean = false, recyclerView: RecyclerView) {

        recyclerView.submitListAwaitV2(viewItemList = viewItemList, isFirst = isFirst)
    }
}

private val ListView.listViewModel: ListViewModel?
    get() = when (this) {

        is BaseViewModelActivity<*, *> -> {
            viewModel as? ListViewModel
        }

        is BaseViewModelFragment<*, *> -> {
            viewModel as? ListViewModel
        }

        else -> {
            null
        }
    }
