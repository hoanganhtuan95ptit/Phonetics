package com.simple.phonetics.utils.exts

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Extension cho Context: kiểm tra quyền đã được cấp hay chưa
 */
fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * Extension cho Fragment: kiểm tra quyền đã được cấp hay chưa
 */
fun Fragment.hasPermission(permission: String): Boolean {
    return requireContext().hasPermission(permission)
}

/**
 * Extension cho Context: kiểm tra nhiều quyền cùng lúc
 */
fun Context.hasPermissions(vararg permissions: String): Boolean {
    return permissions.all { hasPermission(it) }
}

/**
 * Extension cho Fragment: kiểm tra nhiều quyền cùng lúc
 */
fun Fragment.hasPermissions(vararg permissions: String): Boolean {
    return requireContext().hasPermissions(*permissions)
}
