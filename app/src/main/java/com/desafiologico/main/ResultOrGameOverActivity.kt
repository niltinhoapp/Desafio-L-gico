package com.desafiologico.main

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
import com.bumptech.glide.Glide
import com.desafiologico.R
import com.desafiologico.databinding.ActivityResultOrGameOverBinding
import com.desafiologico.databinding.ContainerGameOverBinding
import com.desafiologico.databinding.ContainerResultBinding
import com.desafiologico.utils.GameDataManager
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size

class ResultOrGameOverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultOrGameOverBinding
    private lateinit var resultBinding: ContainerResultBinding
    private lateinit var gameOverBinding: ContainerGameOverBinding
    private var mediaPlayer: MediaPlayer? = null
    private val wrongAnswers by lazy { intent.getIntExtra("WRONG_ANSWERS", 0) }
    private val maxWrongAnswers by lazy { intent.getIntExtra("MAX_WRONG_ANSWERS", 3) } // Usado em getFeedbackMessage
    private val totalQuestions by lazy { intent.getIntExtra("TOTAL_QUESTIONS", 48) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ResultOrGameOverActivity", "Início do onCreate")

        try {
            binding = ActivityResultOrGameOverBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("ResultOrGameOverActivity", "Layout inflado com sucesso")
        } catch (e: Exception) {
            Log.e("ResultOrGameOverActivity", "Erro ao inflar o layout", e)
            // Considerar finalizar a activity ou mostrar uma UI de erro genérica se o layout principal falhar
            return
        }

        try {
            // Inflate layouts incluídos
            resultBinding =
                ContainerResultBinding.bind(binding.root.findViewById(R.id.resultContainer))
            gameOverBinding =
                ContainerGameOverBinding.bind(binding.root.findViewById(R.id.gameOverContainer))
            Log.d("ResultOrGameOverActivity", "Layouts incluídos inflados com sucesso")
        } catch (e: Exception) {
            Log.e("ResultOrGameOverActivity", "Erro ao inflar layouts incluídos", e)
            // Pode ser crítico se os bindings forem usados antes de uma verificação
        }

        setupScreen()
        Log.d("ResultOrGameOverActivity", "Finalização do onCreate")
    }


    private fun setupScreen() {
        val screenType = intent.getStringExtra("SCREEN_TYPE") ?: "RESULT"
        val score = intent.getIntExtra("SCORE", 0) // Alterado default para 0 para consistência
        val averageTime = intent.getDoubleExtra("AVERAGE_TIME", 0.0) // Alterado default para 0.0
        // wrongAnswers, maxWrongAnswers e totalQuestions são lazy properties

        when (screenType) {
            "RESULT" -> showResultScreen(score, averageTime)
            "GAME_OVER" -> showGameOverScreen(score) // Removido wrongAnswers, maxWrongAnswers, pois são properties da classe
            else -> {
                Log.e("ResultOrGameOverActivity", "Tipo de tela desconhecido: $screenType. Exibindo tela de resultado padrão.")
                showResultScreen(score, averageTime) // Fallback para tela de resultado
            }
        }
    }


    private fun calculatePerformance(score: Int): Double {
        if (totalQuestions <= 0) {
            Log.e("ResultOrGameOverActivity", "TOTAL_QUESTIONS deve ser maior que 0 para calcular performance.")
            return 0.0
        }
        return (score.toDouble() / totalQuestions * 100).also {
            Log.d("ResultOrGameOverActivity", "Desempenho calculado: $it%")
        }
    }


    @SuppressLint("SetTextI18n")
    private fun showResultScreen(score: Int, averageTime: Double) {
        ObjectAnimator.ofFloat(resultBinding.resultContainer, "alpha", 0f, 1f).apply {
            duration = 600
            start()
        }
        gameOverBinding.root.visibility = View.GONE // Usando binding direto
        resultBinding.resultContainer.visibility = View.VISIBLE

        val (feedbackText, soundResId, medalResId) = getFeedbackResources(score)

        resultBinding.scoreTextView.text = "Você conquistou $score pontos de $totalQuestions"
        resultBinding.feedbackTextView.text = feedbackText
        resultBinding.statsTextView.text = """
            🕒 Tempo médio por pergunta: ${"%.2f".format(averageTime)} segundos
            📊 Desempenho: ${"%.2f".format(calculatePerformance(score))}%
        """.trimIndent()

        medalResId?.let {
            Glide.with(this)
                .load(it)
                // .override(100, 100) // Considere se o override é necessário ou se o ImageView tem tamanho definido
                .into(resultBinding.medalImageView)
        }

        applyFadeInEffect(resultBinding.scoreTextView)
        applyFadeInEffect(resultBinding.feedbackTextView)
        applyFadeInEffect(resultBinding.statsTextView)
        animateScore(resultBinding.scoreTextView, score, totalQuestions)

        resultBinding.retryButton.setOnClickListener {
            // Não é necessário enviar score de volta para MainActivity aqui,
            // pois GameDataManager já deve ter sido atualizado pela TestActivity.
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        playFeedbackSound(soundResId)
        launchKonfetti()
    }

    private fun getFeedbackResources(score: Int): Triple<String, Int, Int?> {
        val percentage = if (totalQuestions > 0) score.toDouble() / totalQuestions else 0.0
        return when {
            percentage == 1.0 -> Triple(
                "Incrível! Você acertou tudo!",
                R.raw.perfect_score,
                R.drawable.gold_medal
            )
            percentage > 0.75 -> Triple(
                "Ótimo desempenho! Continue assim!",
                R.raw.great_performance,
                R.drawable.ic_trophy
            )
            percentage > 0.5 -> Triple(
                "Muito bom! Você pode melhorar.",
                R.raw.try_again, // Som pode ser revisto para "Muito Bom"
                R.drawable.silver_medal
            )
            else -> Triple(
                "Não desista! Tente novamente!",
                R.raw.good_performance, // Som pode ser revisto para "Tente Novamente"
                null // Ou uma medalha de bronze/participação
            )
        }
    }

    private fun launchKonfetti() {
        val konfettiView = resultBinding.konfettiView // Usando binding direto
        konfettiView.post {
            try {
                konfettiView.build()
                    .addColors(Color.YELLOW, Color.GREEN, Color.MAGENTA, Color.RED, Color.BLUE, Color.CYAN, Color.WHITE)
                    .setDirection(0.0, 359.0) // Amplo espectro de direções
                    .setSpeed(4f, 12f)
                    .setFadeOutEnabled(true)
                    .setTimeToLive(3000L)
                    .addShapes(Shape.Square, Shape.Circle)
                    .addSizes(Size(8, 2f), Size(12, 3f), Size(16, 4f)) // Ajustado para tamanhos menores
                    .setPosition(konfettiView.width / 2f, konfettiView.width / 2f, -50f, -50f) // Emitir do topo central
                    .streamFor(300, 3000L) // Menos partículas, duração mais longa

                Log.d("ResultOrGameOverActivity", "Konfetti iniciado com sucesso")
            } catch (e: Exception) {
                Log.e("ResultOrGameOverActivity", "Erro ao inicializar Konfetti", e)
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun showGameOverScreen(currentRoundScore: Int) {
        resultBinding.root.visibility = View.GONE // Usando binding direto
        gameOverBinding.root.visibility = View.VISIBLE // Usando binding direto

        // Garante que a pontuação da rodada não seja negativa ao adicionar ao total
        val adjustedRoundScore = currentRoundScore.coerceAtLeast(0)

        // Adiciona a pontuação da rodada atual à pontuação total geral via GameDataManager
        GameDataManager.addScoreToOverallTotal(this, adjustedRoundScore)
        Log.d("ResultOrGameOverActivity", "Adicionado $adjustedRoundScore à pontuação total geral via GameDataManager.")

        // Obtém a nova pontuação total geral do GameDataManager para exibição
        val newOverallTotalScore = GameDataManager.getOverallTotalScore(this)
        Log.d("ResultOrGameOverActivity", "Nova pontuação total geral obtida do GameDataManager: $newOverallTotalScore.")

        // Atualiza o TextView com a pontuação total geral
        gameOverBinding.totalScoreTextView.text = "Sua pontuação total: $newOverallTotalScore"

        // **NOVO**: Define o texto de feedback usando getFeedbackMessage
        // Supondo que você tenha um TextView com id: gameOverFeedbackTextView no layout container_game_over
        if (::gameOverBinding.isInitialized && gameOverBinding.root.findViewById<TextView>(R.id.gameOverFeedbackTextView) != null) {
            gameOverBinding.gameOverFeedbackTextView.text = getFeedbackMessage(currentRoundScore, wrongAnswers, maxWrongAnswers)
        } else {
            Log.w("ResultOrGameOverActivity", "gameOverFeedbackTextView não encontrado no layout gameOverContainer.")
            // Como fallback, pode-se usar o Log ou um Toast, ou mesmo um TextView já existente se fizer sentido
            // Exemplo: gameOverBinding.totalScoreTextView.text = getFeedbackMessage(currentRoundScore, wrongAnswers, maxWrongAnswers)
            // Se não houver um TextView dedicado, você pode concatenar a mensagem no totalScoreTextView ou criar um.
            // Por agora, vou apenas logar, você precisará adicionar o TextView ao seu layout.
            Log.i("ResultOrGameOverActivity", "Mensagem de Game Over (não exibida na UI): ${getFeedbackMessage(currentRoundScore, wrongAnswers, maxWrongAnswers)}")
        }


        setupLottieAnimation(currentRoundScore, wrongAnswers)
        playFeedbackSound(getSoundResource(currentRoundScore, wrongAnswers, maxWrongAnswers))

        gameOverBinding.retryButton.setOnClickListener {
            Log.d("ResultOrGameOverActivity", "Botão de reinício clicado")
            restartGame()
        }

        gameOverBinding.exitButton.setOnClickListener {
            Log.d("ResultOrGameOverActivity", "Botão de saída clicado. Finalizando atividade")
            finish() // Apenas finaliza esta activity, o usuário voltará para a activity anterior no stack (provavelmente MainActivity)
        }
    }

    private fun getFeedbackMessage(score: Int, currentWrongAnswers: Int, maxLevelWrongAnswers: Int): String {
        return if (currentWrongAnswers >= maxLevelWrongAnswers) {
            // A pontuação exibida aqui é a da rodada atual, não a total geral.
            "Game Over: Você atingiu o limite de $maxLevelWrongAnswers erros.\nPontuação nesta rodada: $score"
        } else {
            // Este caso pode não ser "Game Over" se o limite de erros não foi atingido,
            // a menos que haja outra condição de Game Over.
            // Se SCREEN_TYPE é "GAME_OVER" mas limite de erros não foi atingido, a lógica de Game Over deve ser revisada.
            // Assumindo que se chegou aqui, é Game Over por algum motivo.
            "Fim de jogo!\nPontuação nesta rodada: $score"
        }
    }

    private fun setupLottieAnimation(score: Int, currentWrongAnswers: Int) {
        // Acessando LottieAnimationView diretamente pelo binding do container, se o ID for consistente
        // Se 'lottieAnimationView' for um ID dentro de 'gameOverContainer', use gameOverBinding.lottieAnimationView
        // Se for um ID no layout principal 'activity_result_or_game_over', use binding.lottieAnimationView
        // Vou assumir que está no gameOverContainer para este exemplo:
        // (Certifique-se que o ID 'lottieAnimationView' existe em container_game_over.xml)
        val animationView = gameOverBinding.lottieAnimationView // Exemplo, ajuste se o ID for diferente ou em outro binding

        val animationResource = when {
            currentWrongAnswers >= maxWrongAnswers && score <= 10 -> R.raw.ic_animationcerebro
            currentWrongAnswers >= maxWrongAnswers && score > 10 -> R.raw.ic_datafound // Ajustado para > 10
            else -> R.raw.gangster_hey_pluto // Animação padrão se não for game over por erros com score baixo
        }
        animationView.setAnimation(animationResource)
        animationView.playAnimation()
    }

    private fun getSoundResource(score: Int, currentWrongAnswers: Int, maxLevelWrongAnswers: Int): Int {
        return when {
            currentWrongAnswers >= maxLevelWrongAnswers && score <= 10 -> R.raw.game_over_sound1
            currentWrongAnswers >= maxLevelWrongAnswers && score > 10 -> R.raw.game_over_sound2 // Ajustado para > 10
            else -> R.raw.gangster_hey_pluto // Som padrão
        }
    }

    private fun applyFadeInEffect(view: View, duration: Long = 1000) {
        if (view.visibility != View.VISIBLE) {
            Log.w("ResultOrGameOverActivity", "View não visível para animação: ${view.resources.getResourceEntryName(view.id)}")
            return
        }
        view.alpha = 0f
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(AnimationUtils.loadInterpolator(this, android.R.interpolator.accelerate_decelerate))
            .start()
    }

    private fun animateScore(
        scoreTextView: TextView,
        score: Int,
        totalRoundQuestions: Int,
        duration: Long = 1500
    ) {
        val animator = ValueAnimator.ofInt(0, score).apply {
            this.duration = duration
            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Int
                scoreTextView.text = getString(R.string.score_text, animatedValue, totalRoundQuestions)
            }
        }
        val scaleX = ObjectAnimator.ofFloat(scoreTextView, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(scoreTextView, "scaleY", 1f, 1.2f, 1f)

        AnimatorSet().apply {
            playTogether(animator, scaleX, scaleY)
            interpolator = AnimationUtils.loadInterpolator(this@ResultOrGameOverActivity, android.R.interpolator.accelerate_decelerate)
            start()
        }
    }

    private fun playFeedbackSound(soundResId: Int) {
        releaseMediaPlayer()
        try {
            mediaPlayer = MediaPlayer.create(this, soundResId)
            mediaPlayer?.setOnCompletionListener { releaseMediaPlayer() }
            mediaPlayer?.start()
            if (mediaPlayer == null) {
                Log.e("ResultOrGameOverActivity", "MediaPlayer.create retornou null para o recurso: $soundResId")
            }
        } catch (e: Exception) {
            Log.e("ResultOrGameOverActivity", "Erro ao criar ou iniciar MediaPlayer", e)
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                Log.e("ResultOrGameOverActivity", "Erro ao liberar MediaPlayer", e)
            }
            mediaPlayer = null
        }
    }

    private fun restartGame() {
        // Leva para MainActivity para escolher o nível novamente
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    override fun onPause() {
        super.onPause()
        releaseMediaPlayer()
    }

    override fun onStop() {
        super.onStop()
        releaseMediaPlayer() // Garante a liberação se a activity for parada rapidamente
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
    }
}