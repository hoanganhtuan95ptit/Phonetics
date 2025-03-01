package com.simple.phonetics

import com.simple.phonetics.entities.Language

val DEFAULT_TRANSLATE = mapOf(
    "title_language" to "Select language",
    "title_translate" to "Translate:",
    "title_voice" to "Voice:",
    "title_voice_speed" to "Voice speed:",
    "title_result" to "Result:",
    "title_history" to "History:",
    "title_phonetic" to "Phonetic:",
    "title_ipa" to "Ipa:",
    "ipa_list_screen_title" to "Ipa list",
    "ipa_detail_screen_vowels_long" to "Vowels long",
    "ipa_detail_screen_vowels_short" to "Vowels short",
    "ipa_detail_screen_diphthongs" to "Diphthongs",
    "ipa_detail_screen_consonants_voiced" to "Consonants voiced",
    "ipa_detail_screen_consonants_unvoiced" to "Consonants unvoiced",
    "ipa_detail_screen_title_example" to "Example:",
    "hint_enter_text" to "Enter text here!",
    "hint_enter_language_text" to "Enter \$language_name here!",
    "message_start_sync" to "Starting synchronization...",
    "message_sync_completed" to "Synchronization completed",
    "message_result_empty" to "No content yet!",
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
    "action_view_all_ipa" to "View all",
    "speed_lever" to "Speed \$lever",
    "voice_index" to "Voice \$index",
    "translating" to "Translating",
    "translate_failed" to "Translation failed",
    "rate_title" to "Rate app",
    "rate_message" to "We’d love your feedback to make the app even better. Your review means a lot! \uD83C\uDF1F",
    "rate_action_positive" to "Rate",
    "rate_action_negative" to "Late"
)

val DEFAULT_LANGUAGE = listOf(
    Language(
        id = Language.EN,
        name = "English",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/main/flags/en.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "UK",
                name = "English - UK",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/en_UK.txt"
            ),
            Language.IpaSource(
                code = "US",
                name = "English - US",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/en_US.txt"
            )
        )
    )
)