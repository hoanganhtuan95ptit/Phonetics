package com.simple.phonetics.utils.exts

import android.media.MediaPlayer
import com.simple.phonetics.PhoneticsApp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first

suspend fun playMedia(source: Int) = playMedia {
    it.setDataSource(PhoneticsApp.share.resources.openRawResourceFd(source))
}

suspend fun playMedia(path: String) = playMedia {
    it.setDataSource(path)
}

private suspend fun playMedia(block: (MediaPlayer) -> Unit) = channelFlow {

    val mediaPlayer = MediaPlayer()

    mediaPlayer.setOnErrorListener { mp, what, extra ->

        trySend(Unit)

        true // Trả về true nếu đã xử lý lỗi
    }

    mediaPlayer.setOnCompletionListener { mp ->

        trySend(Unit)
    }

    block.invoke(mediaPlayer)

    mediaPlayer.prepare()
    mediaPlayer.start()

    awaitClose {

        mediaPlayer.release()
    }
}.first()
