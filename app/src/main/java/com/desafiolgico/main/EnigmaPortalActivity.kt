package com.desafiolgico.main

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityEnigmaPortalBinding
import com.desafiolgico.utils.EnigmaPortalGate
import com.google.android.material.card.MaterialCardView
import kotlin.math.abs

class EnigmaPortalActivity : AppCompatActivity() {

    companion object { const val EXTRA_SCORE = "extra_score" }

    private lateinit var binding: ActivityEnigmaPortalBinding

    private data class Enigma(
        val title: String,
        val question: String,
        val options: List<String>,
        val correct: Int,
        val hint: String
    )

    private val enigmas = listOf(
        Enigma(
            title = "ENIGMA I",
            question = "Sequência: 2, 6, 12, 20, 30, ?",
            options = listOf("36", "40", "42", "44"),
            correct = 1,
            hint = "Dica: n² + n (2²+2=6, 3²+3=12...)"
        ),
        Enigma(
            title = "ENIGMA II",
            question = "Tenho chaves, mas não abro portas. Tenho espaço, mas não tenho salas. O que sou?",
            options = listOf("Um mapa", "Um teclado", "Um livro", "Uma janela"),
            correct = 1,
            hint = "Dica: “keys” + “space”."
        ),
        Enigma(
            title = "ENIGMA III",
            question = "CÓDIGO FINAL: use as respostas corretas (I, II) e escolha o padrão.\nQual opção fecha o portal?",
            options = listOf(
                "I=40, II=Teclado → 4-0-2",
                "I=40, II=Teclado → 4-1-2",
                "I=40, II=Teclado → 4-0-1",
                "I=40, II=Teclado → 4-2-0"
            ),
            correct = 0,
            hint = "Dica: pegue os dígitos do número + posição do item correto."
        )
    )

    private var stage = 0
    private var errorsLeft = 2

    private var timer: CountDownTimer? = null
    private var stability = 100 // 0..100

    private var finished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnigmaPortalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarPortal.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbarPortal.setNavigationOnClickListener { finish() }

        val score = intent.getIntExtra(EXTRA_SCORE, 0)

