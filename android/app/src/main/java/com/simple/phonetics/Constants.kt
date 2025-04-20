package com.simple.phonetics

object Param {

    const val IPA = "IPA"
    const val RATE = "RATE"
    const val TEXT = "TEXT"
    const val FIRST = "FIRST"
    const val RESULT = "RESULT"
    const val NUMBER = "NUMBER"
    const val TASK_ID = "TASK_ID"
    const val REVERSE = "REVERSE"
    const val IS_SUPPORT = "IS_SUPPORT"
    const val KEY_REQUEST = "KEY_REQUEST"
    const val LANGUAGE_CODE = "LANGUAGE_CODE"
    const val POSITIVE_BACKGROUND = "POSITIVE_BACKGROUND"

    const val VOICE_ID = "VOICE_ID"
    const val VOICE_SPEED = "VOICE_SPEED"

    const val ROOT_TRANSITION_NAME = "ROOT_TRANSITION_NAME"
}

object DeeplinkManager {

    const val TOAST = "app://toast"
    const val SPEAK = "app://speak"
    const val EVENT = "app://event"
    const val CONFIG = "app://config"
    const val CONFIRM = "app://confirm"
    const val LANGUAGE = "app://language"
    const val IPA_LIST = "app://ipa_list"
    const val PHONETICS = "app://phonetics"
    const val RECORDING = "app://recording"
    const val IPA_DETAIL = "app://ipa_detail"

    const val GAME = "app://game"
    const val GAME_CONFIG = "app://game_config"
    const val GAME_IPA_MATCH = "app://game_ipa_match"
    const val GAME_IPA_WORDLE = "app://game_ipa_wordle"
    const val GAME_IPA_PUZZLE = "app://game_ipa_puzzle"
    const val GAME_CONGRATULATION = "app://game_congratulation"
}

object Payload {

    const val NAME = "NAME"
    const val SIZE = "SIZE"
    const val TEXT = "TEXT"
    const val THEME = "THEME"
    const val IMAGE = "IMAGE"
    const val MARGIN = "MARGIN"
    const val PADDING = "PADDING"
    const val NAME_COLOR = "NAME_COLOR"
    const val BACKGROUND = "BACKGROUND"
    const val LOADING_STATUS = "LOADING_STATUS"
    const val BACKGROUND_COLOR = "BACKGROUND_COLOR"
}

object Constants {

    const val WORD_COUNT_MIN = 10
}

object EventName {

    const val DISMISS = "DISMISS"

    const val GET_VOICE_REQUEST = "GET_VOICE_REQUEST"
    const val GET_VOICE_RESPONSE = "GET_VOICE_RESPONSE"

    const val START_READING_TEXT_REQUEST = "START_READING_TEXT_REQUEST"
    const val START_READING_TEXT_RESPONSE = "START_READING_TEXT_RESPONSE"

    const val STOP_READING_TEXT_REQUEST = "STOP_READING_TEXT_REQUEST"
    const val STOP_READING_TEXT_RESPONSE = "STOP_READING_TEXT_RESPONSE"

    const val STOP_SPEAK_TEXT_REQUEST = "STOP_SPEAK_TEXT_REQUEST"
    const val STOP_SPEAK_TEXT_RESPONSE = "STOP_SPEAK_TEXT_RESPONSE"

    const val START_SPEAK_TEXT_REQUEST = "START_SPEAK_TEXT_REQUEST"
    const val START_SPEAK_TEXT_RESPONSE = "START_SPEAK_TEXT_RESPONSE"

    const val CHECK_SUPPORT_SPEAK_TEXT_REQUEST = "CHECK_SUPPORT_SPEAK_TEXT_REQUEST"
    const val CHECK_SUPPORT_SPEAK_TEXT_RESPONSE = "CHECK_SUPPORT_SPEAK_TEXT_RESPONSE"

    const val MICROPHONE = "MICROPHONE"
}

object Id {

    const val IPA = "IPA"
    const val GAME = "GAME"
    const val VOICE = "VOICE"
    const val LISTEN = "LISTEN"
    const val CHOOSE = "CHOOSE"
    const val BUTTON = "BUTTON"
    const val CONFIG = "CONFIRM"
    const val RESOURCE = "RESOURCE"
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