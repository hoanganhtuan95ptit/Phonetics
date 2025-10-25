package com.simple.phonetics.utils.exts

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

fun <T> Flow<T>.wrapError(block: (Throwable) -> T) = this@wrapError.catch {

    emit(block.invoke(it))
}