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

class NextPhaseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNextPhaseBinding
    private var mediaPlayer: MediaPlayer? = null

    private val handler = Handler(Looper.getMainLooper())
    private var autoCloseMs = 6000L

    private val autoCloseRunnable = Runnable {
        if (!isFinishing) finish()
    }

    private var pulseX: ObjectAnimator? = null
    private var pulseY: ObjectAnimator? = null

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

        // ✅ Vem da fase secreta?
        val fromSecret = intent.getBooleanExtra("FROM_SECRET", false)
        val secretLevel = intent.getStringExtra("SECRET_LEVEL") ?: ""
        val hits = intent.getIntExtra("HITS", 0)

        // ✅ 3s no modo secreto (pedido)
        autoCloseMs = if (fromSecret) 3000L else 6000L

        // Título inteligente
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
        binding.curiosityTextView.text = curiosities.random()

        // Fade-in leve (não atrapalha leitura)
        binding.curiosityTextView.alpha = 0f
        binding.curiosityTextView.animate()
            .alpha(1f)
            .setDuration(220)
            .start()

        // Som curtinho
        playSound(R.raw.correct_sound)

        // Botão com pulso bem suave
        startPulse()

        binding.continueButton.setOnClickListener {
            handler.removeCallbacks(autoCloseRunnable)
            playSound(R.raw.button_click)
            finish()
        }

        handler.postDelayed(autoCloseRunnable, autoCloseMs)
    }

    private fun startPulse() {
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
        try { pulseX?.cancel() } catch (_: Exception) {}
        try { pulseY?.cancel() } catch (_: Exception) {}
        pulseX = null
        pulseY = null
    }

    private fun playSound(resId: Int) {
        try {
            mediaPlayer?.release()
            val mp = MediaPlayer.create(this, resId)
            mediaPlayer = mp
            mp.setOnCompletionListener {
                try { it.release() } catch (_: Exception) {}
                if (mediaPlayer === it) mediaPlayer = null
            }
            mp.start()
        } catch (e: Exception) {
            Log.w("NextPhaseActivity", "⚠️ Falha ao tocar som: ${e.localizedMessage}")
        }
    }

    override fun onPause() {
        super.onPause()
        try { mediaPlayer?.let { if (it.isPlaying) it.pause() } } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(autoCloseRunnable)
        stopPulse()
        try { mediaPlayer?.release() } catch (_: Exception) {}
        mediaPlayer = null
    }
}
