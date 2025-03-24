package com.simple.phonetics.utils.exts

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.simple.phonetics.PhoneticsApp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

suspend fun playVibrate() = channelFlow {

    val context = PhoneticsApp.share

    val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    val pattern = longArrayOf(0, 300, 200, 300, 500) // Độ trễ, rung, dừng, rung, dừng

    if (vibrator.hasVibrator()) if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val effect = VibrationEffect.createWaveform(pattern, -1) // -1: không lặp lại
        vibrator.vibrate(effect)
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(pattern, -1)
    }

    launch {
        delay(360L)
        trySend(Unit)
    }

    awaitClose {
        vibrator.cancel()
    }
}.first()