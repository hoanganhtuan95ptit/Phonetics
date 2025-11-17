package com.simple.phonetics.ui.home.services.input

import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.simple.autobind.annotation.AutoBind
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.ext.resize
import com.simple.phonetics.R
import com.simple.phonetics.databinding.FragmentHomeBinding
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.phonetics.ui.home.services.HomeService
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.simple.phonetics.utils.exts.colorBackgroundVariant
import com.simple.phonetics.utils.exts.listenerOnHeightChange
import com.unknown.coroutines.launchCollect
import com.unknown.theme.utils.exts.colorSurface
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.math.absoluteValue
import kotlin.math.min

@AutoBind(HomeFragment::class)
class InputHomeService : HomeService {

    private lateinit var viewModel: InputHomeViewModel

    private lateinit var homeViewModel: HomeViewModel

    override fun setup(homeFragment: HomeFragment) {

        viewModel = homeFragment.viewModels<InputHomeViewModel>().value
        homeViewModel = homeFragment.viewModel

        setupBack(homeFragment)
        setupScroll(homeFragment)

        observeData(fragment = homeFragment)
        observeHomeData(fragment = homeFragment)
    }

    private fun setupBack(homeFragment: HomeFragment) {

        homeFragment.activity?.onBackPressedDispatcher?.addCallback(homeFragment.viewLifecycleOwner, object : OnBackPressedCallback(true) {

            override fun handleOnBackPressed() {

                val binding = homeFragment.binding ?: return

                if (binding.etText.text.toString().isNotEmpty()) if (binding.vBackgroundInput.translationY.absoluteValue > binding.vBackgroundInput.height / 2) {

                    binding.recyclerView.smoothScrollToPosition(0)
                } else {

                    binding.etText.setText("")
                } else {

                    homeFragment.activity?.finish()
                }
            }
        })
    }

    private fun setupScroll(homeFragment: HomeFragment) {

        val binding = homeFragment.binding ?: return

        val headerTopFlow = combine(
            binding.statusBar.listenerOnHeightChange(),
            binding.frameHeader.listenerOnHeightChange(),
        ) {

            it.sum()
        }

        val inputHeightFlow = combine(
            headerTopFlow,
            binding.etText.listenerOnHeightChange(),
            binding.frameControl.listenerOnHeightChange()
        ) {

            it.sum()
        }

        val inputHeightIncludeFilterFlow = combine(
            inputHeightFlow,
            binding.recFilter.listenerOnHeightChange(),
        ) {

            it.sum()
        }

        val scrollFlow = channelFlow {

            val listener = object : RecyclerView.OnScrollListener() {

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

                    trySend(dy)
                }
            }

            binding.recyclerView.addOnScrollListener(listener)

            trySend(0f)

            awaitClose {

                binding.recyclerView.removeOnScrollListener(listener)
            }
        }

        val inputSpaceViewFlow = channelFlow<View?> {

            val onGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {

                trySend(binding.recyclerView.findViewById(R.id.frame_home_input))
            }

            binding.recyclerView.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)

            trySend(binding.recyclerView.findViewById(R.id.frame_home_input))

            awaitClose {

                binding.recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
            }
        }

        headerTopFlow.distinctUntilChanged().launchCollect(homeFragment.viewLifecycleOwner){

        }

        inputHeightFlow.distinctUntilChanged().launchCollect(homeFragment.viewLifecycleOwner) {

            viewModel.updateInputHeight(height = it)
        }

        inputHeightIncludeFilterFlow.distinctUntilChanged().launchCollect(homeFragment.viewLifecycleOwner) {

            homeViewModel.updateInputHeight(height = it)
            viewModel.updateInputHeightIncludeFilter(height = it)
        }

        scrollFlow.flatMapLatest {

            if (homeViewModel.viewItemList.value.isNullOrEmpty()) {
                flowOf(0)
            } else {
                inputSpaceViewFlow.distinctUntilChanged().map { it?.locationY() ?: -inputHeightIncludeFilterFlow.first() }
            }
        }.distinctUntilChanged().launchCollect(homeFragment) {

            updatePosition(binding = binding, y = it, headerTop = 0)
        }
    }

    private fun observeData(fragment: HomeFragment) = with(viewModel) {

        theme.collectWithLockTransitionUntilData(fragment = fragment, tag = "THEME_INPUT") {

            val binding = fragment.binding ?: return@collectWithLockTransitionUntilData

            binding.statusBar.setBackgroundColor(it.colorSurface)
            binding.vBackgroundHeader.setBackgroundColor(it.colorSurface)

            binding.recFilter.setBackgroundColor(it.colorBackgroundVariant)
        }

        inputInfo.collectWithLockTransitionUntilData(fragment = fragment, tag = "INPUT_INFO") {

            val binding = fragment.binding ?: return@collectWithLockTransitionUntilData

            binding.vBackgroundInput.resize(height = it.inputHeight)
            binding.vBackgroundInput.setBackground(background = it.background)

//            binding.ivBackgroundInput.setImage(it.imageBackground, ColorFilterTransformation(it.imageBackgroundFilter), BlurTransformation())
        }
    }

    private fun observeHomeData(fragment: HomeFragment) = with(homeViewModel) {

    }

    private fun updatePosition(binding: FragmentHomeBinding, y: Int = 0, headerTop: Int) {

        var alpha = y * 1f / (binding.statusBar.height + binding.frameHeader.height)
        alpha = min(1f, alpha.absoluteValue)
        if (alpha.isNaN()) alpha = 0f

        binding.statusBar.alpha = alpha
        binding.vBackgroundHeader.alpha = alpha

//        binding.statusBar.elevation = alpha * DP.DP_24
//        binding.frameHeader.elevation = alpha * DP.DP_24

        binding.etText.translationY = y + 0f

//        if (binding.recFilter.top + binding.recFilter.translationY <= headerTop) {

            binding.recFilter.translationY = y + 0f
//        } else {
//
//            binding.recFilter.translationY = binding.recFilter.top - headerTop + 0f
//        }

        binding.frameControl.translationY = y + 0f
        binding.vBackgroundInput.translationY = y + 0f
        binding.ivBackgroundInput.translationY = y + 0f

        Log.d("tuanha", "updatePosition: ${binding.recFilter.top}")

    }

    private fun View.locationY(): Int {

        val location = IntArray(2)
        getLocationOnScreen(location)
        return location[1]
    }
}