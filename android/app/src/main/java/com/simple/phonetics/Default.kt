package com.simple.phonetics

import com.simple.phonetics.entities.Language

val DEFAULT_TRANSLATE = mapOf(
    "action_clear" to "Clear",
    "action_confirm_change_language" to "Change phonetic language",
    "action_play_game" to "Play game",
    "action_reverse" to "Reverse",
    "action_try_speak" to "Try speak",
    "action_view_all_ipa" to "View all",

    "game_config_screen_action_play_game" to "Jump into the game now!",
    "game_config_screen_message_resource_from_history" to "Based on previously looked up words",
    "game_config_screen_message_resource_from_popular" to "Based on commonly used words",
    "game_config_screen_message_resource_limit" to "Selection unavailable due to low word count",
    "game_config_screen_title_resource" to "Select play source",

    "game_ipa_wordle_screen_action" to "Check",
    "game_ipa_wordle_screen_action_continue" to "Continue",
    "game_ipa_wordle_screen_action_retry" to "Retry",
    "game_ipa_wordle_screen_message_answer" to "\$param1 of \$param2 is \$param3",
    "game_ipa_wordle_screen_title" to "Select \$param1 that matches \$param2 below?",
    "game_ipa_wordle_screen_title_answer_failed" to "Not exactly",
    "game_ipa_wordle_screen_title_answer_true" to "Great",

    "game_screen_title" to "Game",

    "hint_enter_language_text" to "Enter \$language_name here!",
    "hint_enter_text" to "Enter text here!",

    "ipa_c" to "Central",
    "ipa_de" to "German",
    "ipa_eo" to "Esperanto",
    "ipa_es_es" to "Spanish (Spain)",
    "ipa_es_mx" to "Spanish (Mexico)",
    "ipa_fa" to "Persian",
    "ipa_fi" to "Finnish",
    "ipa_fr_fr" to "French (France)",
    "ipa_fr_qc" to "French (Québec)",
    "ipa_is" to "Icelandic",
    "ipa_ja" to "Japanese",
    "ipa_jam" to "Jamaican Creole",
    "ipa_km" to "Khmer",
    "ipa_ko" to "Korean",
    "ipa_ma" to "Malay (Malaysian and Indonesian)",
    "ipa_n" to "Northern",
    "ipa_nb" to "Norwegian Bokmål",
    "ipa_nl" to "Dutch",
    "ipa_or" to "Odia",
    "ipa_ro" to "Romanian",
    "ipa_s" to "Southern",
    "ipa_sv" to "Swedish",
    "ipa_sw" to "Swahili",
    "ipa_tts" to "Isan",
    "ipa_uk" to "English - UK",
    "ipa_us" to "English - US",
    "ipa_yue" to "Cantonese",
    "ipa_zh_hans" to "Simplified Chinese",
    "ipa_zh_hant" to "Traditional Chinese",

    "ipa_detail_screen_consonants_unvoiced" to "Consonants unvoiced",
    "ipa_detail_screen_consonants_voiced" to "Consonants voiced",
    "ipa_detail_screen_diphthongs" to "Diphthongs",
    "ipa_detail_screen_title_example" to "Example:",
    "ipa_detail_screen_vowels_long" to "Vowels long",
    "ipa_detail_screen_vowels_short" to "Vowels short",

    "ipa_list_screen_title" to "Ipa list",

    "language_ar" to "Arab",
    "language_de" to "Germany",
    "language_eo" to "Esperanto",
    "language_en" to "English",
    "language_es" to "Spain, Mexico",
    "language_fa" to "Iran",
    "language_fi" to "Finland",
    "language_fr" to "France, Québec",
    "language_is" to "Iceland",
    "language_ja" to "Japan",
    "language_jam" to "Jamaican Creole",
    "language_km" to "Cambodia",
    "language_ko" to "South Korea",
    "language_ma" to "Malaysia, Indonesia",
    "language_nb" to "Norway",
    "language_nl" to "Netherlands",
    "language_or" to "India",
    "language_ro" to "Romania",
    "language_sv" to "Sweden",
    "language_sw" to "Swahili",
    "language_tts" to "Thailand",
    "language_vi" to "Vietnamese",
    "language_zh" to "Chinese",

    "message_completed_sync_phonetics" to "Phonetics \$ipa_name synchronized successfully",
    "message_completed_sync_translate" to "Translation synchronized successfully",
    "message_result_empty" to "No content yet!",
    "message_select_language" to "Select the language you want to find phonetics for",
    "message_start_sync" to "Starting synchronization...",
    "message_start_sync_phonetics" to "Synchronizing phonetics \$ipa_name \$percent%",
    "message_start_sync_translate" to "Synchronizing translation \$percent%",
    "message_support_translate" to "Translation supported",
    "message_sync_completed" to "Synchronization completed",
    "message_translate_download" to "Downloading translation...",

    "rate_action_negative" to "Late",
    "rate_action_positive" to "Rate",
    "rate_message" to "We’d love your feedback to make the app even better. Your review means a lot! 🌟",
    "rate_title" to "Rate app",

    "recording_screen_title" to "Please speak \$language_name",

    "resource_history" to "From the search history",
    "resource_popular" to "From the popular list",

    "speed_lever" to "Speed \$lever",

    "title_game" to "Entertainment game:",
    "title_history" to "History:",
    "title_ipa" to "Ipa:",
    "title_language" to "Select language",
    "title_phonetic" to "Phonetic:",
    "title_result" to "Result:",
    "title_translate" to "Translate:",
    "title_voice" to "Voice:",
    "title_voice_speed" to "Voice speed:",

    "translating" to "Translating",
    "translate_failed" to "Translation failed",

    "type_ipa" to "Transcription",
    "type_text" to "Letter",
    "type_voice" to "Pronounce",

    "voice_index" to "Voice \$index"
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
    ),
    Language(
        id = Language.VI,
        name = "Vietnamese",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/main/flags/vi.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "N",
                name = "Northern",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/vi_N.txt"
            ),
            Language.IpaSource(
                code = "C",
                name = "Central",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/vi_C.txt"
            ),
            Language.IpaSource(
                code = "S",
                name = "Southern",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/vi_S.txt"
            )
        )
    ),
    Language(
        id = Language.ZH,
        name = "Chinese",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_zh.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "zh_Hans",
                name = "Simplified Chinese",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/zh_hans.txt"
            ),
            Language.IpaSource(
                code = "zh_Hant",
                name = "Traditional Chinese",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/zh_hant.txt"
            ),
            Language.IpaSource(
                code = "yue",
                name = "Cantonese",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/yue.txt"
            )
        )
    ),
    Language(
        id = Language.ES,
        name = "Spain, Mexico",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_es.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "es_ES",
                name = "Spanish (Spain)",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/es_ES.txt"
            ),
            Language.IpaSource(
                code = "es_MX",
                name = "Spanish (Mexico)",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/es_MX.txt"
            )
        )
    ),
    Language(
        id = Language.FR,
        name = "France, Québec",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_fr.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "fr_FR",
                name = "French (France)",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/fr_FR.txt"
            ),
            Language.IpaSource(
                code = "fr_QC",
                name = "French (Québec)",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/fr_QC.txt"
            )
        )
    ),
    Language(
        id = Language.AR,
        name = "Arab",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_ar.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "ar",
                name = "Arabic (Modern Standard)",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/ar.txt"
            )
        )
    ),
    Language(
        id = Language.DE,
        name = "Germany",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_de.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "de",
                name = "German",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/de.txt"
            )
        )
    ),
    Language(
        id = Language.EO,
        name = "Esperanto",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_eo.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "eo",
                name = "Esperanto",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/eo.txt"
            )
        )
    ),
    Language(
        id = Language.FA,
        name = "Iran",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_fa.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "fa",
                name = "Persian",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/fa.txt"
            )
        )
    ),
    Language(
        id = Language.FI,
        name = "Finland",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_fi.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "fi",
                name = "Finnish",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/fi.txt"
            )
        )
    ),
    Language(
        id = Language.IS,
        name = "Iceland",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_is.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "is",
                name = "Icelandic",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/is.txt"
            )
        )
    ),
    Language(
        id = Language.JA,
        name = "Japan",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_ja.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "ja",
                name = "Japanese",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/ja.txt"
            )
        )
    ),
    Language(
        id = Language.KM,
        name = "Cambodia",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_km.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "km",
                name = "Khmer",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/km.txt"
            )
        )
    ),
    Language(
        id = Language.KO,
        name = "South Korea",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_ko.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "ko",
                name = "Korean",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/ko.txt"
            )
        )
    ),
    Language(
        id = Language.MA,
        name = "Malaysia, Indonesia",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/malay.png",
        listIpa = listOf(
            Language.IpaSource(
                code = "ma",
                name = "Malay (Malaysian and Indonesian)",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/ma.txt"
            )
        )
    ),
    Language(
        id = Language.NB,
        name = "Norway",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/norway.png",
        listIpa = listOf(
            Language.IpaSource(
                code = "nb",
                name = "Norwegian Bokmål",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/nb.txt"
            )
        )
    ),
    Language(
        id = Language.NL,
        name = "Netherlands",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_nl.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "nl",
                name = "Dutch",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/nl.txt"
            )
        )
    ),
    Language(
        id = Language.OR,
        name = "India",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/india.png",
        listIpa = listOf(
            Language.IpaSource(
                code = "or",
                name = "Odia",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/or.txt"
            )
        )
    ),
    Language(
        id = Language.RO,
        name = "Romania",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_ro.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "ro",
                name = "Romanian",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/ro.txt"
            )
        )
    ),
    Language(
        id = Language.SV,
        name = "Sweden",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_sv.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "sv",
                name = "Swedish",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/sv.txt"
            )
        )
    ),
    Language(
        id = Language.SW,
        name = "Swahili",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_sw.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "sw",
                name = "Swahili",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/sw.txt"
            )
        )
    ),
    Language(
        id = Language.TTS,
        name = "Thailand",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/isan.png",
        listIpa = listOf(
            Language.IpaSource(
                code = "tts",
                name = "Isan",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/tts.txt"
            )
        )
    ),
    Language(
        id = Language.JAM,
        name = "Jamaican Creole",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/jam.png",
        listIpa = listOf(
            Language.IpaSource(
                code = "jam",
                name = "Jamaican Creole",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/jam.txt"
            )
        )
    )
)