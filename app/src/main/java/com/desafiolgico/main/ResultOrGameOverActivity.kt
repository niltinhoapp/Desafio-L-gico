package com.desafiolgico.main

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityResultOrGameOverBinding
import com.desafiolgico.databinding.ContainerGameOverBinding
import com.desafiolgico.databinding.ContainerResultBinding
import nl.dionsegijn.konfetti.KonfettiView
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size

class ResultOrGameOverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultOrGameOverBinding
    private lateinit var resultBinding: ContainerResultBinding
    private lateinit var gameOverBinding: ContainerGameOverBinding
    private var mediaPlayer: MediaPlayer? = null
    private val wrongAnswers by lazy { intent.getIntExtra("WRONG_ANSWERS", 0) }
    private val maxWrongAnswers by lazy { intent.getIntExtra("MAX_WRONG_ANSWERS", 3) }
    private val totalQuestions by lazy { intent.getIntExtra("TOTAL_QUESTIONS", 48) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("DEBUG", "Início do onCreate")

        try {
            binding = ActivityResultOrGameOverBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("DEBUG", "Layout inflado com sucesso")
        } catch (e: Exception) {
            Log.e("ERROR", "Erro ao inflar o layout", e)
        }

        try {
            // Inflate layouts incluídos
            resultBinding =
                ContainerResultBinding.bind(binding.root.findViewById(R.id.resultContainer))
            gameOverBinding =
                ContainerGameOverBinding.bind(binding.root.findViewById(R.id.gameOverContainer))
            Log.d("DEBUG", "Layouts incluídos inflados com sucesso")
        } catch (e: Exception) {
            Log.e("ERROR", "Erro ao inflar layouts incluídos", e)
        }

        setupScreen()
        Log.d("DEBUG", "Finalização do onCreate")
    }


    private fun setupScreen() {
        val screenType = intent.getStringExtra("SCREEN_TYPE") ?: "RESULT"
        val score = intent.getIntExtra("SCORE", -1)
        val averageTime = intent.getDoubleExtra("AVERAGE_TIME", -1.0)
        val wrongAnswers = intent.getIntExtra("WRONG_ANSWERS", 0)
        val maxWrongAnswers = intent.getIntExtra("MAX_WRONG_ANSWERS", 3)
        val totalQuestions = intent.getIntExtra("TOTAL_QUESTIONS", 48)

        when (screenType) {
            "RESULT" -> showResultScreen(score, averageTime)
            "GAME_OVER" -> showGameOverScreen(score, wrongAnswers, maxWrongAnswers)
            else -> Log.e("ERROR", "Tipo de tela desconhecido: $screenType")
        }
    }


    private fun calculatePerformance(score: Int): Double {
        // Garantir que o score seja válido e evitar divisões por zero
        if (totalQuestions <= 0) {
            Log.e("DEBUG", "TOTAL_QUESTIONS deve ser maior que 0.")
            return 0.0
        }

        // Calcular o desempenho como uma porcentagem
        return (score.toDouble() / totalQuestions * 100).also {
            Log.d("DEBUG", "Desempenho calculado: $it%")
        }
    }


    @SuppressLint("SetTextI18n")
    private fun showResultScreen(score: Int, averageTime: Double) {
        // Configurar o layout de resultado usando ViewBinding
        binding.root.findViewById<View>(R.id.resultContainer).visibility = View.VISIBLE
        binding.root.findViewById<View>(R.id.gameOverContainer).visibility = View.GONE

        val (feedbackText, soundResId, medalResId) = when {
            score == totalQuestions -> Triple(
                "Incrível! Você acertou tudo!",
                R.raw.perfect_score,
                R.drawable.gold_medal
            )

            score > totalQuestions * 0.75 -> Triple(
                "Ótimo desempenho! Continue assim!",
                R.raw.great_performance,
                R.drawable.ic_trophy
            )

            score > totalQuestions * 0.5 -> Triple(
                "Muito bom! Você pode melhorar.",
                R.raw.try_again,
                R.drawable.silver_medal
            )

            else -> Triple(
                "Não desista! Tente novamente!",
                R.raw.good_performance,
                null
            )
        }

        resultBinding.scoreTextView.text = buildString {
            append("Você conquistou ")
            append(score)
            append(" pontos de ")
            append(totalQuestions)
        }
        resultBinding.feedbackTextView.text = feedbackText
        resultBinding.statsTextView.text = """
        🕒 Tempo médio por pergunta: ${"%.2f".format(averageTime)} segundos
         📊 Desempenho: ${"%.2f".format(calculatePerformance(score))}%
    """.trimIndent()

        medalResId?.let {
            Glide.with(this)
                .load(it)
                .override(100, 100)
                .into(resultBinding.medalImageView)
        }

        applyFadeInEffect(resultBinding.scoreTextView)
        applyFadeInEffect(resultBinding.feedbackTextView)
        applyFadeInEffect(resultBinding.statsTextView)

        animateScore(resultBinding.scoreTextView, score, totalQuestions)
        resultBinding.retryButton.setOnClickListener { restartGame() }
        playFeedbackSound(soundResId)

        val konfettiView = findViewById<KonfettiView>(R.id.konfettiView)
        try {
            konfettiView.build()
                .addColors(
                    Color.YELLOW,
                    Color.GREEN,
                    Color.MAGENTA,
                    Color.RED,
                    Color.BLUE,
                    Color.CYAN,
                    Color.WHITE
                )
                .setDirection(0.0, 359.0)
                .setSpeed(1f, 6f)
                .setFadeOutEnabled(true)
                .setTimeToLive(3000L)
                .addShapes(Shape.Square, Shape.Circle)
                .addSizes(Size(6, 3f), Size(12, 5f), Size(16, 6f), Size(24, 8f), Size(32, 10f))
                .setPosition(-50f, konfettiView.width + 50f, -50f, -50f)
                .streamFor(800, 5000L)
            Log.d("DEBUG", "Konfetti iniciado com sucesso")
        } catch (e: Exception) {
            Log.e("ERROR", "Erro ao inicializar Konfetti", e)
        }

    }


    @SuppressLint("SetTextI18n")
    private fun showGameOverScreen(score: Int, wrongAnswers: Int, maxWrongAnswers: Int) {
        // Exibir o container de Game Over e ocultar o de resultados
        binding.root.findViewById<View>(R.id.resultContainer).visibility = View.GONE
        binding.root.findViewById<View>(R.id.gameOverContainer).visibility = View.VISIBLE

        // Recuperar a pontuação total acumulada das SharedPreferences
        val sharedPreferences = getSharedPreferences("GamePrefs", MODE_PRIVATE)
        val totalScore = sharedPreferences.getInt("totalScore", 0)

        // Atualizar a pontuação total acumulada com a pontuação atual
        val newTotalScore = totalScore + score
        sharedPreferences.edit().putInt("totalScore", newTotalScore).apply()


        // Determinar mensagem personalizada de feedback para Game Over
        val feedbackMessage = if (wrongAnswers >= maxWrongAnswers) {
            "Game Over: Você atingiu o limite de $maxWrongAnswers erros. Sua pontuação total: $score"
        } else {
            "Game Over: \n Continue jogando! Sua pontuação atual: $score"
        }

        // Atualizar mensagem na interface
        gameOverBinding.messageTextView.text = feedbackMessage
        gameOverBinding.totalScoreTextView.text = "Sua pontuação total: ..."

        // Configurar animação Lottie com base na pontuação
        val lottieAnimationView =
            gameOverBinding.root.findViewById<LottieAnimationView>(R.id.lottieAnimationView)
        val animationResource = when {
            wrongAnswers >= maxWrongAnswers && score <= 10 -> R.raw.ic_datafound
            wrongAnswers >= maxWrongAnswers && score in 11..25 -> R.raw.ic_datafound
            else -> R.raw.ic_datafound
        }
        lottieAnimationView.setAnimation(animationResource)
        lottieAnimationView.invalidate()
        lottieAnimationView.requestLayout()
        lottieAnimationView.playAnimation()

        // Exibir tempo médio e percentual de desempenho
        val averageTime = intent.getDoubleExtra("AVERAGE_TIME", 0.0)
        gameOverBinding.averageTimeTextView.text = """
        🕒 Tempo médio por pergunta: ${"%.2f".format(averageTime)} segundos
        📊 Desempenho: ${"%.2f".format(calculatePerformance(score))}%
    """.trimIndent()

        // Aplicar animação de fade-in para elementos textuais
        applyFadeInEffect(gameOverBinding.messageTextView)
        applyFadeInEffect(gameOverBinding.averageTimeTextView)

        // Reproduzir som baseado no desempenho
        val soundResource = when {
            wrongAnswers >= maxWrongAnswers && score <= 10 -> R.raw.game_over_sound1
            wrongAnswers >= maxWrongAnswers && score in 11..25 -> R.raw.game_over_sound2
            else -> R.raw.gangster_hey_pluto
        }
        playFeedbackSound(soundResource)

        // Configurar botões de reinício e saída
        gameOverBinding.retryButton.setOnClickListener {
            Log.d("DEBUG", "Botão de reinício clicado")
            restartGame()
        }
        gameOverBinding.exitButton.setOnClickListener {
            Log.d("DEBUG", "Botão de saída clicado. Finalizando atividade")
            finish()
        }
    }

    private fun applyFadeInEffect(view: View, duration: Long = 1000) {
        if (view.visibility != View.VISIBLE) {
            Log.w("WARNING", "View não visível para animação: ${view.id}")
            return
        }

        view.alpha = 0f
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(
                AnimationUtils.loadInterpolator(
                    this,
                    android.R.interpolator.accelerate_decelerate
                )
            )
            .start()
        Log.d("DEBUG", "Efeito de fade-in aplicado na view: ${view.id}")
    }

    private fun animateScore(
        scoreTextView: TextView,
        score: Int,
        totalQuestions: Int,
        duration: Long = 1500
    ) {
        val animator = ValueAnimator.ofInt(0, score).apply {
            this.duration = duration
            addUpdateListener { animation ->
                val currentScore = animation.animatedValue as Int
                scoreTextView.text = getString(R.string.score_text, currentScore, totalQuestions)
            }
        }

        val scaleX = ObjectAnimator.ofFloat(scoreTextView, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(scoreTextView, "scaleY", 1f, 1.2f, 1f)

        val animatorSet = AnimatorSet().apply {
            playTogether(animator, scaleX, scaleY)
            interpolator = AnimationUtils.loadInterpolator(
                this@ResultOrGameOverActivity,
                android.R.interpolator.accelerate_decelerate
            )
        }
        animatorSet.start()
    }

    // Reproduz um som de feedback e libera o MediaPlayer ao final
    private fun playFeedbackSound(soundResId: Int) {
        releaseMediaPlayer() // Libera qualquer instanciação anterior do MediaPlayer
        try {
            mediaPlayer = MediaPlayer.create(this, soundResId)
            mediaPlayer?.apply {
                setOnPreparedListener { start() }
                setOnCompletionListener { releaseMediaPlayer() } // Libera após a reprodução
            } ?: run {
                Log.e("DEBUG", "MediaPlayer retornou null para o recurso: $soundResId")
            }
        } catch (e: Exception) {
            Log.e("DEBUG", "Erro ao reproduzir som", e)
        }
    }

    // Libera recursos do MediaPlayer
    private fun releaseMediaPlayer() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            mediaPlayer = null
            Log.d("DEBUG", "MediaPlayer liberado com sucesso.")
        }
    }

    // Reinicia o jogo
    private fun restartGame() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    // Libera o MediaPlayer quando a Activity é pausada ou destruída
    override fun onPause() {
        super.onPause()
        releaseMediaPlayer()
    }

    override fun onStop() {
        super.onStop()
        releaseMediaPlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
    }
}