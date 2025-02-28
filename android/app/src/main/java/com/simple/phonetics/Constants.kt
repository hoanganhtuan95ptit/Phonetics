package com.simple.phonetics

enum class TAG{
    VIEW_ITEM_LIST
}

object Param {

    const val IPA = "IPA"
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

    const val TOAST = "app://toast"
    const val SPEAK = "app://speak"
    const val CONFIG = "app://config"
    const val CONFIRM = "app://confirm"
    const val LANGUAGE = "app://language"
    const val IPA_LIST = "app://ipa_list"
    const val PHONETICS = "app://phonetics"
    const val IPA_DETAIL = "app://ipa_detail"
}

object Payload {

    const val NAME = "NAME"
    const val SIZE = "SIZE"
    const val THEME = "THEME"
    const val IMAGE = "IMAGE"
    const val MARGIN = "MARGIN"
    const val PADDING = "PADDING"
    const val SELECTED = "SELECTED"
    const val NAME_COLOR = "NAME_COLOR"
    const val BACKGROUND = "BACKGROUND"
    const val LOADING_STATUS = "LOADING_STATUS"
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
    const val IPA_LIST = "IPA_LIST"
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