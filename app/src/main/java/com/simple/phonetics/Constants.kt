package com.simple.phonetics


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

object Param {

    const val TEXT = "TEXT"
    const val FIRST = "FIRST"
    const val LANGUAGE_CODE = "LANGUAGE_CODE"

    const val VOICE_ID = "VOICE_ID"
    const val VOICE_SPEED = "VOICE_SPEED"

    const val ROOT_TRANSITION_NAME = "ROOT_TRANSITION_NAME"
}

object Deeplink {

    const val LANGUAGE = "app://language"
    const val PHONETICS = "app://PHONETICS"
}

object Payload {

    const val SELECTED = "SELECTED"
}

object EventName {

    const val GET_VOICE_REQUEST = "GET_VOICE_REQUEST"
    const val GET_VOICE_RESPONSE = "GET_VOICE_RESPONSE"

    const val SPEAK_TEXT_REQUEST = "SPEAK_TEXT_REQUEST"
    const val SPEAK_TEXT_RESPONSE = "SPEAK_TEXT_RESPONSE"

    const val STOP_SPEAK_TEXT_REQUEST = "STOP_SPEAK_TEXT_REQUEST"
    const val STOP_SPEAK_TEXT_RESPONSE = "STOP_SPEAK_TEXT_RESPONSE"
}