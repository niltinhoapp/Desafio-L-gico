package com.desafiolgico.main

import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityEnigmaPortalBinding
import com.desafiolgico.utils.EnigmaPortalGate
import com.desafiolgico.utils.SecurePrefs
import com.google.android.material.card.MaterialCardView
import kotlin.math.abs

class EnigmaPortalActivity : AppCompatActivity() {

    companion object { const val EXTRA_SCORE = "extra_score" }

    // ---- Persistência do RUN (SecurePrefs) ----
    private companion object {
        private const val KEY_RUN_SAVED = "portal_run_saved"
        private const val KEY_RUN_STAGE = "portal_run_stage"
        private const val KEY_RUN_ERRORS = "portal_run_errors"
        private const val KEY_RUN_STABILITY = "portal_run_stability"
        private const val KEY_RUN_FINISHED = "portal_run_finished"
        private const val KEY_RUN_LAST_TS = "portal_run_last_ts" // anti “pause”
    }

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
    private var stability = 100 // 0..100
    private var finished = false

    private var timer: CountDownTimer? = null

    // ✅ trava anti clique duplo
    private var inputLocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnigmaPortalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarPortal.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbarPortal.setNavigationOnClickListener { finish() }

        val score = intent.getIntExtra(EXTRA_SCORE, 0)

        EnigmaPortalGate.touchToday(this)

