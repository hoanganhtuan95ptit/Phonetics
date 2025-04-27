package com.simple.phonetics.ui.home.view.phonetic

import androidx.lifecycle.asFlow
import com.google.auto.service.AutoService
import com.simple.adapter.MultiAdapter
import com.simple.adapter.base.BaseBindingViewHolder
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.databinding.ItemTextBinding
import com.simple.coreapp.ui.adapters.texts.NoneTextViewItem
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.phonetics.ui.home.view.HomeView
import org.koin.androidx.viewmodel.ext.android.viewModel

@AutoService(HomeView::class)
class PhoneticHomeView : HomeView {

    override fun setup(fragment: HomeFragment) {

        val viewModel: HomeViewModel by fragment.viewModel()

        val phoneticHomeViewModel: PhoneticHomeViewModel by fragment.viewModel()

        phoneticHomeViewModel.viewItemList.observe(fragment.viewLifecycleOwner) {

            viewModel.updateTypeViewItemList(type = -1, it)
        }

        phoneticHomeViewModel.pairViewList.asFlow().launchCollect(fragment.viewLifecycleOwner) {

            val binding = fragment.binding?.recyclerView ?: return@launchCollect

            val adapter = binding.adapter.asObjectOrNull<MultiAdapter>() ?: return@launchCollect

            adapter.currentList.forEachIndexed { index, viewItem ->

                if (viewItem !is NoneTextViewItem || !viewItem.id.startsWith("phonetic_")) return@forEachIndexed

                val pair = it.firstOrNull { pair -> viewItem.id.endsWith(pair.first, true) }

                if (pair == null) return@forEachIndexed

                binding.findViewHolderForAdapterPosition(index)
                    ?.asObjectOrNull<BaseBindingViewHolder<*>>()
                    ?.binding
                    .asObjectOrNull<ItemTextBinding>()
                    ?.tvTitle?.text = pair.second
            }
        }
    }
}