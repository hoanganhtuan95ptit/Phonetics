package com.simple.phonetics.ui.game.items.ipa_wordle

data class GameIPAWordleQuiz(
    val answers: List<com.simple.phonetic.entities.Phonetic>,
    val answerType: Type,

    val question: com.simple.phonetic.entities.Phonetic,
    val questionType: Type,
) {

    enum class Type {
        VOICE, IPA, TEXT
    }
}
