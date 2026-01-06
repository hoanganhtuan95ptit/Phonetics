package com.simple.phonetics.ui.home.services.background

import androidx.core.view.updatePadding
import com.google.android.material.appbar.AppBarLayout
import com.simple.autobind.annotation.AutoBind
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.getStatusBarHeight
import com.simple.coreapp.utils.ext.resize
import com.simple.coreapp.utils.extentions.getHeightStatusBarOrNull
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.services.HomeService
import com.simple.phonetics.utils.exts.listenerWindowInsetsChangeAsync
import com.unknown.coroutines.launchCollect
import com.unknown.theme.utils.exts.colorBackground
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlin.math.absoluteValue


@AutoBind(HomeFragment::class)
class BackgroundHomeService : HomeService {

    override fun setup(homeFragment: HomeFragment) {

        val statusBarHeightDefault = getStatusBarHeight(homeFragment.requireContext())

        homeFragment.viewModel.themeFlow.launchCollect(homeFragment.viewLifecycleOwner) {

            val binding = homeFragment.binding ?: return@launchCollect

            binding.toolbarBackground.setBackgroundColor(it.colorBackground)
        }


        val binding = homeFragment.binding ?: return

        binding.ivBackground.post {

            binding.ivBackground.translationX = binding.ivBackground.width * 1.5f / 5
            binding.ivBackground.translationY = -binding.ivBackground.height * 1f / 5
        }

        binding.root.listenerWindowInsetsChangeAsync().map { it.getHeightStatusBarOrNull() ?: statusBarHeightDefault }.launchCollect(homeFragment.viewLifecycleOwner) {

            binding.toolbar.resize(height = it + DP.DP_56)

            binding.tvTitle.translationY = it / 2f
            binding.ivLanguage.translationY = it / 2f

            binding.etText.updatePadding(top = it + DP.DP_56)
        }

        binding.appBarLayout.listenerOffsetChangesAsync().launchCollect(homeFragment.viewLifecycleOwner) { verticalOffset ->

            val totalRange = binding.appBarLayout.totalScrollRange.toFloat()
            if (totalRange == 0f) return@launchCollect

            val percentage = verticalOffset.absoluteValue / totalRange

            binding.toolbarBackground.alpha = (percentage * 3.0f).coerceIn(0f, 1f)
        }
    }

    private fun AppBarLayout.listenerOffsetChangesAsync(): Flow<Int> = callbackFlow {

        val listener = AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->

            trySend(verticalOffset)
        }

        addOnOffsetChangedListener(listener)

        awaitClose {

            removeOnOffsetChangedListener(listener)
        }
    }
}