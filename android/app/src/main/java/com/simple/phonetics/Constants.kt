package com.simple.phonetics

object Param {

    const val RATE = "RATE"
    const val TEXT = "TEXT"
    const val FIRST = "FIRST"
    const val LANGUAGE_CODE = "LANGUAGE_CODE"
    const val TRANSITION_DURATION = "TRANSITION_DURATION"

    const val VOICE_ID = "VOICE_ID"
    const val VOICE_SPEED = "VOICE_SPEED"

    const val ROOT_TRANSITION_NAME = "ROOT_TRANSITION_NAME"
}

object Deeplink {

    const val SPEAK = "app://speak"
    const val CONFIG = "app://config"
    const val CONFIRM = "app://confirm"
    const val LANGUAGE = "app://language"
    const val PHONETICS = "app://phonetics"
}

object Payload {

    const val NAME = "NAME"
    const val THEME = "THEME"
    const val SELECTED = "SELECTED"
    const val NAME_COLOR = "NAME_COLOR"
    const val BACKGROUND_COLOR = "BACKGROUND_COLOR"
}

object EventName {

    const val GET_VOICE_REQUEST = "GET_VOICE_REQUEST"
    const val GET_VOICE_RESPONSE = "GET_VOICE_RESPONSE"

    const val START_LISTEN_TEXT_REQUEST = "START_LISTEN_TEXT_REQUEST"
    const val START_LISTEN_TEXT_RESPONSE = "START_LISTEN_TEXT_RESPONSE"

    const val STOP_LISTEN_TEXT_REQUEST = "STOP_LISTEN_TEXT_REQUEST"
    const val STOP_LISTEN_TEXT_RESPONSE = "STOP_LISTEN_TEXT_RESPONSE"

    const val STOP_SPEAK_TEXT_REQUEST = "STOP_SPEAK_TEXT_REQUEST"
    const val STOP_SPEAK_TEXT_RESPONSE = "STOP_SPEAK_TEXT_RESPONSE"

    const val START_SPEAK_TEXT_REQUEST = "START_SPEAK_TEXT_REQUEST"
    const val START_SPEAK_TEXT_RESPONSE = "START_SPEAK_TEXT_RESPONSE"

    const val CHECK_SUPPORT_SPEAK_TEXT_REQUEST = "CHECK_SUPPORT_SPEAK_TEXT_REQUEST"
    const val CHECK_SUPPORT_SPEAK_TEXT_RESPONSE = "CHECK_SUPPORT_SPEAK_TEXT_RESPONSE"
}

object Id {

    const val IPA = "IPA"
    const val VOICE = "VOICE"
    const val SENTENCE = "SENTENCE"
    const val TRANSLATE = "TRANSLATE"
}

object SpeakState {

    const val READY = "READY"
    const val RECORD_END = "RECORD_END"
    const val RECORD_START = "RECORD_START"
}

object TransitionName {

    const val SELECT_LANGUAGE = "select_language"
}