package com.simple.phonetics

val BRANCH: String
    get() = if (BuildConfig.DEBUG) BuildConfig.BRANCH else "main"

const val TYPE_HISTORY = Int.MAX_VALUE - 100
const val TYPE_VERSION = Int.MAX_VALUE

object Param {

    const val IPA = "IPA"
    const val RATE = "RATE"
    const val TEXT = "TEXT"
    const val FIRST = "FIRST"
    const val RESULT = "RESULT"
    const val NUMBER = "NUMBER"
    const val TASK_ID = "TASK_ID"
    const val REVERSE = "REVERSE"
    const val RESOURCE = "RESOURCE"
    const val VOICE_LIST = "VOICE_LIST"
    const val IS_SUPPORT = "IS_SUPPORT"
    const val KEY_REQUEST = "KEY_REQUEST"
    const val LANGUAGE_CODE = "LANGUAGE_CODE"
    const val PHONETIC_CODE = "PHONETIC_CODE"
    const val VIEW_ITEM_LIST = "VIEW_ITEM_LIST"
    const val BACKGROUND_COLOR = "BACKGROUND_COLOR"
    const val POSITIVE_BACKGROUND = "POSITIVE_BACKGROUND"

    const val VOICE_ID = "VOICE_ID"
    const val VOICE_SPEED = "VOICE_SPEED"

    const val ROOT_TRANSITION_NAME = "ROOT_TRANSITION_NAME"
}

object DeeplinkManager {

    const val COPY = "app://copy"
    const val TOAST = "app://toast"
    const val SPEAK = "app://speak"
    const val EVENT = "app://event"
    const val REVIEW = "app://review"
    const val UPDATE = "app://update"
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

    const val NEW_VERSION = "new_version"
}

object EventName {

    const val DISMISS = "DISMISS"
    const val SHOW_POPUP = "SHOW_POPUP"

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

    const val IPA_VIEW_ITEM_CLICKED = "IPA_VIEW_ITEM_CLICKED"
    const val HISTORY_VIEW_ITEM_CLICKED = "HISTORY_VIEW_ITEM_CLICKED"
    const val SENTENCE_VIEW_ITEM_CLICKED = "SENTENCE_VIEW_ITEM_CLICKED"
    const val TEXT_SIMPLE_VIEW_ITEM_CLICKED = "TEXT_SIMPLE_VIEW_ITEM_CLICKED"

    const val MICROPHONE = "MICROPHONE"
}

object Module {

    val MLKIT by lazy {
        "mlkit" to "com.unknown.mlkit.MlkitInitializer"
    }
}

object Id {

    const val IPA = "IPA"
    const val GAME = "GAME"
    const val VOICE = "VOICE"
    const val LISTEN = "LISTEN"
    const val CHOOSE = "CHOOSE"
    const val BUTTON = "BUTTON"
    const val CONFIG = "CONFIRM"
    const val SUGGEST = "SUGGEST"
    const val RESOURCE = "RESOURCE"
    const val SENTENCE = "SENTENCE"
    const val IPA_LIST = "IPA_LIST"
    const val TRANSLATE = "TRANSLATE"
}

@Suppress("KotlinConstantConditions", "SimplifyBooleanWithConstants")
object Config {

    val ADS_DEBUG = BuildConfig.DEBUG && false
    val RATE_DEBUG = BuildConfig.DEBUG && false
    val EVENT_DEBUG = BuildConfig.DEBUG && false
    val UPDATE_DEBUG = BuildConfig.DEBUG && false
}

object SpeakState {

    const val READY = "SPEAK_STATE_READY"
    const val RECORD_END = "SPEAK_STATE_RECORD_END"
    const val RECORD_START = "SPEAK_STATE_RECORD_START"

    val stateList = listOf(READY, RECORD_END, RECORD_START)
}

object ErrorCode {

    const val NOT_INTERNET = "NOT_INTERNET"
}

object TransitionName {

    const val SELECT_LANGUAGE = "select_language"
}