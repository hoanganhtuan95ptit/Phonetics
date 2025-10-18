package com.simple.phonetics.utils.provider.ipa_reading

import android.media.AudioAttributes
import android.media.MediaPlayer
import com.simple.analytics.logAnalytics
import com.simple.autobind.annotation.AutoBind
import com.simple.crashlytics.logCrashlytics
import com.simple.ipa.entities.Ipa
import com.simple.phonetics.utils.exts.wrapLink
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doSuccess
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AutoBind(IpaReading::class)
class OnlineIpaReading : IpaReading {

    override fun reading(ipa: Ipa, phoneticCode: String): Flow<ResultState<String>> = channelFlow {

        trySend(ResultState.Start)


        var voiceLink: String? = null

        if (ipa.voices.isNotEmpty()) {

            voiceLink = ipa.voices[phoneticCode]
        }

        if (voiceLink == null) {

            voiceLink = ipa.voice
        }

        voiceLink = voiceLink.wrapLink()


        val timeoutJob = launch {

            kotlinx.coroutines.delay(1 * 60 * 1000)

            trySend(ResultState.Failed(RuntimeException("timeout")))
        }


        val mediaPlayer = MediaPlayer().apply {

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()

            setAudioAttributes(audioAttributes)
            setDataSource(voiceLink)
            prepareAsync()

            setOnPreparedListener {

                timeoutJob.cancel()

                trySend(ResultState.Running(""))
                start() // Bắt đầu phát nhạc khi đã chuẩn bị xong
            }

            setOnErrorListener { _, _, extra ->

                trySend(ResultState.Failed(RuntimeException("$extra")))
                true // Trả về true nếu đã xử lý lỗi
            }

            setOnCompletionListener { _ ->

                trySend(ResultState.Success(""))
            }
        }

        awaitClose {

            mediaPlayer.reset()
            mediaPlayer.release()
        }
    }.map {

        it.doFailed {
            logCrashlytics(TAG, it)
        }

        it.doSuccess {
            logAnalytics("${TAG}_success")
        }

        it
    }

    companion object{

        private const val TAG = "online_ipa_reading"
    }
}