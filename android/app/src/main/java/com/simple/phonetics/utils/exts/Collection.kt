package com.simple.phonetics.utils.exts

public inline fun <T> Collection<T>.takeIfNotEmpty(): Collection<T>? = takeIf { !isEmpty() }