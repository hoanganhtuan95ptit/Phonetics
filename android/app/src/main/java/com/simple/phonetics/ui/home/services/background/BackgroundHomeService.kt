package com.simple.phonetics.ui.home.services.background

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import com.google.android.material.appbar.AppBarLayout
import com.simple.autobind.annotation.AutoBind
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.resize
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.services.HomeService
import com.simple.phonetics.utils.exts.collectWithLockTransitionIfCached
import com.unknown.coroutines.launchCollect
import com.unknown.size.uitls.exts.statusBarHeight
import com.unknown.theme.utils.exts.colorBackground
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.absoluteValue


@AutoBind(HomeFragment::class)
class BackgroundHomeService : HomeService {

    override fun setup(homeFragment: HomeFragment) {

        val backgroundHomeViewModel: BackgroundHomeViewModel by homeFragment.viewModels()

        backgroundHomeViewModel.sizeFlow.collectWithLockTransitionIfCached(fragment = homeFragment, tag = "BACKGROUND_RESIZE") { size, isFromCache ->

            val binding = homeFragment.binding ?: return@collectWithLockTransitionIfCached

            val statusBarHeight = size.statusBarHeight

            binding.toolbar.resize(height = statusBarHeight + DP.DP_56)

            binding.tvTitle.translationY = statusBarHeight / 2f
            binding.ivLanguage.translationY = statusBarHeight / 2f

            binding.etText.updatePadding(top = statusBarHeight + DP.DP_56)
        }

        backgroundHomeViewModel.themeFlow.collectWithLockTransitionIfCached(fragment = homeFragment, tag = "BACKGROUND_THEME") { theme, isFromCache ->

            val binding = homeFragment.binding ?: return@collectWithLockTransitionIfCached

            binding.toolbarBackground.setBackgroundColor(theme.colorBackground)
        }

        backgroundHomeViewModel.backgroundAlpha.collectWithLockTransitionIfCached(fragment = homeFragment, tag = "BACKGROUND_ALPHA") { alpha, isFromCache ->

            val binding = homeFragment.binding ?: return@collectWithLockTransitionIfCached

            binding.toolbarBackground.alpha = alpha
        }


        val binding = homeFragment.binding ?: return

        binding.collapsingToolbarLayout.outlineProvider = object : ViewOutlineProvider() {

            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, DP.DP_16 * 1f)
            }
        }
        binding.collapsingToolbarLayout.clipToOutline = true

        binding.appBarLayout.listenerOffsetChangesAsync().launchCollect(homeFragment.viewLifecycleOwner) { verticalOffset ->

            val totalRange = binding.appBarLayout.totalScrollRange.toFloat()
            if (totalRange == 0f) return@launchCollect

            val percentage = verticalOffset.absoluteValue / totalRange

            backgroundHomeViewModel.updateBackgroundAlpha(alpha = (percentage * 3.0f).coerceIn(0f, 1f))
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