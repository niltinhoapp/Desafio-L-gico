package com.desafiolgico.main.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.desafiolgico.R

class SfxPlayer(context: Context) {

    private val appCtx = context.applicationContext

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val sfxCorrect: Int = soundPool.load(appCtx, R.raw.sfx_correct, 1)
    private val sfxWrong: Int = soundPool.load(appCtx, R.raw.sfx_wrong, 1)

    fun playCorrect() {
        soundPool.play(sfxCorrect, 1f, 1f, 1, 0, 1f)
    }

    fun playWrong() {
        soundPool.play(sfxWrong, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
