package com.simple.phonetics.utils.exts

import android.media.MediaPlayer
import com.simple.phonetics.PhoneticsApp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first

suspend fun playMedia(source: Int) = channelFlow {

    val context = PhoneticsApp.share

    val mediaPlayer = MediaPlayer.create(context, source)

    mediaPlayer.setOnErrorListener { mp, what, extra ->

        trySend(Unit)

        true // Trả về true nếu đã xử lý lỗi
    }

    mediaPlayer.setOnCompletionListener { mp ->

        trySend(Unit)
    }

    mediaPlayer.start()

    awaitClose {

        mediaPlayer.release()
    }
}.first()