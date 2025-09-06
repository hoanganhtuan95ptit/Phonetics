package com.simple.phonetics.ui.base.fragments.exts

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import com.simple.coreapp.ui.base.activities.BaseViewBindingActivity
import com.simple.coreapp.ui.base.dialogs.BaseViewBindingDialogFragment
import com.simple.coreapp.ui.base.dialogs.sheet.BaseViewBindingSheetFragment
import com.simple.coreapp.ui.base.fragments.BaseViewBindingFragment

val Any.context: Context?
    get() = when (this) {

        is Fragment -> {
            context
        }

        is FragmentActivity -> {
            this
        }

        else -> {
            null
        }
    }


val Any.viewLifecycleOwner: LifecycleOwner?
    get() = when (this) {

        is Fragment -> {
            viewLifecycleOwner
        }

        is FragmentActivity -> {
            this
        }

        else -> {
            null
        }
    }

val Any.binding: ViewBinding?
    get() = when (this) {

        is BaseViewBindingActivity<*> -> {
            binding
        }

        is BaseViewBindingFragment<*> -> {
            binding
        }

        is BaseViewBindingSheetFragment<*> -> {
            binding
        }

        is BaseViewBindingDialogFragment<*> -> {
            binding
        }

        else -> {
            null
        }
    }