        // trava se não tiver score (segurança)
        if (score < EnigmaPortalGate.requiredScore()) {
            Toast.makeText(
                this,
                "Portal bloqueado. Alcance ${EnigmaPortalGate.requiredScore()} pontos.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        // ✅ 3 tentativas por dia
        val left = EnigmaPortalGate.attemptsLeftToday(this)
        if (left <= 0) {
            Toast.makeText(this, "O portal já foi usado 3x hoje. Volte amanhã.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupButtons()
        playIntro()
        renderStage()
        updateAttemptsBadge()
        startStabilityTimer()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    // -------------------------
    // AAA Intro
    // -------------------------
    private fun playIntro() {
        binding.overlayTop.visibility = View.VISIBLE
        binding.overlayTop.alpha = 0f
        binding.txtOverlay.text = "CÂMARA DOS ENIGMAS"

        // “breath”
        binding.cardEnigma.scaleX = 0.98f
        binding.cardEnigma.scaleY = 0.98f
        binding.cardEnigma.alpha = 0.85f

        binding.overlayTop.animate()
            .alpha(1f)
            .setDuration(320)
            .withEndAction {
                binding.overlayTop.animate()
                    .alpha(0f)
                    .setDuration(520)
                    .withEndAction { binding.overlayTop.visibility = View.GONE }
                    .start()
            }
            .start()
    }

    private fun setupButtons() {
        binding.opt1.setOnClickListener { onOption(0 ) }
        binding.opt2.setOnClickListener { onOption(1) }
        binding.opt3.setOnClickListener { onOption(2) }
        binding.opt4.setOnClickListener { onOption(3) }
    }

    // -------------------------
    // Render
    // -------------------------
    private fun renderStage() {
        val e = enigmas[stage]
        binding.txtStage.text = e.title
        binding.txtQuestion.text = e.question
        binding.txtHint.text = e.hint

        binding.opt1.text = e.options[0]
        binding.opt2.text = e.options[1]
        binding.opt3.text = e.options[2]
        binding.opt4.text = e.options[3]

        binding.txtLives.text = "Erros restantes: $errorsLeft"
        updateDoorsUi()
        updateStabilityUi()
        updateAttemptsBadge()

        // anima cartão (premium)
        binding.cardEnigma.animate().cancel()
        binding.cardEnigma.scaleX = 0.985f
        binding.cardEnigma.scaleY = 0.985f
        binding.cardEnigma.alpha = 0.0f
        binding.cardEnigma.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(240)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }


    private fun updateDoorsUi() {
        // estados:
        // index < stage  -> SOLVED (dourado)
        // index == stage -> CURRENT (foco forte)
        // index > stage  -> LOCKED (apagado)
        applyDoorState(binding.door1, binding.txtDoor1, 0)
        applyDoorState(binding.door2, binding.txtDoor2, 1)
        applyDoorState(binding.door3, binding.txtDoor3, 2)
    }

    private fun applyDoorState(card: MaterialCardView, tv: TextView, index: Int) {
        val gold = ContextCompat.getColor(this, R.color.portal_gold)
        val stroke = ContextCompat.getColor(this, R.color.portal_stroke)
        val surface55 = ContextCompat.getColor(this, R.color.portal_surface_55)
        val surface70 = ContextCompat.getColor(this, R.color.portal_surface_70)
        val text = ContextCompat.getColor(this, R.color.portal_text)
        val textDim = ContextCompat.getColor(this, R.color.portal_text_dim)

        val isSolved = index < stage
        val isCurrent = index == stage

        when {
            isSolved -> {
                card.setCardBackgroundColor(surface70)
                card.strokeColor = gold
                card.strokeWidth = dp(2)
                card.cardElevation = dp(2).toFloat()

                tv.setTextColor(text)
                tv.alpha = 0.95f
            }

            isCurrent -> {
                card.setCardBackgroundColor(surface70)
                card.strokeColor = gold
                card.strokeWidth = dp(3)
                card.cardElevation = dp(4).toFloat()

                tv.setTextColor(text)
                tv.alpha = 1f

                // brilho suave “vivo”
                card.animate().cancel()
                card.scaleX = 1f
                card.scaleY = 1f
                card.animate().scaleX(1.01f).scaleY(1.01f).setDuration(220).start()
            }

            else -> {
                card.setCardBackgroundColor(surface55)
                card.strokeColor = stroke
                card.strokeWidth = dp(1)
                card.cardElevation = 0f

                tv.setTextColor(textDim)
                tv.alpha = 0.55f
            }
        }
    }



    private fun pulseDoor(index: Int) {
        val v = when (index) {
            0 -> binding.door1
            1 -> binding.door2
            else -> binding.door3
        }
        v.animate().cancel()
        v.scaleX = 1f; v.scaleY = 1f
        v.animate()
            .scaleX(1.05f).scaleY(1.05f)
            .setDuration(140)
            .withEndAction {
                v.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
            }
            .start()
    }

    private fun shakeDoor(index: Int) {
        val v = when (index) {
            0 -> binding.door1
            1 -> binding.door2
            else -> binding.door3
        }
        val anim = ObjectAnimator.ofFloat(v, "translationX", 0f, 14f, -14f, 9f, -9f, 4f, -4f, 0f)
        anim.duration = 260
        anim.start()
    }

    // -------------------------
    // Stability timer + danger fx
    // -------------------------
    private fun startStabilityTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(60_000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                if (finished) return
                stability = (stability - 1).coerceAtLeast(0)
                updateStabilityUi()

                if (stability in 1..18) {
                    // “perigo” — micro tremor + flicker
                    microShake()
                    flickerStabilityText()
                }
                if (stability <= 0) fail("O portal colapsou.")
            }

            override fun onFinish() {
                if (!finished) fail("Tempo esgotado. O portal fechou.")
            }
        }.start()
    }

    private fun updateStabilityUi() {
        binding.progStability.max = 100
        binding.progStability.progress = stability
        binding.txtStability.text = "Estabilidade do Portal: $stability%"

        // cor dinâmica do indicador (AAA)
        val color = when {
            stability >= 55 -> ContextCompat.getColor(this, R.color.portal_gold)
            stability >= 30 -> ContextCompat.getColor(this, R.color.portal_amber)
            else -> ContextCompat.getColor(this, R.color.portal_red)
        }
        binding.progStability.setIndicatorColor(color)
    }

    private fun flickerStabilityText() {
        binding.txtStability.animate().cancel()
        binding.txtStability.alpha = 1f
        binding.txtStability.animate()
            .alpha(0.55f)
            .setDuration(70)
            .withEndAction { binding.txtStability.animate().alpha(1f).setDuration(110).start() }
            .start()
    }

    // -------------------------
    // Gameplay
    // -------------------------
    private fun onOption(index: Int) {
        val e = enigmas[stage]

        if (index == e.correct) {
            val solvedIndex = stage // ✅ guarda qual etapa foi concluída AGORA
            pulseCard()
            pulseDoor(solvedIndex)

            if (stage == enigmas.lastIndex) {
                win()
            } else {
                stage++
                renderStage()
            }
        } else {
            errorsLeft--
            stability = (stability - 22).coerceAtLeast(0)

            shakeCard()
            shakeDoor(stage) // ✅ treme a etapa atual
            updateStabilityUi()

            binding.txtLives.text = "Erros restantes: $errorsLeft"

            if (errorsLeft <= 0 || stability <= 0) {
                fail("Você errou demais. O portal fechou.")
            }
        }
    }

    private fun pulseCard() {
        binding.cardEnigma.animate().cancel()
        binding.cardEnigma.scaleX = 1f
        binding.cardEnigma.scaleY = 1f
        binding.cardEnigma.animate()
            .scaleX(1.02f).scaleY(1.02f)
            .setDuration(140)
            .withEndAction {
                binding.cardEnigma.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
            }
            .start()
    }

    private fun shakeCard() {
        val anim = ObjectAnimator.ofFloat(
            binding.cardEnigma,
            "translationX",
            0f, 16f, -16f, 10f, -10f, 6f, -6f, 0f
        )
        anim.duration = 320
        anim.start()
    }

    private fun microShake() {
        val a = abs((stability - 18) / 18f) // 0..1
        val amp = (2f + 6f * a).coerceIn(2f, 8f)
        binding.rootPortal.animate().cancel()
        binding.rootPortal.translationX = 0f
        binding.rootPortal.animate()
            .translationX(amp)
            .setDuration(40)
            .withEndAction {
                binding.rootPortal.animate()
                    .translationX(-amp)
                    .setDuration(40)
                    .withEndAction { binding.rootPortal.animate().translationX(0f).setDuration(60).start() }
                    .start()
            }
            .start()
    }

    // -------------------------
    // Attempts badge (3/dia)
    // -------------------------
    private fun updateAttemptsBadge() {
        val left = EnigmaPortalGate.attemptsLeftToday(this)
        binding.txtAttemptsBadge.text = "Tentativas: $left/3"
        binding.txtAttemptsBadge.alpha = if (left <= 1) 1f else 0.95f
    }

    // -------------------------
    // Win / Fail
    // -------------------------
    private fun win() {
        if (finished) return
        finished = true
        timer?.cancel()

        EnigmaPortalGate.markPlayedToday(this, win = true)
        EnigmaPortalGate.addRelic(this)

        val relics = EnigmaPortalGate.getRelicsCount(this)
        val left = EnigmaPortalGate.attemptsLeftToday(this)

        binding.overlayTop.visibility = View.VISIBLE
        binding.overlayTop.alpha = 0f
        binding.txtOverlay.text = "VITÓRIA!\nRelíquia obtida • Total: $relics\nTentativas: $left/3"

        binding.overlayTop.animate()
            .alpha(1f)
            .setDuration(320)
            .withEndAction {
                Toast.makeText(this, "Você dominou o Portal dos Enigmas!", Toast.LENGTH_LONG).show()
                finish()
            }
            .start()
    }

    private fun fail(msg: String) {
        if (finished) return
        finished = true
        timer?.cancel()

        EnigmaPortalGate.markPlayedToday(this, win = false)
        val left = EnigmaPortalGate.attemptsLeftToday(this)

        binding.overlayTop.visibility = View.VISIBLE
        binding.overlayTop.alpha = 0f
        binding.txtOverlay.text = "FALHOU\n$msg\nTentativas: $left/3"

        binding.overlayTop.animate()
            .alpha(1f)
            .setDuration(260)
            .withEndAction {
                Toast.makeText(this, "Portal fechado (tentativa consumida).", Toast.LENGTH_LONG).show()
                finish()
            }
            .start()
    }

    // -------------------------
    // Flash overlay (AAA)
    // -------------------------
    private fun flash(argb: Int) {
        val v = binding.flashOverlay
        v.setBackgroundColor(argb)
        v.visibility = View.VISIBLE
        v.alpha = 0f
        v.animate().cancel()
        v.animate()
            .alpha(1f)
            .setDuration(60)
            .withEndAction {
                v.animate()
                    .alpha(0f)
                    .setDuration(160)
                    .withEndAction { v.visibility = View.GONE }
                    .start()
            }
            .start()
    }

    private fun haptic(ms: Long) {
        val vib = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") vib.vibrate(ms)
        }
    }


    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()


}
