package com.simple.phonetics.ui.home.services.phonetic

import androidx.lifecycle.asFlow
import com.simple.adapter.MultiAdapter
import com.simple.adapter.base.BaseBindingViewHolder
import com.simple.autobind.annotation.AutoBind
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.databinding.ItemTextBinding
import com.simple.coreapp.ui.adapters.texts.NoneTextViewItem
import com.simple.coreapp.utils.ext.setText
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.phonetics.ui.home.services.HomeService
import com.unknown.coroutines.launchCollect
import org.koin.androidx.viewmodel.ext.android.viewModel

@AutoBind(HomeFragment::class)
class PhoneticHomeService : HomeService {

    override fun setup(homeFragment: HomeFragment) {

        val homeViewModel: HomeViewModel by homeFragment.viewModel()

        val viewModel: PhoneticHomeViewModel by homeFragment.viewModel()

        viewModel.viewItemList.observe(homeFragment.viewLifecycleOwner) {

            homeViewModel.updateTypeViewItemList(type = -1, it)
        }

        viewModel.pairViewList.asFlow().launchCollect(homeFragment.viewLifecycleOwner) {

            val binding = homeFragment.binding?.recyclerView ?: return@launchCollect

            val adapter = binding.adapter.asObjectOrNull<MultiAdapter>() ?: return@launchCollect

            adapter.currentList.forEachIndexed { index, viewItem ->

                if (viewItem !is NoneTextViewItem || !viewItem.id.startsWith("phonetic_")) return@forEachIndexed

                val pair = it.firstOrNull { pair -> viewItem.id.endsWith(pair.first, true) }

                if (pair == null) return@forEachIndexed

                binding.findViewHolderForAdapterPosition(index)
                    ?.asObjectOrNull<BaseBindingViewHolder<*>>()
                    ?.binding
                    .asObjectOrNull<ItemTextBinding>()
                    ?.tvTitle?.setText(pair.second)
            }
        }
    }
}