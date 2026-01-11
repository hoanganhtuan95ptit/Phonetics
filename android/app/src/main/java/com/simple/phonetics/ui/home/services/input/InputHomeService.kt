package com.simple.phonetics.ui.home.services.input

import androidx.fragment.app.viewModels
import com.simple.autobind.annotation.AutoBind
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.phonetics.ui.home.services.HomeService
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.unknown.coroutines.launchCollect
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

@AutoBind(HomeFragment::class)
class InputHomeService : HomeService {

    private lateinit var viewModel: InputHomeViewModel

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var configViewModel: ConfigViewModel


    override fun setup(homeFragment: HomeFragment) {

        homeViewModel = homeFragment.viewModel<HomeViewModel>().value
        configViewModel = homeFragment.activityViewModel<ConfigViewModel>().value

        viewModel = homeFragment.viewModels<InputHomeViewModel>().value

        observeData(fragment = homeFragment)
        observeHomeData(fragment = homeFragment)
    }

    private fun observeData(fragment: HomeFragment) = with(viewModel) {

        textDirection.collectWithLockTransitionUntilData(fragment = fragment, tag = "INPUT_HOME_SERVICE_TEXT_DIRECTION") {

            val binding = fragment.binding ?: return@collectWithLockTransitionUntilData

            binding.etText.textDirection = it
        }
    }

    private fun observeHomeData(fragment: HomeFragment) = with(homeViewModel) {

        isReverseFlow.launchCollect(fragment.viewLifecycleOwner) {

            viewModel.updateReverse(it)
        }
    }
}