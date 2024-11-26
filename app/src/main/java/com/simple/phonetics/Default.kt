package com.simple.phonetics

import com.simple.phonetics.entities.Ipa
import com.simple.phonetics.entities.Language


val DEFAULT_TRANSLATE = mapOf(
    "title_voice" to "Voice:",
    "title_history" to "History:",
    "title_phonetic" to "Phonetic:",
    "title_translate" to "Translate:",
    "title_voice_speed" to "Voice Speed:",
    "speed_lever" to "Speed \$lever",
    "voice_index" to "Voice \$index",
    "hint_enter_text" to "Enter here!",
    "hint_enter_language_text" to "Enter \$language_name here!",
    "message_support_translate" to "Support translate",
    "message_translate_download" to "Translate downloading…",
    "message_result_empty" to "There is no content yet!",
    "action_clear" to "Clear",
    "action_reverse" to "Reverse",
    "translating" to "Translation is in progress",
    "translate_failed" to "The translation process failed"
)


val DEFAULT_LANGUAGE = listOf(
    Language(
        id = Language.EN,
        name = "English",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/main/flags/en.webp",
        listIpa = listOf(
            Ipa(
                code = "UK",
                name = "UK",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/en_UK.txt"
            ),
            Ipa(
                code = "US",
                name = "US",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/en_US.txt"
            )
        ),
        isSupportDetect = true
    )
)