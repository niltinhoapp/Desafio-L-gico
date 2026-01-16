package com.desafiolgico.main

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityResultOrGameOverBinding
import com.desafiolgico.databinding.ContainerGameOverBinding
import com.desafiolgico.databinding.ContainerResultBinding
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.VictoryFx
import com.desafiolgico.utils.applyEdgeToEdge
import nl.dionsegijn.konfetti.xml.KonfettiView

class ResultOrGameOverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultOrGameOverBinding
    private lateinit var resultBinding: ContainerResultBinding
    private lateinit var gameOverBinding: ContainerGameOverBinding

    private lateinit var levelManager: LevelManager
    private lateinit var konfettiView: KonfettiView

    private var mediaPlayer: MediaPlayer? = null

    private val wrongAnswers by lazy { intent.getIntExtra(EXTRA_WRONG_ANSWERS, 0) }
    private val maxWrongAnswers by lazy { intent.getIntExtra(EXTRA_MAX_WRONG_ANSWERS, 3) }
    private val totalQuestions by lazy { intent.getIntExtra(EXTRA_TOTAL_QUESTIONS, 48) }

    private val returnToActiveGame by lazy {
        intent.getBooleanExtra(EXTRA_RETURN_TO_ACTIVE_GAME, false)
    }

    companion object {
        const val SCREEN_TYPE_RESULT = "RESULT"
        const val SCREEN_TYPE_GAME_OVER = "GAME_OVER"

        const val EXTRA_SCREEN_TYPE = "SCREEN_TYPE"
        const val EXTRA_SCORE = "SCORE"
        const val EXTRA_AVERAGE_TIME = "AVERAGE_TIME"
        const val EXTRA_LEVEL = "level"
        const val EXTRA_WRONG_ANSWERS = "WRONG_ANSWERS"
        const val EXTRA_MAX_WRONG_ANSWERS = "MAX_WRONG_ANSWERS"
        const val EXTRA_TOTAL_QUESTIONS = "TOTAL_QUESTIONS"
        const val EXTRA_RETURN_TO_ACTIVE_GAME = "RETURN_TO_ACTIVE_GAME"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()

        binding = ActivityResultOrGameOverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ includes já vêm como bindings (no seu caso)
        resultBinding = binding.resultContainer
        gameOverBinding = binding.gameOverContainer

        konfettiView = findViewById(R.id.konfettiView)
        levelManager = LevelManager(this)

        Log.d("ResultDBG", "returnToActiveGame=$returnToActiveGame level=${intent.getStringExtra(EXTRA_LEVEL)}")

        setupScreen()
    }

    private fun setupScreen() {
        val screenType = intent.getStringExtra(EXTRA_SCREEN_TYPE) ?: SCREEN_TYPE_RESULT
        val score = intent.getIntExtra(EXTRA_SCORE, 0)
        val averageTime = intent.getDoubleExtra(EXTRA_AVERAGE_TIME, 0.0)

        when (screenType) {
            SCREEN_TYPE_GAME_OVER -> showGameOverScreen(score)
            SCREEN_TYPE_RESULT -> showResultScreen(score, averageTime)
            else -> showResultScreen(score, averageTime)
        }
    }

    // ========================= RESULT =========================

    @SuppressLint("SetTextI18n")
    private fun showResultScreen(score: Int, averageTime: Double) {
        resultBinding.root.visibility = View.VISIBLE
        gameOverBinding.root.visibility = View.GONE
        konfettiView.visibility = View.GONE

        levelManager.checkAndSaveLevelUnlocks()

        val performance = calculatePerformance()
        val (baseFeedbackText, baseSoundResId, medalResId) = getFeedbackResources(performance)

        var finalFeedbackText = baseFeedbackText
        var finalSoundResId = baseSoundResId

        val currentLevelName = intent.getStringExtra(EXTRA_LEVEL)

        if (returnToActiveGame) {
            if (performance >= 75.0) {
                finalFeedbackText = getString(R.string.fase_secreta_sucesso_prefix) + "\n" + baseFeedbackText
            } else {
                finalFeedbackText = getString(R.string.fase_secreta_falha_prefix)
                finalSoundResId = R.raw.game_over_sound1
            }
            currentLevelName?.let { GameDataManager.clearLastQuestionIndex(this, it) }
        }

        resultBinding.scoreTextView.text =
            getString(R.string.result_points_format, score, totalQuestions)

        resultBinding.feedbackTextView.text = finalFeedbackText

        resultBinding.statsTextView.text = getString(
            R.string.result_stats_format,
            averageTime,
            performance
        )

        medalResId?.let {
            Glide.with(this).load(it).into(resultBinding.medalImageView)
        }

        animateResultViews()
        playFeedbackSound(finalSoundResId)

        if (performance >= 75.0 && !returnToActiveGame) {
            val level = currentLevelName.orEmpty()
            val isEnigma = level == GameDataManager.SecretLevels.ENIGMA
            val isExperiente = level == GameDataManager.Levels.EXPERIENTE

            if (isEnigma || isExperiente) {
                VictoryFx.play(this, konfettiView)
            } else {
                showVictoryCelebration()
            }
        }

        resultBinding.retryButton.setOnClickListener { navigateAfterResult() }
    }

    private fun animateResultViews() {
        val fadeDuration = 800L
        listOf(
            resultBinding.medalImageView,
            resultBinding.feedbackTextView,
            resultBinding.scoreTextView,
            resultBinding.statsTextView,
            resultBinding.retryButton
        ).forEachIndexed { index, view ->
            view.alpha = 0f
            view.scaleX = 0.9f
            view.scaleY = 0.9f
            view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(index * 120L)
                .setDuration(fadeDuration)
                .start()
        }
    }

    private fun showVictoryCelebration() {
        try {
            val inspirationBox = resultBinding.root.findViewById<View>(R.id.inspirationBox)
            val inspirationText = resultBinding.root.findViewById<TextView>(R.id.inspirationText)

            inspirationBox.visibility = View.VISIBLE
            inspirationBox.alpha = 0f
            inspirationBox.animate().alpha(1f).setDuration(1000L).start()

            ObjectAnimator.ofFloat(inspirationBox, "alpha", 0.8f, 1f).apply {
                duration = 2000
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                start()
            }

            inspirationBox.postDelayed({
                inspirationText.clearAnimation()
                inspirationBox.visibility = View.GONE
            }, 8000L)
        } catch (e: Exception) {
            Log.w("ResultOrGameOver", "Falha ao exibir celebração", e)
        }
    }

    // ========================= GAME OVER =========================

    private fun showGameOverScreen(currentRoundScore: Int) {
        resultBinding.root.visibility = View.GONE
        gameOverBinding.root.visibility = View.VISIBLE
        konfettiView.visibility = View.GONE

        val safeScore = currentRoundScore.coerceAtLeast(0)

        val totalAcumulado = GameDataManager.getOverallTotalScore(this)
        gameOverBinding.totalScoreTextView.text =
            getString(R.string.pontuacao_total_format, totalAcumulado)

        gameOverBinding.gameOverFeedbackTextView.text =
            getFeedbackMessage(safeScore, wrongAnswers, maxWrongAnswers)

        levelManager.checkAndSaveLevelUnlocks()

        setupLottieAnimation(safeScore, wrongAnswers)

        val soundResId = when {
            wrongAnswers >= maxWrongAnswers && safeScore <= 10 -> R.raw.game_over_sound1
            wrongAnswers >= maxWrongAnswers && safeScore > 10  -> R.raw.game_over_sound2
            else -> R.raw.try_again
        }
        playFeedbackSound(soundResId)

        gameOverBinding.retryButton.setOnClickListener { navigateAfterResult() }
        gameOverBinding.exitButton.setOnClickListener { navigateAfterResult() }
    }

    private fun getFeedbackMessage(score: Int, currentWrong: Int, maxWrong: Int): String {
        return if (currentWrong >= maxWrong) {
            getString(R.string.game_over_limit_reached, maxWrong, score)
        } else {
            getString(R.string.game_over_score_only, score)
        }
    }

    private fun setupLottieAnimation(score: Int, currentWrong: Int) {
        val animView = gameOverBinding.lottieAnimationView
        val animRes = when {
            currentWrong >= maxWrongAnswers && score <= 10 -> R.raw.ic_animationcerebro
            currentWrong >= maxWrongAnswers && score > 10  -> R.raw.ic_datafound
            else -> R.raw.gangster_hey_pluto
        }

        animView.apply {
            cancelAnimation()
            progress = 0f
            setAnimation(animRes)
            repeatCount = 0
            playAnimation()
        }
    }

    // ========================= PERF / FEEDBACK =========================

    private fun calculatePerformance(): Double {
        val correct = (totalQuestions - wrongAnswers).coerceAtLeast(0)
        return if (totalQuestions <= 0) 0.0 else (correct.toDouble() / totalQuestions * 100.0)
    }

    private fun getFeedbackResources(performancePercent: Double): Triple<String, Int, Int?> {
        return when {
            performancePercent >= 100.0 -> Triple(
                getString(R.string.incredible_all_correct),
                R.raw.perfect_score,
                R.drawable.gold_medal
            )
            performancePercent >= 75.0 -> Triple(
                getString(R.string.great_performance),
                R.raw.great_performance,
                R.drawable.ic_trophy
            )
            performancePercent >= 50.0 -> Triple(
                getString(R.string.good_performance),
                R.raw.try_again,
                R.drawable.silver_medal
            )
            else -> Triple(
                getString(R.string.try_again),
                R.raw.try_again,
                null
            )
        }
    }

    // ========================= AUDIO =========================

    private fun playFeedbackSound(soundResId: Int) {
        releaseMediaPlayer()
        try {
            mediaPlayer = MediaPlayer.create(this, soundResId).apply {
                setOnCompletionListener { releaseMediaPlayer() }
                start()
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.release()
            } catch (_: Exception) {
            }
        }
        mediaPlayer = null
    }

    // ========================= NAV =========================

    private fun navigateAfterResult() {
        if (returnToActiveGame) {
            val levelToReturnTo = GameDataManager.getUltimoNivelNormal(this)

            if (levelToReturnTo != null) {
                startActivity(Intent(this, TestActivity::class.java).apply {
                    putExtra(EXTRA_LEVEL, levelToReturnTo)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                })
            } else {
                Log.w("ResultActivity", "⚠️ Retorno ativo, mas nenhum nível normal salvo. Indo para o menu.")
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
        } else {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
        finish()
    }

    override fun onPause() {
        super.onPause()
        releaseMediaPlayer()
    }


    override fun onDestroy() {
        VictoryFx.cancel(binding.konfettiView)
        VictoryFx.release()
        releaseMediaPlayer()
        super.onDestroy()
    }

}
