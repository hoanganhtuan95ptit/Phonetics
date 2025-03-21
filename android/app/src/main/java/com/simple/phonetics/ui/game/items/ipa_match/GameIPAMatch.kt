package com.simple.phonetics.ui.game.items.ipa_match

import com.simple.phonetics.entities.Phonetic


data class GameIPAMatchQuiz(
    val match: List<Pair<Option, Option>>,
) {

    data class Option(
        val type: Type,
        val phonetic: Phonetic
    ) {

        enum class Type {
            NONE, IPA, TEXT, VOICE
        }
    }
}

data class GameIPAMatchPair(
    val option: GameIPAMatchQuiz.Option?,
    val newType: GameIPAMatchQuiz.Option.Type?
)