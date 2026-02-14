package com.desafiolgico.main

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.text.method.ScrollingMovementMethod
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityEnigmaPortalBinding
import com.desafiolgico.databinding.ActivityExpertChallengeBinding
import com.desafiolgico.model.Question
import com.desafiolgico.model.QuestionManager
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.ScoreManager
import com.desafiolgico.utils.applyEdgeToEdge
import com.desafiolgico.utils.applySystemBarsPadding
import com.google.android.material.button.MaterialButton

class ExpertChallengeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpertChallengeBinding
    private lateinit var questionManager: QuestionManager
    private lateinit var scoreManager: ScoreManager

    private var correctSound: MediaPlayer? = null
    private var wrongSound: MediaPlayer? = null

    private var questions: List<Question> = emptyList()
    private var currentIndex = 0
    private val maxWrongAnswers = 3
    private var wrongAnswersCount = 0

    private var countDownTimer: CountDownTimer? = null
    private val totalTimeInMillis: Long = 45_000L
    private var remainingTimeInMillis: Long = totalTimeInMillis

    // Torre / Estrela
    private var towerMaxTravel = 0f
    private var currentTowerProgress = 0f

    private val optionButtons = mutableListOf<MaterialButton>()

    // Urgência do timer
    private var urgentPulseRunning = false
    private var urgentPulseAnim: AnimatorSet? = null
    private var urgentShakeAnim: ObjectAnimator? = null

    // “Breathing” da estrela no topo
    private var starBreathRunning = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Edge-to-edge UMA vez, antes do setContentView
        applyEdgeToEdge(lightSystemBarIcons = false)

        // ✅ Binding certo (Expert)
        binding = ActivityExpertChallengeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ padding de system bars no ROOT desta tela (ou no content container se existir)
        binding.root.applySystemBarsPadding(applyTop = true, applyBottom = true)

        GameDataManager.init(this)
        applySystemBarInsets()

        // ✅ Pergunta 100% legível (rola dentro do TextView)
        binding.questionTextView.apply {
            movementMethod = android.text.method.ScrollingMovementMethod()
            isVerticalScrollBarEnabled = false
            ellipsize = null
            // dica: deixa ilimitado e controla pelo layout/scroll, ou use um valor alto
            maxLines = 50
        }

        // ✅ Torre: calcula deslocamento máximo após layout
        binding.progressTower.post {
            val towerHeight = binding.towerBackground.height
            val starHeight = binding.starIndicator.height
            towerMaxTravel = (towerHeight - starHeight).coerceAtLeast(0).toFloat()
            updateTowerProgress(0f)
            updateStarStyle(0f)
        }

        correctSound = MediaPlayer.create(this, R.raw.sfx_correct)
        wrongSound = MediaPlayer.create(this, R.raw.sfx_wrong)

        questionManager = QuestionManager(LanguageHelper.getLanguage(this))
        scoreManager = ScoreManager(this)

        binding.btnBack.setOnClickListener {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
            finish()
        }

        setupOptions()
        loadQuestions()
    }

    private fun applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootExpertLayout) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(dp(16), dp(16) + sys.top, dp(16), dp(16))
            insets
        }
    }

    private fun setupOptions() {
        val topMarginPx = dp(8)

        repeat(4) { index ->
            val btn = MaterialButton(
                this,
                null,
                com.google.android.material.R.attr.materialButtonStyle
            ).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = topMarginPx }

                // seu botão 3D
                setBackgroundResource(R.drawable.bg_expert_option_button)
                stateListAnimator = null
                backgroundTintList = null

                setTextColor(ContextCompat.getColor(this@ExpertChallengeActivity, android.R.color.white))
                textSize = 16f
                isAllCaps = false

                // ✅ press premium (toque)
                setOnTouchListener { v, ev ->
                    when (ev.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.animate().cancel()
                            v.animate().scaleX(0.985f).scaleY(0.985f)
                                .setDuration(80).setInterpolator(DecelerateInterpolator()).start()
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.animate().cancel()
                            v.animate().scaleX(1f).scaleY(1f)
                                .setDuration(120).setInterpolator(OvershootInterpolator(0.9f)).start()
                        }
                    }
                    false
                }

                setOnClickListener { checkAnswer(index) }
            }

            optionButtons.add(btn)
            binding.optionsContainer.addView(btn)
        }
    }

    private fun loadQuestions() {
        questions = questionManager.getQuestionsByLevel(GameDataManager.Levels.EXPERIENTE)
        if (questions.isEmpty()) {
            finish()
            return
        }
        currentIndex = 0
        wrongAnswersCount = 0
        showCurrentQuestion(first = true)
    }

    private fun showCurrentQuestion(first: Boolean = false) {
        if (currentIndex >= questions.size) {
            navigateToResult()
            return
        }

        val q = questions[currentIndex]

        // ✅ Transição premium
        if (first) {
            binding.questionTextView.alpha = 0f
            binding.optionsContainer.alpha = 0f
        }

        binding.progressTextView.text = getString(
            R.string.expert_progress_format,
            currentIndex + 1,
            questions.size
        )

        updateErrorsUI()

        q.options.forEachIndexed { i, opt ->
            if (i < optionButtons.size) {
                optionButtons[i].visibility = View.VISIBLE
                optionButtons[i].text = opt
            }
        }
        for (i in q.options.size until optionButtons.size) {
            optionButtons[i].visibility = View.GONE
        }

        resetOptionStyles()
        setOptionsEnabled(true)

        // texto e scroll reset
        binding.questionTextView.text = q.questionText
        binding.questionTextView.scrollTo(0, 0)

        // fade-in suave
        binding.questionTextView.animate().cancel()
        binding.optionsContainer.animate().cancel()

        binding.questionTextView.animate()
            .alpha(1f)
            .setDuration(if (first) 220L else 160L)
            .setInterpolator(DecelerateInterpolator())
            .start()

        binding.optionsContainer.animate()
            .alpha(1f)
            .setDuration(if (first) 260L else 180L)
            .setStartDelay(if (first) 60L else 0L)
            .setInterpolator(DecelerateInterpolator())
            .start()

        startTimer()
    }

    private fun startTimer() {
        countDownTimer?.cancel()
        remainingTimeInMillis = totalTimeInMillis

        binding.timerProgressBar.max = 100
        binding.timerProgressBar.progress = 100
        stopUrgentPulse()
        updateTimerBar(remainingTimeInMillis)

        countDownTimer = object : CountDownTimer(totalTimeInMillis, 100L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeInMillis = millisUntilFinished
                updateTimerBar(millisUntilFinished)

                // ✅ últimos 7s = modo urgência (pulse + shake leve)
                if (millisUntilFinished <= 7_000L) startUrgentPulse() else stopUrgentPulse()
            }

            override fun onFinish() {
                remainingTimeInMillis = 0
                updateTimerBar(0)
                startUrgentPulse() // mantém vibe de urgência até trocar
                handleTimeUp()
            }
        }.start()
    }

    // ✅ Verde -> Amarelo -> Vermelho + feel premium
    private fun updateTimerBar(remainingMs: Long) {
        val ratio = (remainingMs.toFloat() / totalTimeInMillis.toFloat()).coerceIn(0f, 1f)
        binding.timerProgressBar.progress = (ratio * 100f).toInt()

        val green = 0xFF00E676.toInt()
        val yellow = 0xFFFFD54F.toInt()
        val red = 0xFFFF5252.toInt()

        val color = if (ratio >= 0.5f) {
            val t = (ratio - 0.5f) / 0.5f
            ColorUtils.blendARGB(yellow, green, t)
        } else {
            val t = ratio / 0.5f
            ColorUtils.blendARGB(red, yellow, t)
        }

        binding.timerProgressBar.progressTintList = ColorStateList.valueOf(color)

        // micro feedback visual no vermelho
        if (ratio <= 0.22f) {
            binding.timerProgressBar.alpha = 0.98f
        } else {
            binding.timerProgressBar.alpha = 1f
        }
    }

    private fun startUrgentPulse() {
        if (urgentPulseRunning) return
        urgentPulseRunning = true

        // pulse vertical sutil (barra fica mais “viva”)
        val bar = binding.timerProgressBar
        bar.pivotY = bar.height.toFloat()

        val pulse = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(bar, View.SCALE_Y, 1f, 1.15f, 1f).setDuration(520L),
                ObjectAnimator.ofFloat(bar, View.ALPHA, 1f, 0.78f, 1f).setDuration(520L)
            )
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (urgentPulseRunning) startUrgentPulse()
                }
            })
        }
        urgentPulseAnim = pulse
        pulse.start()

        // shake super leve no container (quase imperceptível, mas dá tensão)
        val shake = ObjectAnimator.ofFloat(binding.questionTextView, View.TRANSLATION_X, 0f, -2f, 2f, -1.5f, 1.5f, 0f)
        shake.duration = 520L
        shake.interpolator = DecelerateInterpolator()
        urgentShakeAnim = shake
        shake.start()
    }

    private fun stopUrgentPulse() {
        urgentPulseRunning = false
        urgentPulseAnim?.cancel()
        urgentPulseAnim = null
        urgentShakeAnim?.cancel()
        urgentShakeAnim = null
        binding.timerProgressBar.scaleY = 1f
        binding.timerProgressBar.alpha = 1f
        binding.questionTextView.translationX = 0f
    }

    private fun handleTimeUp() {
        setOptionsEnabled(false)
        wrongAnswersCount++
        updateErrorsUI()

        val isGameOver = wrongAnswersCount >= maxWrongAnswers

        binding.rootExpertLayout.postDelayed({
            if (isGameOver) {
                navigateToResult(isGameOver = true)
            } else {
                currentIndex++
                showCurrentQuestion()
            }
        }, 900L)
    }

    private fun updateErrorsUI() {
        binding.errorsTextView.text = getString(
            R.string.expert_errors_format,
            wrongAnswersCount,
            maxWrongAnswers
        )
    }

    private fun checkAnswer(selectedIndex: Int) {
        if (currentIndex >= questions.size) return

        setOptionsEnabled(false)
        countDownTimer?.cancel()
        stopUrgentPulse()

        val q = questions[currentIndex]
        val isCorrect = selectedIndex == q.correctAnswerIndex

        // ✅ pinta só selecionado
        paintButtonsForAnswer(selectedIndex, isCorrect)

        val selectedButton = optionButtons.getOrNull(selectedIndex)

        // haptic premium (bem leve)
        selectedButton?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

        if (isCorrect) {
            correctSound?.start()

            val progressFraction = (currentIndex + 1).toFloat() / questions.size.toFloat()

            // ✅ estrela sobe com zoom + rotação + glow premium
            animateTowerTo(progressFraction)

            selectedButton?.let { animateCorrectAnswer(it) }

            selectedButton?.let {
                animateCorrectAnswer(it)
                playShineSweep(it)   // ✅ brilho varrendo (AAA)
            }


            scoreManager.addScore(
                remainingTimeInMillis = remainingTimeInMillis,
                totalTimeInMillis = totalTimeInMillis
            )

            // ✅ mapa (experiente)
            GameDataManager.addCorrectForLevel(this, GameDataManager.Levels.EXPERIENTE, 1)

        } else {
            wrongSound?.start()
            selectedButton?.let { animateWrongAnswer(it) }

            wrongAnswersCount++
            scoreManager.onWrongAnswer()
            updateErrorsUI()
        }

        val isGameOver = wrongAnswersCount >= maxWrongAnswers

        binding.rootExpertLayout.postDelayed({
            if (isGameOver) {
                navigateToResult(isGameOver = true)
            } else {
                currentIndex++
                showCurrentQuestion()
            }
        }, if (isCorrect) 800L else 1200L)
    }

    private fun paintButtonsForAnswer(selected: Int, isCorrect: Boolean) {
        val white = ContextCompat.getColor(this, android.R.color.white)
        val correctTint = ContextCompat.getColorStateList(this, R.color.correctAnswerColor)
        val wrongTint = ContextCompat.getColorStateList(this, R.color.wrongAnswerColor)

        optionButtons.forEachIndexed { i, b ->
            b.setBackgroundResource(R.drawable.bg_expert_option_button)
            b.backgroundTintList = null
            b.setTextColor(white)

            if (i == selected) {
                b.backgroundTintList = if (isCorrect) correctTint else wrongTint
                b.setTextColor(white)
            }
        }
    }

    private fun animateCorrectAnswer(view: View) {
        val set = AnimatorSet()
        set.playTogether(
            ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.08f, 1f),
            ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.08f, 1f)
        )
        set.duration = 260L
        set.interpolator = OvershootInterpolator(0.9f)
        set.start()
    }

    private fun animateWrongAnswer(view: View) {
        ObjectAnimator.ofFloat(
            view,
            View.TRANSLATION_X,
            0f, -18f, 18f, -14f, 14f, -8f, 8f, 0f
        ).apply { duration = 320L }.start()
    }

    private fun resetOptionStyles() {
        val white = ContextCompat.getColor(this, android.R.color.white)
        optionButtons.forEach { b ->
            b.setBackgroundResource(R.drawable.bg_expert_option_button)
            b.backgroundTintList = null
            b.translationX = 0f
            b.scaleX = 1f
            b.scaleY = 1f
            b.setTextColor(white)
        }
    }

    private fun setOptionsEnabled(enabled: Boolean) {
        optionButtons.forEach { it.isEnabled = enabled }
    }

    private fun updateTowerProgress(progress: Float) {
        currentTowerProgress = progress.coerceIn(0f, 1f)
        val translationY = -towerMaxTravel * currentTowerProgress
        binding.starIndicator.translationY = translationY
        binding.starGlow.translationY = translationY
    }

    private fun animateTowerTo(targetProgress: Float) {
        val clamped = targetProgress.coerceIn(0f, 1f)

        val move = ValueAnimator.ofFloat(currentTowerProgress, clamped).apply {
            duration = 560L
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val p = anim.animatedValue as Float
                updateTowerProgress(p)
                updateStarStyle(p)
            }
        }

        val star = binding.starIndicator

        val zoomX = ObjectAnimator.ofFloat(star, View.SCALE_X, 1f, 1.22f, 1f).apply {
            duration = 560L
            interpolator = OvershootInterpolator(0.85f)
        }
        val zoomY = ObjectAnimator.ofFloat(star, View.SCALE_Y, 1f, 1.22f, 1f).apply {
            duration = 560L
            interpolator = OvershootInterpolator(0.85f)
        }

        val rotate = ObjectAnimator.ofFloat(star, View.ROTATION, 0f, -8f, 6f, 0f).apply {
            duration = 560L
            interpolator = DecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(move, zoomX, zoomY, rotate)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    star.rotation = 0f
                    playTowerGlow()
                    updateStarBreathing(clamped)
                }
            })
            start()
        }
    }

    // ✅ Outline -> dourado -> filled (premium)
    private fun updateStarStyle(progress: Float) {
        val p = progress.coerceIn(0f, 1f)

        when {
            p < 0.34f -> {
                binding.starIndicator.setImageResource(R.drawable.ic_star_outline)
                binding.starIndicator.imageTintList = ColorStateList.valueOf(0xCCFFFFFF.toInt())
            }
            p < 0.67f -> {
                binding.starIndicator.setImageResource(R.drawable.ic_star_outline)
                binding.starIndicator.imageTintList = ColorStateList.valueOf(0xFFFFD54F.toInt()) // dourado
            }
            else -> {
                binding.starIndicator.setImageResource(R.drawable.ic_star_filled)
                binding.starIndicator.imageTintList = ColorStateList.valueOf(0xFFFFD54F.toInt()) // dourado
            }
        }
    }

    private fun updateStarBreathing(progress: Float) {
        val shouldBreath = progress >= 0.67f
        if (shouldBreath && !starBreathRunning) startStarBreath()
        if (!shouldBreath && starBreathRunning) stopStarBreath()
    }

    private fun startStarBreath() {
        starBreathRunning = true
        val star = binding.starIndicator
        star.animate().cancel()

        fun loop() {
            if (!starBreathRunning) return
            star.animate()
                .scaleX(1.06f).scaleY(1.06f)
                .setDuration(650L)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    if (!starBreathRunning) return@withEndAction
                    star.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(650L)
                        .setInterpolator(DecelerateInterpolator())
                        .withEndAction { loop() }
                        .start()
                }
                .start()
        }
        loop()
    }

    private fun stopStarBreath() {
        starBreathRunning = false
        val star = binding.starIndicator
        star.animate().cancel()
        star.scaleX = 1f
        star.scaleY = 1f
    }

    private fun playTowerGlow() {
        val glowView = binding.starGlow

        glowView.visibility = View.VISIBLE
        glowView.alpha = 0f
        glowView.scaleX = 0.55f
        glowView.scaleY = 0.55f

        val scaleX = ObjectAnimator.ofFloat(glowView, View.SCALE_X, 0.55f, 1.65f)
        val scaleY = ObjectAnimator.ofFloat(glowView, View.SCALE_Y, 0.55f, 1.65f)
        val alpha = ObjectAnimator.ofFloat(glowView, View.ALPHA, 0f, 1f, 0f)

        AnimatorSet().apply {
            duration = 520L
            interpolator = DecelerateInterpolator()
            playTogether(scaleX, scaleY, alpha)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    glowView.visibility = View.GONE
                }
            })
            start()
        }
    }

    private fun navigateToResult(isGameOver: Boolean = false) {
        countDownTimer?.cancel()
        stopUrgentPulse()
        stopStarBreath()

        val finalScore = scoreManager.getOverallScore()
        val screenType = if (isGameOver) "GAME_OVER" else "RESULT"

        val intent = Intent(this, ResultOrGameOverActivity::class.java).apply {
            putExtra("SCREEN_TYPE", screenType)
            putExtra("SCORE", finalScore)
            putExtra("TOTAL_QUESTIONS", questions.size)
            putExtra("WRONG_ANSWERS", wrongAnswersCount)
            putExtra("MAX_WRONG_ANSWERS", maxWrongAnswers)
            putExtra("AVERAGE_TIME", 0.0)
        }
        startActivity(intent)
        finish()
    }

    override fun onPause() {
        super.onPause()
        countDownTimer?.cancel()
        stopUrgentPulse()
        stopStarBreath()
    }

    override fun onDestroy() {
        super.onDestroy()
        correctSound?.release()
        wrongSound?.release()
        correctSound = null
        wrongSound = null
    }
    // --- AAA: Shine sweep (brilho que varre o botão) ----------------------------

    private fun playShineSweep(target: View) {
        // garante que já mediu
        if (target.width == 0 || target.height == 0) {
            target.post { playShineSweep(target) }
            return
        }

        val shine = ShineDrawable().apply {
            setBounds(0, 0, target.width, target.height)
        }

        target.overlay.add(shine)

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 520L
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                shine.progress = anim.animatedValue as Float
                // força redraw (overlay)
                target.invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    target.overlay.remove(shine)
                }
                override fun onAnimationCancel(animation: Animator) {
                    target.overlay.remove(shine)
                }
            })
            start()
        }
    }

    /**
     * Drawable que desenha um "raio" diagonal com gradiente e anima via progress (0..1).
     * Desenha dentro do botão (overlay), sem mexer em backgrounds/tints.
     */
    private class ShineDrawable : android.graphics.drawable.Drawable() {

        private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
        }

        private val shaderMatrix = android.graphics.Matrix()

        // 0..1 (vai da esquerda pra direita)
        var progress: Float = 0f
            set(value) {
                field = value.coerceIn(0f, 1f)
                invalidateSelf()
            }

        override fun draw(canvas: android.graphics.Canvas) {
            val b = bounds
            if (b.width() <= 0 || b.height() <= 0) return

            val w = b.width().toFloat()
            val h = b.height().toFloat()

            // largura do feixe de brilho
            val band = w * 0.28f

            // gradiente horizontal (a gente vai rotacionar o canvas depois)
            val colors = intArrayOf(
                android.graphics.Color.argb(0, 255, 255, 255),
                android.graphics.Color.argb(0, 255, 255, 255),
                android.graphics.Color.argb(200, 255, 255, 255),
                android.graphics.Color.argb(0, 255, 255, 255),
                android.graphics.Color.argb(0, 255, 255, 255)
            )
            val stops = floatArrayOf(0f, 0.35f, 0.5f, 0.65f, 1f)

            val shader = android.graphics.LinearGradient(
                -band, 0f,
                band, 0f,
                colors, stops,
                android.graphics.Shader.TileMode.CLAMP
            )

            // move o feixe de brilho ao longo do X
            val travel = w + band * 2f
            val x = (-band) + travel * progress

            shaderMatrix.reset()
            shaderMatrix.setTranslate(x, 0f)
            shader.setLocalMatrix(shaderMatrix)

            paint.shader = shader

            // desenha diagonal (varrendo)
            canvas.save()

            // rotaciona no centro (diagonal premium)
            canvas.rotate(-18f, w / 2f, h / 2f)

            // desenha um retângulo alto o suficiente pra cobrir ao rotacionar
            val extra = h * 1.5f
            canvas.drawRect(0f, -extra, w, h + extra, paint)

            canvas.restore()
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
            invalidateSelf()
        }

        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
            paint.colorFilter = colorFilter
            invalidateSelf()
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
    }


    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
