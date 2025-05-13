package com.desafiolgico.main

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityExpertChallengeBinding
import com.desafiolgico.model.Question
import com.desafiolgico.model.QuestionManager
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.ScoreManager
import com.desafiolgico.utils.applyEdgeToEdge
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

    // Torre / Estrela
    private var towerMaxTravel = 0f
    private var currentTowerProgress = 0f
    private var remainingTimeInMillis: Long = totalTimeInMillis

    private val optionButtons = mutableListOf<MaterialButton>()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()
        binding = ActivityExpertChallengeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        GameDataManager.init(this)

        binding.progressTower.post {
            val towerHeight = binding.towerBackground.height
            val starHeight = binding.starIndicator.height
            towerMaxTravel = (towerHeight - starHeight).coerceAtLeast(0).toFloat()
            updateTowerProgress(0f)
        }

        correctSound = MediaPlayer.create(this, R.raw.sfx_correct)
        wrongSound = MediaPlayer.create(this, R.raw.sfx_wrong)

        questionManager = QuestionManager(LanguageHelper.getLanguage(this))
        scoreManager = ScoreManager(this)

        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupOptions()
        loadQuestions()
    }

    private fun setupOptions() {
        val topMarginPx = (8 * resources.displayMetrics.density).toInt()

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

                // fundo 3D do modo experiente
                setBackgroundResource(R.drawable.bg_expert_option_button)

                // evita "briga" do Material com o drawable 3D
                stateListAnimator = null

                // ✅ padrão do modo experiente: texto branco
                setTextColor(ContextCompat.getColor(this@ExpertChallengeActivity, android.R.color.white))
                textSize = 16f
                isAllCaps = false

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
        showCurrentQuestion()
    }

    private fun showCurrentQuestion() {
        if (currentIndex >= questions.size) {
            navigateToResult()
            return
        }

        val q = questions[currentIndex]
        binding.questionTextView.text = q.questionText

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
        startTimer()
    }

    private fun startTimer() {
        countDownTimer?.cancel()
        remainingTimeInMillis = totalTimeInMillis

        binding.timerProgressBar.max = 100
        binding.timerProgressBar.progress = 100

        countDownTimer = object : CountDownTimer(totalTimeInMillis, 100L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeInMillis = millisUntilFinished
                val percent = (millisUntilFinished * 100 / totalTimeInMillis)
                    .toInt()
                    .coerceIn(0, 100)
                binding.timerProgressBar.progress = percent
            }

            override fun onFinish() {
                remainingTimeInMillis = 0
                binding.timerProgressBar.progress = 0
                handleTimeUp()
            }
        }.start()
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

        val q = questions[currentIndex]
        val isCorrect = selectedIndex == q.correctAnswerIndex

        // ✅ Regra: pinta SOMENTE o botão escolhido
        paintButtonsForAnswer(selectedIndex, isCorrect)

        val selectedButton = optionButtons.getOrNull(selectedIndex)

        if (isCorrect) {
            correctSound?.start()

            val progressFraction = (currentIndex + 1).toFloat() / questions.size.toFloat()
            animateTowerTo(progressFraction)

            selectedButton?.let { animateCorrectAnswer(it) }

            scoreManager.addScore(
                remainingTimeInMillis = remainingTimeInMillis,
                totalTimeInMillis = totalTimeInMillis
            )
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

    /**
     * ✅ Regra decidida:
     * - Se acertou: só o botão selecionado fica verde
     * - Se errou: só o botão selecionado fica vermelho
     * - Os outros voltam ao estilo 3D padrão
     */
    private fun paintButtonsForAnswer(selected: Int, isCorrect: Boolean) {
        val white = ContextCompat.getColor(this, android.R.color.white)
        val correctTint = ContextCompat.getColorStateList(this, R.color.correctAnswerColor)
        val wrongTint = ContextCompat.getColorStateList(this, R.color.wrongAnswerColor)

        optionButtons.forEachIndexed { i, b ->
            // volta ao fundo 3D original (sem tint)
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
        val x = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.07f, 1f).apply {
            duration = 250L
            interpolator = DecelerateInterpolator()
        }
        val y = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.07f, 1f).apply {
            duration = 250L
            interpolator = DecelerateInterpolator()
        }
        x.start(); y.start()
    }

    private fun animateWrongAnswer(view: View) {
        ObjectAnimator.ofFloat(
            view,
            "translationX",
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
        ValueAnimator.ofFloat(currentTowerProgress, clamped).apply {
            duration = 500L
            addUpdateListener { updateTowerProgress(it.animatedValue as Float) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    playTowerGlow()
                }
            })
            start()
        }
    }

    private fun playTowerGlow() {
        val glowView = binding.starGlow

        glowView.visibility = View.VISIBLE
        glowView.alpha = 0f
        glowView.scaleX = 0.6f
        glowView.scaleY = 0.6f

        val scaleX = ObjectAnimator.ofFloat(glowView, View.SCALE_X, 0.6f, 1.5f)
        val scaleY = ObjectAnimator.ofFloat(glowView, View.SCALE_Y, 0.6f, 1.5f)
        val alpha = ObjectAnimator.ofFloat(glowView, View.ALPHA, 0f, 1f, 0f)

        AnimatorSet().apply {
            duration = 450L
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
    }

    override fun onDestroy() {
        super.onDestroy()
        correctSound?.release()
        wrongSound?.release()
        correctSound = null
        wrongSound = null
    }
}
