package com.simple.phonetics.entities

import com.simple.state.ResultState

class Sentence(
    val text: String
) {

    var translateState: ResultState<String> = ResultState.Start

    var phonetics: List<Phonetic> = emptyList()
}