package com.desafiolgico.main

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityDailyChallengeBinding
import com.desafiolgico.model.Question
import com.desafiolgico.model.QuestionManager
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.applyEdgeToEdge
import com.desafiolgico.utils.applySystemBarsPadding
import com.google.android.material.button.MaterialButton
import kotlin.math.roundToInt

class DailyChallengeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyChallengeBinding
    private lateinit var questionManager: QuestionManager

    private val optionButtons = mutableListOf<MaterialButton>()
    private var dailyQuestions: List<Question> = emptyList()

    private var index = 0
    private var correctCount = 0
    private var visualScore = 0

    // Timer
    private var timer: CountDownTimer? = null
    private val perQuestionMillis = 15_000L
    private var remainingMillis = perQuestionMillis
    private var lastTickSecond = -1

    // Auto back
    private var autoBackRunnable: Runnable? = null

    // Breathing
    private var breathingAnim: ValueAnimator? = null

    // SFX
    private var soundPool: SoundPool? = null
    private var sfxClick = 0
    private var sfxCorrect = 0
    private var sfxWrong = 0
    private var sfxTick = 0
    private var sfxWin = 0

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Edge-to-edge primeiro (antes de inflar layout)
        applyEdgeToEdge(lightSystemBarIcons = false)

        binding = ActivityDailyChallengeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Insets no container de conteúdo (padding + system bars)
        binding.contentDaily.applySystemBarsPadding(applyTop = true, applyBottom = true)

        GameDataManager.init(this)
        initSfx()

        // ✅ Se já fez hoje: aviso rápido e volta
        if (GameDataManager.isDailyDone(this)) {
            Toast.makeText(this, getString(R.string.daily_come_back_tomorrow), Toast.LENGTH_SHORT).show()
            goBackToMain()
            return
        }

        questionManager = QuestionManager(LanguageHelper.getLanguage(this))

        optionButtons.clear()
        optionButtons.addAll(listOf(binding.opt1, binding.opt2, binding.opt3, binding.opt4))

        optionButtons.forEachIndexed { i, btn ->
            btn.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                playSfx(sfxClick)
                pop(btn)
                onAnswer(i)
            }
        }

        binding.btnBackMenu.setOnClickListener {
            playSfx(sfxClick)
            pop(binding.btnBackMenu)
            goBackToMain()
        }

        dailyQuestions = buildDailyQuestions()
        if (dailyQuestions.size < 3) {
            Toast.makeText(this, getString(R.string.daily_not_enough_questions), Toast.LENGTH_SHORT).show()
            goBackToMain()
            return
        }

        playEnterAnimations()
        showQuestion()
    }

    private fun buildDailyQuestions(): List<Question> {
        val unlocked = GameDataManager.getUnlockedLevels(this).toMutableSet()
        unlocked.add(GameDataManager.Levels.INICIANTE)

        return GameDataManager.getDailyQuestions(
            context = this,
            questionManager = questionManager,
            unlockedLevels = unlocked
        )
    }

    private fun showQuestion() {
        resetUIForQuestion()

        val q = dailyQuestions[index]
        binding.progressText.text = "${index + 1}/3"

        animateQuestionSwap(q.questionText)

        q.options.forEachIndexed { i, text ->
            if (i < optionButtons.size) {
                optionButtons[i].visibility = View.VISIBLE
                optionButtons[i].text = text
            }
        }
        for (i in q.options.size until optionButtons.size) {
            optionButtons[i].visibility = View.GONE
        }

        staggerOptions()
        startBreathing()
        startTimer()
    }

    private fun startTimer() {
        timer?.cancel()
        remainingMillis = perQuestionMillis
        lastTickSecond = -1

        binding.timerBar.max = 100
        setProgressSmooth(100)
        setTimerColorNormal()

        timer = object : CountDownTimer(perQuestionMillis, 100L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = millisUntilFinished

                val pct = ((millisUntilFinished.toDouble() / perQuestionMillis) * 100.0)
                    .roundToInt()
                    .coerceIn(0, 100)

                setProgressSmooth(pct)

                // fundo “fica mais tenso” no final
                val tension = 1f - (pct / 100f)
                binding.lottieBgParticles.alpha = 0.18f + (tension * 0.10f)

                when {
                    pct <= 10 -> {
                        setTimerColorDanger()
                        pulseTimerBar()
                    }
                    pct <= 20 -> setTimerColorWarn()
                    else -> setTimerColorNormal()
                }

                val secLeft = (millisUntilFinished / 1000L).toInt()
                if (secLeft in 1..5 && secLeft != lastTickSecond) {
                    lastTickSecond = secLeft

                    // tick acelera
                    val rate = 1f + ((5 - secLeft) * 0.08f)
                    playSfx(sfxTick, rate)

                    binding.timerBar.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    pulseTimerBar()
                }
            }

            override fun onFinish() {
                remainingMillis = 0L
                setProgressSmooth(0)
                onTimeUp()
            }
        }.start()
    }

    private fun setProgressSmooth(pct: Int) {
        if (Build.VERSION.SDK_INT >= 24) {
            binding.timerBar.setProgress(pct, true)
        } else {
            binding.timerBar.progress = pct
        }
    }

    private fun onTimeUp() {
        setOptionsEnabled(false)
        stopBreathing()

        playSfx(sfxWrong)
        flashOverlay(false)
        shakeView(binding.questionText)

        paintSelected(selected = -1, isCorrect = false, correctIndex = null)

        binding.rootDaily.postDelayed({ next() }, 700L)
    }

    private fun onAnswer(selectedIndex: Int) {
        timer?.cancel()
        setOptionsEnabled(false)
        stopBreathing()

        val q = dailyQuestions[index]
        val isCorrect = selectedIndex == q.correctAnswerIndex

        paintSelected(selected = selectedIndex, isCorrect = isCorrect, correctIndex = q.correctAnswerIndex)

        if (isCorrect) {
            playSfx(sfxCorrect)
            flashOverlay(true)
            popBig(optionButtons[selectedIndex])
            playConfetti()
            spawnFloatingText(optionButtons[selectedIndex], "+XP", 0xFF4CAF50.toInt())

            binding.progressText.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
            bounce(binding.progressText)

            correctCount++
            val timeBonus = ((remainingMillis.toDouble() / perQuestionMillis) * 10.0).roundToInt()
            visualScore += (10 + timeBonus)

            binding.rootDaily.postDelayed({ next() }, 520L)
        } else {
            playSfx(sfxWrong)
            flashOverlay(false)
            optionButtons[selectedIndex].performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            shakeView(optionButtons[selectedIndex])
            binding.rootDaily.postDelayed({ next() }, 850L)
        }
    }

    private fun next() {
        index++
        if (index >= 3) finishDaily() else showQuestion()
    }

    private fun finishDaily() {
        timer?.cancel()
        setOptionsEnabled(false)
        stopBreathing()

        val xpEarned = when (correctCount) {
            3 -> 30
            2 -> 20
            1 -> 10
            else -> 5
        }

        GameDataManager.addXP(this, xpEarned)
        GameDataManager.saveDailyResult(this, correctCount, visualScore, xpEarned)
        GameDataManager.markDailyDone(this)

        val badgeRes = when (correctCount) {
            3 -> R.drawable.gold_medal
            2 -> R.drawable.silver_medal
            else -> R.drawable.ic_trophy
        }

        showResultUI(badgeRes, xpEarned)
        setResult(RESULT_OK)
    }

    private fun showResultUI(badgeRes: Int, xpEarned: Int) {
        binding.gameArea.animate().cancel()
        binding.gameArea.animate()
            .alpha(0f)
            .translationY(18f)
            .setDuration(180)
            .withEndAction {
                binding.gameArea.visibility = View.GONE
                binding.subtitleDaily.visibility = View.GONE

                binding.resultBox.visibility = View.VISIBLE
                binding.resultBox.alpha = 0f
                binding.resultBox.translationY = 44f
                binding.resultBox.scaleX = 0.98f
                binding.resultBox.scaleY = 0.98f

                binding.badgeImage.visibility = View.VISIBLE
                binding.badgeImage.setImageResource(badgeRes)

                binding.resultTitle.text = getString(R.string.daily_result_title)
                binding.resultText.text = getString(
                    R.string.daily_result_text_format,
                    correctCount,
                    visualScore
                )
                binding.xpText.text = getString(
                    R.string.daily_xp_text_format,
                    xpEarned,
                    GameDataManager.getDailyStreak(this)
                )

                binding.comeBackTomorrowText.visibility = View.VISIBLE
                binding.comeBackTomorrowText.text = getString(R.string.daily_come_back_tomorrow)

                binding.btnBackMenu.visibility = View.VISIBLE
                binding.btnBackMenu.text = getString(R.string.daily_back_to_menu)
                binding.btnBackMenu.setOnClickListener { goBackToMain() }

                // Badge “boom”
                binding.badgeImage.scaleX = 0.6f
                binding.badgeImage.scaleY = 0.6f
                binding.badgeImage.alpha = 0f
                binding.badgeImage.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(420)
                    .setInterpolator(OvershootInterpolator(1.1f))
                    .start()

                binding.resultBox.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(320)
                    .setInterpolator(OvershootInterpolator(0.9f))
                    .withEndAction {
                        playSfx(sfxWin)
                        bounce(binding.btnBackMenu)
                        playConfetti()

                        // ✅ AAA: coroa no 3/3
                        if (correctCount == 3) playCrownPerfect()
                    }
                    .start()

                autoBackRunnable?.let { binding.rootDaily.removeCallbacks(it) }
                val r = Runnable { if (!isFinishing) goBackToMain() }
                autoBackRunnable = r
                binding.rootDaily.postDelayed(r, 2500L)
            }
            .start()
    }

    private fun playCrownPerfect() {
        val crown = binding.lottieCrown
        crown.visibility = View.VISIBLE
        crown.progress = 0f
        crown.scaleX = 0.85f
        crown.scaleY = 0.85f
        crown.alpha = 0f

        crown.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(240)
            .setInterpolator(OvershootInterpolator(1.2f))
            .withEndAction { crown.playAnimation() }
            .start()

        spawnFloatingText(binding.badgeImage, "PERFEITO!", 0xFFFFD700.toInt())
    }

    private fun resetUIForQuestion() {
        binding.resultBox.visibility = View.GONE
        binding.lottieCrown.visibility = View.GONE

        binding.gameArea.visibility = View.VISIBLE
        binding.gameArea.alpha = 1f
        binding.gameArea.translationY = 0f

        binding.subtitleDaily.visibility = View.VISIBLE

        val defaultTint = ContextCompat.getColorStateList(this, R.color.button_default)
        val white = ContextCompat.getColor(this, android.R.color.white)

        optionButtons.forEach {
            it.isEnabled = true
            it.alpha = 1f
            it.translationY = 0f
            it.backgroundTintList = defaultTint
            it.setTextColor(white)
        }
    }

    private fun setOptionsEnabled(enabled: Boolean) {
        optionButtons.forEach { it.isEnabled = enabled }
    }

    private fun paintSelected(selected: Int, isCorrect: Boolean, correctIndex: Int?) {
        val white = ContextCompat.getColor(this, android.R.color.white)
        val defaultTint = ContextCompat.getColorStateList(this, R.color.button_default)
        val correctTint = ContextCompat.getColorStateList(this, R.color.correctAnswerColor)
        val wrongTint = ContextCompat.getColorStateList(this, R.color.wrongAnswerColor)

        optionButtons.forEachIndexed { i, b ->
            b.backgroundTintList = defaultTint
            b.setTextColor(white)

            if (i == selected) {
                b.backgroundTintList = if (isCorrect) correctTint else wrongTint
            }

            // ✅ AAA: se errou, mostra a correta
            if (!isCorrect && selected != -1 && correctIndex != null && i == correctIndex) {
                b.backgroundTintList = correctTint
            }
        }
    }

    // ---------- ANIMAÇÕES ----------

    private fun playEnterAnimations() {
        val views = listOf(binding.titleDaily, binding.subtitleDaily, binding.progressText, binding.timerBar)
        views.forEach { it.alpha = 0f }

        binding.titleDaily.translationY = -28f
        binding.subtitleDaily.translationY = -18f

        binding.titleDaily.animate().alpha(1f).translationY(0f).setDuration(240).start()
        binding.subtitleDaily.animate().alpha(1f).translationY(0f).setStartDelay(80).setDuration(220).start()
        binding.progressText.animate().alpha(1f).setStartDelay(140).setDuration(200).start()
        binding.timerBar.animate().alpha(1f).setStartDelay(140).setDuration(200).start()
    }

    private fun animateQuestionSwap(newText: String) {
        binding.questionText.animate().cancel()
        binding.questionText.animate()
            .alpha(0f)
            .translationY(-12f)
            .setDuration(140)
            .withEndAction {
                binding.questionText.text = newText
                binding.questionText.translationY = 18f
                binding.questionText.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(220)
                    .setInterpolator(OvershootInterpolator(0.7f))
                    .start()
            }
            .start()
    }

    private fun staggerOptions() {
        optionButtons.forEachIndexed { i, b ->
            if (b.visibility != View.VISIBLE) return@forEachIndexed
            b.alpha = 0f
            b.translationY = 22f
            b.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(i * 70L)
                .setDuration(220)
                .setInterpolator(OvershootInterpolator(0.9f))
                .start()
        }
    }

    private fun pop(v: View) {
        v.animate().cancel()
        v.scaleX = 1f; v.scaleY = 1f
        v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).withEndAction {
            v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }.start()
    }

    private fun popBig(v: View) {
        v.animate().cancel()
        v.scaleX = 1f; v.scaleY = 1f
        v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(130).withEndAction {
            v.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
        }.start()
    }

    private fun bounce(v: View) {
        v.scaleX = 0.98f; v.scaleY = 0.98f
        v.animate().scaleX(1f).scaleY(1f)
            .setDuration(260)
            .setInterpolator(OvershootInterpolator(0.8f))
            .start()
    }

    private fun shakeView(v: View) {
        v.animate().cancel()
        val d = 10f
        v.translationX = 0f
        v.animate().translationX(d).setDuration(40).withEndAction {
            v.animate().translationX(-d).setDuration(40).withEndAction {
                v.animate().translationX(d * 0.7f).setDuration(40).withEndAction {
                    v.animate().translationX(0f).setDuration(60).start()
                }.start()
            }.start()
        }.start()
    }

    private fun pulseTimerBar() {
        binding.timerBar.animate().cancel()
        binding.timerBar.scaleX = 1f
        binding.timerBar.animate().scaleX(1.02f).setDuration(90).withEndAction {
            binding.timerBar.animate().scaleX(1f).setDuration(120).start()
        }.start()
    }

    private fun flashOverlay(success: Boolean) {
        val v = binding.flashOverlay
        v.visibility = View.VISIBLE
        v.alpha = 0f
        v.setBackgroundColor(if (success) 0x334CAF50 else 0x33F44336)

        v.animate().alpha(1f).setDuration(60).withEndAction {
            v.animate().alpha(0f).setDuration(140).withEndAction {
                v.visibility = View.GONE
            }.start()
        }.start()
    }

    private fun playConfetti() {
        val confetti = binding.lottieConfetti
        confetti.visibility = View.VISIBLE
        confetti.progress = 0f
        confetti.playAnimation()

        confetti.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                confetti.visibility = View.GONE
                confetti.removeAllAnimatorListeners()
            }
            override fun onAnimationCancel(animation: Animator) {
                confetti.visibility = View.GONE
                confetti.removeAllAnimatorListeners()
            }
        })
    }

    private fun spawnFloatingText(anchor: View, text: String, color: Int) {
        val root = binding.rootDaily as ConstraintLayout

        val tv = TextView(this).apply {
            this.text = text
            setTextColor(color)
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            alpha = 0f
        }
        root.addView(tv)

        val aLoc = IntArray(2)
        val rLoc = IntArray(2)
        anchor.getLocationOnScreen(aLoc)
        root.getLocationOnScreen(rLoc)

        tv.x = (aLoc[0] - rLoc[0]).toFloat() + anchor.width / 2f - 20f
        tv.y = (aLoc[1] - rLoc[1]).toFloat() - 18f

        tv.animate()
            .alpha(1f)
            .translationYBy(-60f)
            .setDuration(520)
            .withEndAction { root.removeView(tv) }
            .start()
    }

    private fun startBreathing() {
        stopBreathing()
        val v = binding.questionText
        breathingAnim = ValueAnimator.ofFloat(1f, 1.015f).apply {
            duration = 900
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                val s = it.animatedValue as Float
                v.scaleX = s
                v.scaleY = s
            }
            start()
        }
    }

    private fun stopBreathing() {
        breathingAnim?.cancel()
        breathingAnim = null
        binding.questionText.scaleX = 1f
        binding.questionText.scaleY = 1f
    }

    private fun setTimerColorNormal() {
        binding.timerBar.progressTintList =
            ContextCompat.getColorStateList(this, R.color.correctAnswerColor)
    }

    private fun setTimerColorWarn() {
        binding.timerBar.progressTintList =
            ContextCompat.getColorStateList(this, android.R.color.holo_orange_light)
    }

    private fun setTimerColorDanger() {
        binding.timerBar.progressTintList =
            ContextCompat.getColorStateList(this, R.color.wrongAnswerColor)
    }

    // ---------- SFX ----------

    private fun initSfx() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build()

        sfxClick = loadRawByName("click_sound")
        sfxCorrect = loadRawByName("sfx_correct")
        sfxWrong = loadRawByName("sfx_wrong")
        sfxTick = loadRawByName("sfx_tick")
        sfxWin = loadRawByName("sfx_win")
    }

    private fun loadRawByName(name: String): Int {
        val id = resources.getIdentifier(name, "raw", packageName)
        if (id == 0) return 0
        return soundPool?.load(this, id, 1) ?: 0
    }

    private fun playSfx(id: Int, rate: Float = 1f) {
        if (id == 0) return
        soundPool?.play(id, 1f, 1f, 1, 0, rate)
    }

    // ---------- NAV / CICLO ----------

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.resultBox.visibility == View.VISIBLE) goBackToMain()
        else super.onBackPressed()
    }

    private fun goBackToMain() {
        autoBackRunnable?.let { binding.rootDaily.removeCallbacks(it) }
        autoBackRunnable = null

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        finish()
    }

    override fun onPause() {
        super.onPause()
        timer?.cancel()
        stopBreathing()
        autoBackRunnable?.let { binding.rootDaily.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        stopBreathing()
        autoBackRunnable = null
        soundPool?.release()
        soundPool = null
    }
}
