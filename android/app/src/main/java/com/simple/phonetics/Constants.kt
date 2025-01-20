package com.simple.phonetics

object Param {

    const val TEXT = "TEXT"
    const val FIRST = "FIRST"
    const val LANGUAGE_CODE = "LANGUAGE_CODE"
    const val TRANSITION_DURATION = "TRANSITION_DURATION"

    const val VOICE_ID = "VOICE_ID"
    const val VOICE_SPEED = "VOICE_SPEED"

    const val ROOT_TRANSITION_NAME = "ROOT_TRANSITION_NAME"
}

object Deeplink {

    const val LANGUAGE = "app://language"
    const val PHONETICS = "app://PHONETICS"
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

    const val SPEAK_TEXT_REQUEST = "SPEAK_TEXT_REQUEST"
    const val SPEAK_TEXT_RESPONSE = "SPEAK_TEXT_RESPONSE"

    const val STOP_SPEAK_TEXT_REQUEST = "STOP_SPEAK_TEXT_REQUEST"
    const val STOP_SPEAK_TEXT_RESPONSE = "STOP_SPEAK_TEXT_RESPONSE"
}

object Id {

    const val IPA = "IPA"
}

object TransitionName {

    const val SELECT_LANGUAGE = "select_language"
}