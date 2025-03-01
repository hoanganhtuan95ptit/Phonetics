package com.simple.phonetics.utils.exts

import com.simple.adapter.ViewItemAdapter
import com.simple.coreapp.ui.adapters.texts.NoneTextAdapter
import com.tuanha.adapter.AdapterManager

fun ListPreviewAdapter() = arrayOf(
    NoneTextAdapter(),
    *AdapterManager.all().filterIsInstance<ViewItemAdapter<*, *>>().toTypedArray()
)