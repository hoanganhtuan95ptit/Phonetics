package com.simple.phonetics.ui.game.items.ipa_wordle

import com.simple.phonetics.entities.Phonetic

data class GameIPAWordleQuiz(
    val answers: List<Phonetic>,
    val answerType: Type,

    val question: Phonetic,
    val questionType: Type,
) {

    enum class Type {
        VOICE, IPA, TEXT
    }
}
