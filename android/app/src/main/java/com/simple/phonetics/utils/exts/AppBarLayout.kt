package com.simple.phonetics.utils.exts

import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import kotlin.math.abs

enum class AppBarState {
    EXPANDED, COLLAPSED, SCROLLING
}

fun AppBarLayout.setCurrentOffset(offset: Int) {
    val params = this.layoutParams as? CoordinatorLayout.LayoutParams
    val behavior = params?.behavior as? AppBarLayout.Behavior
    behavior?.setTopAndBottomOffset(offset)
}

fun AppBarLayout.getCurrentOffset(): Int {
    val params = this.layoutParams as? CoordinatorLayout.LayoutParams
    val behavior = params?.behavior as? AppBarLayout.Behavior
    return behavior?.topAndBottomOffset ?: 0
}

fun AppBarLayout.getState(): AppBarState {

    val offset = getCurrentOffset()

    return when {
        offset == 0 -> AppBarState.EXPANDED // Mở hoàn toàn
        abs(offset) >= totalScrollRange -> AppBarState.COLLAPSED // Thu nhỏ hoàn toàn
        else -> AppBarState.SCROLLING // Đang cuộn
    }
}