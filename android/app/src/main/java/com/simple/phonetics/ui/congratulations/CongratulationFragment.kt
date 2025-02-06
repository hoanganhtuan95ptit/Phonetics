package com.simple.phonetics.ui.congratulations

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.simple.coreapp.ui.base.dialogs.sheet.BaseViewBindingSheetFragment
import com.simple.phonetics.databinding.DialogCongratulationBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CongratulationFragment : BaseViewBindingSheetFragment<DialogCongratulationBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        dialog?.window?.setDimAmount(0f)

        viewLifecycleOwner.lifecycleScope.launch {

            delay(3000)
            dismiss()
        }
    }
}