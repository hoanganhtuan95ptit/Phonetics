//package com.simple.phonetics.ui.home.services.input
//
//import androidx.core.graphics.ColorUtils
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MediatorLiveData
//import com.simple.core.utils.extentions.asObjectOrNull
//import com.simple.coreapp.ui.view.Background
//import com.simple.coreapp.utils.ext.DP
//import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
//import com.simple.coreapp.utils.extentions.get
//import com.simple.coreapp.utils.extentions.postValue
//import com.simple.image.ImagePath
//import com.simple.image.RichImage
//import com.simple.phonetics.ui.base.fragments.BaseViewModel
//import com.unknown.theme.utils.exts.colorBackground
//
//class InputHomeViewModel : BaseViewModel() {
//
//    val inputHeight: LiveData<Int> = MediatorLiveData()
//
//    val inputHeightIncludeFilter: LiveData<Int> = MediatorLiveData()
//
//
//    val inputInfo: LiveData<InputInfo> = combineSourcesWithDiff(theme, inputHeightIncludeFilter) {
//
//        val theme = theme.get()
//
//        val inputHeight = inputHeight.get()
//        val inputHeightIncludeFilter = inputHeightIncludeFilter.get()
//
//        val background = Background(
//            backgroundColor = theme.colorBackground,
//            cornerRadius_TL = 0,
//            cornerRadius_TR = 0,
//            cornerRadius_BL = DP.DP_16,
//            cornerRadius_BR = DP.DP_16,
//        )
//
//        val info = InputInfo(
//
//            inputHeight = inputHeight,
//            inputHeightIncludeFilter = inputHeightIncludeFilter,
//
//            background = background,
//
//            imageBackground = ImagePath(theme.get("image_background").asObjectOrNull<String>().orEmpty()),
//            imageBackgroundFilter = ColorUtils.setAlphaComponent(theme.colorBackground, 230)
//        )
//
//        postValue(info)
//    }
//
//
//    fun updateInputHeight(height: Int) {
//        inputHeight.postValue(height)
//    }
//
//    fun updateInputHeightIncludeFilter(height: Int) {
//        inputHeightIncludeFilter.postValue(height)
//    }
//
//    data class InputInfo(
//        val inputHeight: Int,
//        val inputHeightIncludeFilter: Int,
//
//        val background: Background,
//
//        val imageBackground: RichImage,
//        val imageBackgroundFilter: Int
//    )
//}