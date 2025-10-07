package com.simple.phonetics.utils.exts

import android.graphics.Color

fun Map<String, String>.getOrKey(key: String) = get(key) ?: key

fun Map<String, String>.getOrEmpty(key: String) = get(key) ?: ""

fun Map<String, Int>.getOrTransparent(key: String) = get(key) ?: Color.TRANSPARENT

fun Map<String, Any>.colorOrTransparent(key: String): Int {
    return (this[key] as? Int) ?: Color.TRANSPARENT
}

//val Map<String, Any>.colorPrimary: Int get() = colorOrTransparent("colorPrimary") // Màu chính của ứng dụng
val Map<String, Any>.colorPrimaryVariant: Int get() = colorOrTransparent("colorPrimaryVariant") // Biến thể của màu chính
//val Map<String, Any>.colorBackground: Int get() = colorOrTransparent("colorBackground") // Màu nền chính
val Map<String, Any>.colorBackgroundVariant: Int get() = colorOrTransparent("colorBackgroundVariant") // Biến thể của màu nền
//val Map<String, Any>.colorOnBackground: Int get() = colorOrTransparent("colorOnBackground") // Màu nội dung trên nền chính
val Map<String, Any>.colorOnBackgroundVariant: Int get() = colorOrTransparent("colorOnBackgroundVariant") // Biến thể nội dung trên nền
//val Map<String, Any>.colorOnPrimary: Int get() = colorOrTransparent("colorOnPrimary") // Màu nội dung trên màu chính
val Map<String, Any>.colorOnPrimaryVariant: Int get() = colorOrTransparent("colorOnPrimaryVariant") // Biến thể nội dung trên màu chính
//val Map<String, Any>.colorSurface: Int get() = colorOrTransparent("colorSurface") // Màu bề mặt chính (cards, sheets, dialogs)
//val Map<String, Any>.colorOnSurface: Int get() = colorOrTransparent("colorOnSurface") // Màu nội dung trên bề mặt
//val Map<String, Any>.colorOnSurfaceVariant: Int get() = colorOrTransparent("colorOnSurfaceVariant") // Biến thể nội dung trên bề mặt
val Map<String, Any>.colorDivider: Int get() = colorOrTransparent("colorDivider") // Màu đường phân cách / gạch chia
//
//val Map<String, Any>.colorError: Int get() = colorOrTransparent("colorError") // Màu hiển thị lỗi
val Map<String, Any>.colorErrorVariant: Int get() = colorOrTransparent("colorErrorVariant") // Biến thể của màu lỗi
val Map<String, Any>.colorOnErrorVariant: Int get() = colorOrTransparent("colorOnErrorVariant") // Màu nội dung trên nền lỗi biến thể

val Map<String, Any>.colorDiphthongs: Int get() = colorOrTransparent("colorDiphthongs") // Màu biểu thị nguyên âm ngắn
val Map<String, Any>.colorConsonantsVoiced: Int get() = colorOrTransparent("colorConsonantsVoiced") // Màu biểu thị nguyên âm ngắn
val Map<String, Any>.colorConsonantsUnvoiced: Int get() = colorOrTransparent("colorConsonantsUnvoiced") // Màu biểu thị nguyên âm ngắn
val Map<String, Any>.colorVowelsShort: Int get() = colorOrTransparent("colorVowelsShort") // Màu biểu thị nguyên âm ngắn
val Map<String, Any>.colorVowelsLong: Int get() = colorOrTransparent("colorVowelsLong") // Màu biểu thị nguyên âm dài
val Map<String, Any>.colorLoading: Int get() = colorOrTransparent("colorLoading") // Màu hiệu ứng đang tải (loading)
