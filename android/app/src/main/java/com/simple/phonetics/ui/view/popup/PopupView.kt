package com.simple.phonetics.ui.view.popup

import android.content.ComponentCallbacks

interface PopupView {

    fun priority(): Int {

        return 1
    }

    fun setup(componentCallbacks: ComponentCallbacks)
}