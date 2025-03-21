package com.simple.phonetics.ui.game.items.ipa_puzzle


data class GameIPAPuzzleQuiz(
    val answers: List<String>,
    val question: Question,
) {

    data class Question(
        val text: String,
        val ipaMissing: String,
        val ipaIncomplete: String,
    )
}