        // segurança do score
        if (score < EnigmaPortalGate.requiredScore()) {
            Toast.makeText(
                this,
                "Portal bloqueado. Alcance ${EnigmaPortalGate.requiredScore()} pontos.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        // ✅ MELHOR: reserva tentativa ao entrar (ou continua run ativa)
        if (!EnigmaPortalGate.reserveAttemptIfNeeded(this)) {
            Toast.makeText(this, "O portal já foi usado 3x hoje. Volte amanhã.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupButtons()
        playIntro()

        // restore do run (evita reset exploit)
        if (!loadRunState()) {
            stage = 0
            errorsLeft = 2
            stability = 100
            finished = false
            saveRunState()
        }

        renderStage()
        updateAttemptsBadge()
        startStabilityTimer()
    }

    override fun onResume() {
        super.onResume()
        // ✅ anti “pausar o tempo”: desconta tempo fora do app
        if (!finished) applyElapsedPenalty()
        startStabilityTimer()
        updateAttemptsBadge()
    }

    override fun onPause() {
        super.onPause()
        timer?.cancel()
        saveRunState()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    // -------------------------
    // Intro
    // -------------------------
    private fun playIntro() {
        binding.overlayTop.visibility = View.VISIBLE
        binding.overlayTop.alpha = 0f
        binding.txtOverlay.text = "CÂMARA DOS ENIGMAS"

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
        binding.opt1.setOnClickListener { onOption(0) }
        binding.opt2.setOnClickListener { onOption(1) }
        binding.opt3.setOnClickListener { onOption(2) }
        binding.opt4.setOnClickListener { onOption(3) }
    }

    private fun setOptionsEnabled(enabled: Boolean) {
        binding.opt1.isEnabled = enabled
        binding.opt2.isEnabled = enabled
        binding.opt3.isEnabled = enabled
        binding.opt4.isEnabled = enabled
    }

    private fun lockInput(ms: Long = 350L) {
        inputLocked = true
        setOptionsEnabled(false)
        binding.rootPortal.postDelayed({
            inputLocked = false
            if (!finished) setOptionsEnabled(true)
        }, ms)
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

        setOptionsEnabled(true)
    }

    private fun updateDoorsUi() {
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
        ObjectAnimator.ofFloat(v, "translationX", 0f, 14f, -14f, 9f, -9f, 4f, -4f, 0f).apply {
            duration = 260
            start()
        }
    }

    // -------------------------
    // Timer / estabilidade (anti pause)
    // -------------------------
    private fun startStabilityTimer() {
        timer?.cancel()
        if (finished) return

        // 60s, mas a estabilidade é o “HP” (cai 1 por segundo)
        timer = object : CountDownTimer(60_000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                if (finished) return
                stability = (stability - 1).coerceAtLeast(0)
                updateStabilityUi()
                saveRunLastTs()

                if (stability in 1..18) {
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

    private fun applyElapsedPenalty() {
        val last = SecurePrefs.getLong(this, KEY_RUN_LAST_TS, System.currentTimeMillis())
        val now = System.currentTimeMillis()
        val deltaSec = ((now - last) / 1000L).toInt().coerceAtLeast(0)
        if (deltaSec <= 0) return

        // desconta “tempo fora”
        stability = (stability - deltaSec).coerceAtLeast(0)
        updateStabilityUi()
        saveRunLastTs()

        if (stability <= 0) {
            fail("O portal colapsou (tempo fora).")
        }
    }

    private fun updateStabilityUi() {
        binding.progStability.max = 100
        binding.progStability.progress = stability
        binding.txtStability.text = "Estabilidade do Portal: $stability%"

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

    private fun microShake() {
        val a = abs((stability - 18) / 18f)
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
    // Gameplay
    // -------------------------
    private fun onOption(index: Int) {
        if (finished || inputLocked) return
        lockInput()

        val e = enigmas[stage]

        if (index == e.correct) {
            flash(ContextCompat.getColor(this, R.color.portal_flash_success))
            haptic(30)

            // “respiro” leve no acerto
            stability = (stability + 6).coerceAtMost(100)

            val solvedIndex = stage
            pulseCard()
            pulseDoor(solvedIndex)

            if (stage == enigmas.lastIndex) {
                win()
            } else {
                stage++
                saveRunState()
                renderStage()
            }
        } else {
            flash(ContextCompat.getColor(this, R.color.portal_flash_fail))
            haptic(60)

            errorsLeft--

            // penalidade escalando por etapa (fica mais justo e “tenso”)
            val penalty = 14 + stage * 4 // 14, 18, 22
            stability = (stability - penalty).coerceAtLeast(0)

            shakeCard()
            shakeDoor(stage)
            updateStabilityUi()

            binding.txtLives.text = "Erros restantes: $errorsLeft"
            saveRunState()

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
        ObjectAnimator.ofFloat(
            binding.cardEnigma,
            "translationX",
            0f, 16f, -16f, 10f, -10f, 6f, -6f, 0f
        ).apply {
            duration = 320
            start()
        }
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

        EnigmaPortalGate.finishRun(this, win = true)

        val relics = EnigmaPortalGate.getRelicsCount(this)
        val left = EnigmaPortalGate.attemptsLeftToday(this)

        clearRunState()

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

        EnigmaPortalGate.finishRun(this, win = false)

        val left = EnigmaPortalGate.attemptsLeftToday(this)

        clearRunState()

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
    // Flash overlay
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
        val vib = getSystemService<Vibrator>()
        vib?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") it.vibrate(ms)
            }
        }
    }

    // -------------------------
    // SecurePrefs run state
    // -------------------------
    private fun saveRunLastTs() {
        SecurePrefs.putLong(this, KEY_RUN_LAST_TS, System.currentTimeMillis())
    }

    private fun saveRunState() {
        SecurePrefs.putBoolean(this, KEY_RUN_SAVED, true)
        SecurePrefs.putInt(this, KEY_RUN_STAGE, stage)
        SecurePrefs.putInt(this, KEY_RUN_ERRORS, errorsLeft)
        SecurePrefs.putInt(this, KEY_RUN_STABILITY, stability)
        SecurePrefs.putBoolean(this, KEY_RUN_FINISHED, finished)
        saveRunLastTs()
    }

    private fun loadRunState(): Boolean {
        val saved = SecurePrefs.getBoolean(this, KEY_RUN_SAVED, false)
        if (!saved) {
            saveRunLastTs()
            return false
        }

        stage = SecurePrefs.getInt(this, KEY_RUN_STAGE, 0).coerceIn(0, enigmas.lastIndex)
        errorsLeft = SecurePrefs.getInt(this, KEY_RUN_ERRORS, 2).coerceIn(0, 2)
        stability = SecurePrefs.getInt(this, KEY_RUN_STABILITY, 100).coerceIn(0, 100)
        finished = SecurePrefs.getBoolean(this, KEY_RUN_FINISHED, false)

        saveRunLastTs()
        return true
    }

    private fun clearRunState() {
        SecurePrefs.putBoolean(this, KEY_RUN_SAVED, false)
        SecurePrefs.remove(this, KEY_RUN_STAGE)
        SecurePrefs.remove(this, KEY_RUN_ERRORS)
        SecurePrefs.remove(this, KEY_RUN_STABILITY)
        SecurePrefs.remove(this, KEY_RUN_FINISHED)
        SecurePrefs.remove(this, KEY_RUN_LAST_TS)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
