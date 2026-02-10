package com.desafiolgico.main

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityNextPhaseBinding
import com.desafiolgico.utils.GameDataManager
import kotlin.random.Random

class NextPhaseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNextPhaseBinding

    private val handler by lazy { Handler(Looper.getMainLooper()) }

    private var autoCloseMs: Long = 6000L
    private var autoClosePosted = false

    private var pulseX: ObjectAnimator? = null
    private var pulseY: ObjectAnimator? = null

    private var mediaPlayer: MediaPlayer? = null

    private val autoCloseRunnable = Runnable {
        autoClosePosted = false
        if (!isFinishing && !isDestroyed) finish()
    }

    private val curiosities = listOf(
        "🌊 Sabia que o coração de um camarão fica na cabeça?",
        "🐘 O elefante é o único animal com quatro joelhos.",
        "🦋 As borboletas sentem o gosto com os pés!",
        "🔥 O Sol representa 99,86% da massa do Sistema Solar.",
        "💡 O cérebro humano gera eletricidade suficiente para acender uma lâmpada pequena.",
        "⚡ O relâmpago é mais quente que a superfície do Sol.",
        "🌎 A Terra não é perfeitamente redonda — é ligeiramente achatada nos polos.",
        "💓 Seu coração bate cerca de 100 mil vezes por dia.",
        "👀 Os olhos conseguem distinguir mais de 10 milhões de cores.",
        "🦵 O fêmur humano é mais forte que concreto.",
        "🧬 Cada célula do seu corpo contém cerca de 2 metros de DNA.",
        "🌌 Existem mais estrelas no universo do que grãos de areia na Terra.",
        "🐝 Abelhas reconhecem rostos humanos.",
        "🧠 Seu cérebro pesa cerca de 1,4 kg.",
        "🪶 O pinguim tem joelhos escondidos sob as penas.",
        "🦈 Tubarões existem antes dos dinossauros.",
        "🌧️ A chuva tem cheiro — chamado de petrichor.",
        "🌙 A Lua se afasta da Terra cerca de 3,8 cm por ano.",
        "🚀 Um foguete pode ultrapassar 28.000 km/h ao deixar a atmosfera.",
        "🐢 As tartarugas podem respirar pela cloaca (parte traseira do corpo)."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNextPhaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // extras
        val fromSecret = intent.getBooleanExtra(EXTRA_FROM_SECRET, false)
        val secretLevel = intent.getStringExtra(EXTRA_SECRET_LEVEL).orEmpty()

        autoCloseMs = if (fromSecret) 3000L else 6000L

        val title = if (fromSecret) {
            when (secretLevel) {
                GameDataManager.SecretLevels.RELAMPAGO -> "⚡ Curiosidade Relâmpago"
                GameDataManager.SecretLevels.PERFEICAO -> "💎 Curiosidade Perfeição"
                GameDataManager.SecretLevels.ENIGMA -> "🧩 Curiosidade Enigma"
                else -> "✨ Curiosidade rápida"
            }
        } else {
            "✨ Curiosidade rápida"
        }

        binding.phaseTextView.text = title
        binding.curiosityTextView.text = pickCuriosity()

        binding.curiosityTextView.alpha = 0f
        binding.curiosityTextView.animate()
            .alpha(1f)
            .setDuration(220)
            .start()

        playSound(R.raw.correct_sound)
        startPulse()

        binding.continueButton.setOnClickListener {
            cancelAutoClose()
            playSound(R.raw.button_click)
            finish()
        }

        postAutoClose()
    }

    override fun onResume() {
        super.onResume()
        // se voltou e ainda está aberto, garante autoclose e pulse
        startPulse()
        postAutoClose()
    }

    override fun onPause() {
        super.onPause()
        cancelAutoClose()
        stopPulse()
        pauseSound()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelAutoClose()
        stopPulse()
        releaseSound()
    }

    // =============================================================================================
    // Curiosity
    // =============================================================================================

    private fun pickCuriosity(): String {
        if (curiosities.isEmpty()) return "✨ Curiosidade rápida"
        return curiosities[Random.nextInt(curiosities.size)]
    }

    // =============================================================================================
    // Auto close
    // =============================================================================================

    private fun postAutoClose() {
        if (autoClosePosted) return
        autoClosePosted = true
        handler.postDelayed(autoCloseRunnable, autoCloseMs)
    }

    private fun cancelAutoClose() {
        if (!autoClosePosted) return
        handler.removeCallbacks(autoCloseRunnable)
        autoClosePosted = false
    }

    // =============================================================================================
    // Pulse
    // =============================================================================================

    private fun startPulse() {
        if (pulseX?.isRunning == true || pulseY?.isRunning == true) return

        stopPulse()

        pulseX = ObjectAnimator.ofFloat(binding.continueButton, "scaleX", 1f, 1.04f, 1f).apply {
            duration = 900
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        pulseY = ObjectAnimator.ofFloat(binding.continueButton, "scaleY", 1f, 1.04f, 1f).apply {
            duration = 900
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun stopPulse() {
        runCatching { pulseX?.cancel() }.onFailure {
            Log.w(TAG, "Falha ao cancelar pulseX", it)
        }
        runCatching { pulseY?.cancel() }.onFailure {
            Log.w(TAG, "Falha ao cancelar pulseY", it)
        }
        pulseX = null
        pulseY = null
    }

    // =============================================================================================
    // Sound
    // =============================================================================================

    private fun playSound(resId: Int) {
        releaseSound()
        runCatching {
            val mp = MediaPlayer.create(this, resId) ?: return
            mediaPlayer = mp
            mp.setOnCompletionListener {
                runCatching { it.release() }
                if (mediaPlayer === it) mediaPlayer = null
            }
            mp.start()
        }.onFailure {
            Log.w(TAG, "⚠️ Falha ao tocar som", it)
        }
    }

    private fun pauseSound() {
        runCatching {
            mediaPlayer?.let { if (it.isPlaying) it.pause() }
        }.onFailure {
            Log.w(TAG, "Falha ao pausar MediaPlayer", it)
        }
    }

    private fun releaseSound() {
        val mp = mediaPlayer ?: return
        mediaPlayer = null
        runCatching { if (mp.isPlaying) mp.stop() }
        runCatching { mp.release() }
    }

    companion object {
        private const val TAG = "NextPhaseActivity"

        const val EXTRA_FROM_SECRET = "FROM_SECRET"
        const val EXTRA_SECRET_LEVEL = "SECRET_LEVEL"
        const val EXTRA_HITS = "HITS" // mantido (mesmo se não usar)
    }
}
