package com.simple.phonetics.ui.base.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.simple.coreapp.ui.base.dialogs.sheet.BaseViewModelSheetFragment

abstract class BaseSheetFragment<T : ViewBinding, VM : BaseViewModel>() : BaseViewModelSheetFragment<T, VM>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val dialog = (dialog as? BottomSheetDialog) ?: return

        dialog.window?.navigationBarColor = Color.TRANSPARENT

        super.onViewCreated(view, savedInstanceState)
    }
}