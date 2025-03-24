package com.simple.phonetics.entities

sealed class State(val value: Int = 0) {

    data object Start : State()

    data object Completed : State(Int.MAX_VALUE)

    data class SyncTranslate(val name: String, val percent: Float) : State(2)

    data class SyncPhonetics(val code: String, val name: String, val percent: Float) : State(1)
}
