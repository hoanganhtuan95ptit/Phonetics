package com.simple.phonetics

import com.simple.phonetics.entities.Ipa
import com.simple.phonetics.entities.Language

val DEFAULT_TRANSLATE = mapOf(
    "title_voice" to "Voice:",
    "title_result" to "Result:",
    "title_history" to "History:",
    "title_phonetic" to "Phonetic:",
    "title_language" to "Select language",
    "title_translate" to "Translate:",
    "title_voice_speed" to "Voice speed:",
    "hint_enter_text" to "Enter text here!",
    "hint_enter_language_text" to "Enter \$language_name here!",
    "message_start_sync" to "Starting synchronization...",
    "message_result_empty" to "No content yet!",
    "message_sync_completed" to "Synchronization completed",
    "message_select_language" to "Select the language you want to find phonetics for",
    "message_support_translate" to "Translation supported",
    "message_translate_download" to "Downloading translation...",
    "message_start_sync_phonetics" to "Synchronizing phonetics \$ipa_name \$percent%",
    "message_completed_sync_phonetics" to "Phonetics \$ipa_name synchronized successfully",
    "message_start_sync_translate" to "Synchronizing translation \$percent%",
    "message_completed_sync_translate" to "Translation synchronized successfully",
    "action_clear" to "Clear",
    "action_reverse" to "Reverse",
    "action_try_speak" to "Try speak",
    "action_confirm_change_language" to "Change phonetic language",
    "speed_lever" to "Speed \$lever",
    "voice_index" to "Voice \$index",
    "translating" to "Translating",
    "translate_failed" to "Translation failed"
)

val DEFAULT_LANGUAGE = listOf(
    Language(
        id = Language.EN,
        name = "English",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/main/flags/en.webp",
        listIpa = listOf(
            Ipa(
                code = "UK",
                name = "English - UK",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/en_UK.txt"
            ),
            Ipa(
                code = "US",
                name = "English - US",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/en_US.txt"
            )
        )
    )
)