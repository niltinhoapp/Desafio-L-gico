package com.desafiolgico.main

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityTestBinding
import com.desafiolgico.model.Question
import com.desafiolgico.model.QuestionManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class TestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTestBinding
    private lateinit var questions: List<Question>
    private var currentQuestionIndex = 0
    private var score = 0
    private var coins: Int = 0
    private var wrongAnswersCount = 0
    private lateinit var countDownTimer: CountDownTimer
    private var timeLeftInMillis: Long = 0
    private lateinit var correctSound: MediaPlayer
    private lateinit var wrongSound: MediaPlayer
    private var maxWrongAnswers = 5
    private var totalTime: Long = 0 // Armazena o tempo total gasto
    private var questionsAnswered: Int = 0 // Conta as questões respondidas
    private var remainingTimeInMillis: Long = 0 // Armazena o tempo médio gasto
    private var correctAnswersCount: Int = 0
    private val totalQuestions = 48
    private var isTimerPaused = false
    private lateinit var scoreManager: ScoreManager
    private val newPointsValue = 10 // Defina o valor dos novos pontos aqui
    private lateinit var levelUnlockManager: LevelUnlockManager
    private var animationSound: MediaPlayer? = null
    private lateinit var questionManager: QuestionManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)




        questionManager = QuestionManager() // Inicialize aqui
        val selectedLevel = intent.getStringExtra("level") ?: "Iniciante"
        Log.d("TestActivity", "Nível selecionado: $selectedLevel")

        questions = questionManager.getQuestionsByLevel(selectedLevel)



        mostrarAnimacaoDeIntroducao()
        configurarUsuario()
        configurarPontuacao()
        configurarAudio()
        configurarPerguntas()
        configurarAnuncio()
        configurarNivel()


        // Inicializa o gerenciador de desbloqueio ANTES de usá-lo
        levelUnlockManager = LevelUnlockManager(this)

// Recupera dados da Intent
        val savedPoints = intent.getIntExtra("points", 0)

// Atualiza o progresso com os pontos recebidos
        updatePlayerProgress(savedPoints)

// Exemplo: adicionar pontos extras depois de uma resposta certa (se quiser)
        val pontosGanhos = 100
        levelUnlockManager.addPoints(pontosGanhos)


        // Exemplo: depois de terminar o teste, checar se desbloqueou novos níveis
        verificarDesbloqueio()


// Inicializar ScoreManager
        scoreManager = ScoreManager(this)
        observeScoreManager()

// Observando alterações na pontuação
        scoreManager.currentPoints.observe(this) { points ->
            binding.scoreTextView.text = getString(R.string.pontuacao_format, points)
            binding.scoreTextView.text = "Pontuação: $points"
        }

// Observando alterações no streak
        scoreManager.currentStreakLive.observe(this) { streak ->
            binding.streakTextView.text = "Streak: $streak"
            Log.d("TestActivity", "Sequência atual: $streak")
        }
        // Outros códigos...


        // Inicializar Views
        val avatarImageView = findViewById<ImageView>(R.id.logoImageView) // ID do ImageView
        val usernameTextView = findViewById<TextView>(R.id.welcomeUsername) // ID do TextView

        // Receber dados do Intent
        val username = intent.getStringExtra("username") ?: "Jogador"
        val avatarId = intent.getIntExtra("avatar", R.drawable.ic_email_foreground)

        // Atualizar interface com os dados recebidos
        avatarImageView.setImageResource(avatarId) // Exibir avatar ou foto
        usernameTextView.text = username // Exibir nome do usuário

        // Log para depuração
        Log.d("TestActivity", "Nome recebido: $username")
        Log.d("TestActivity", "Avatar recebido: $avatarId")


        binding.coinsTextView.text = buildString {
            append("Moedas: ")
            append(coins)
        }

        // Inicializar o campo de perguntas restantes
        updateQuestionsRemaining()
        loadCoinsFromPreferences()

        MobileAds.initialize(this) {
            val adRequest = AdRequest.Builder().build()
            binding.adView.loadAd(adRequest)
        }

        val level = intent.getStringExtra("level") ?: "Iniciante"
        maxWrongAnswers = when (level) {
            "Intermediário" -> 3
            "Avançado" -> 1
            else -> 5
        }

        binding.levelTextView.text = getString(R.string.n_vel, level)

        correctSound = MediaPlayer.create(this, R.raw.correct_sound)
        wrongSound = MediaPlayer.create(this, R.raw.wrong_sound)

        val questionManager = QuestionManager()
        questions = questionManager.getQuestionsByLevel(level)

        if (questions.isEmpty()) {
            binding.questionTextView.text =
                getString(R.string.nenhuma_pergunta_dispon_vel_para_este_n_vel)
            disableAnswerButtons()
            return
        }

        setupAnswerButtons()
        displayQuestion()

        val sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("points", newPointsValue)
        editor.apply()

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentQuestionIndex", currentQuestionIndex)
        outState.putInt("score", score)
        outState.putInt("wrongAnswersCount", wrongAnswersCount)
        outState.putLong("timeLeftInMillis", timeLeftInMillis)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentQuestionIndex = savedInstanceState.getInt("currentQuestionIndex", 0)
        score = savedInstanceState.getInt("score", 0)
        wrongAnswersCount = savedInstanceState.getInt("wrongAnswersCount", 0)
        timeLeftInMillis = savedInstanceState.getLong("timeLeftInMillis", 30000L)
        displayQuestion()
    }

    private fun mostrarAnimacaoDeIntroducao() {
        binding.gameElements.visibility = View.GONE

        // Libera qualquer som anterior
        animationSound?.release()
        animationSound = MediaPlayer.create(this, R.raw.background_music)

        binding.lottieAnimationView.apply {
            setAnimation(R.raw.airplane_explosion1)
            repeatCount = 4
            visibility = View.VISIBLE
            playAnimation()

            // Inicia o som junto com a animação
            animationSound?.start()

            addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    // Nada extra aqui
                }

                override fun onAnimationEnd(animation: Animator) {
                    animationSound?.stop()
                    animationSound?.release()
                    animationSound = null

                    visibility = View.GONE
                    binding.gameElements.visibility = View.VISIBLE
                }

                override fun onAnimationCancel(animation: Animator) {
                    animationSound?.stop()
                    animationSound?.release()
                    animationSound = null
                }

                override fun onAnimationRepeat(animation: Animator) {

                }
            })
        }
    }


    private fun configurarUsuario() {
        val username = intent.getStringExtra("username") ?: "Jogador"
        val avatarId = intent.getIntExtra("avatar", R.drawable.ic_email_foreground)

        binding.logoImageView.setImageResource(avatarId)
        binding.welcomeUsername.text = username
        Log.d("TestActivity", "Nome recebido: $username")
        Log.d("TestActivity", "Avatar recebido: $avatarId")
    }

    private fun configurarPontuacao() {
        scoreManager = ScoreManager(this)
        observeScoreManager()

        val pontosGanhos = 100
        levelUnlockManager = LevelUnlockManager(this)
        levelUnlockManager.addPoints(pontosGanhos)
        verificarDesbloqueio()

        binding.coinsTextView.text = "Moedas: $coins"
        updateQuestionsRemaining()
        loadCoinsFromPreferences()
    }

    private fun configurarAudio() {
        correctSound = MediaPlayer.create(this, R.raw.correct_sound)
        wrongSound = MediaPlayer.create(this, R.raw.wrong_sound)

    }

    private fun configurarPerguntas() {
        val level = intent.getStringExtra("level") ?: "Iniciante"
        binding.levelTextView.text = getString(R.string.n_vel, level)

        val questionManager = QuestionManager()
        questions = questionManager.getQuestionsByLevel(level)

        if (questions.isEmpty()) {
            binding.questionTextView.text =
                getString(R.string.nenhuma_pergunta_dispon_vel_para_este_n_vel)
            disableAnswerButtons()
        } else {
            setupAnswerButtons()
            displayQuestion()
        }
    }

    private fun configurarAnuncio() {
        MobileAds.initialize(this) {
            val adRequest = AdRequest.Builder().build()
            binding.adView.loadAd(adRequest)
        }
    }

    private fun configurarNivel() {
        val level = intent.getStringExtra("level") ?: "Iniciante"
        maxWrongAnswers = when (level) {
            "Intermediário" -> 3
            "Avançado" -> 1
            else -> 5
        }
    }


    private fun setupAnswerButtons() {
        vibrate()
        val buttons = listOf(
            binding.option1Button,
            binding.option2Button,
            binding.option3Button,
            binding.option4Button
        )
        buttons.forEachIndexed { index, button ->
            button.setOnClickListener { checkAnswer(index) }
        }
    }

    private fun displayQuestion() {
        if (currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            binding.questionTextView.text = question.questionText

            val buttons = listOf(
                binding.option1Button,
                binding.option2Button,
                binding.option3Button,
                binding.option4Button
            )
            question.options.forEachIndexed { index, option ->
                buttons[index].text = option
            }

            resetButtonStyles()
            startTimer()

        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(100)
        }
    }


    private fun resetButtonStyles() {
        val buttons = listOf(
            binding.option1Button,
            binding.option2Button,
            binding.option3Button,
            binding.option4Button
        )
        buttons.forEach { button ->
            button.setBackgroundColor(
                ContextCompat.getColor(this, R.color.defaultButtonColor)
            )
            button.isEnabled = true
        }
    }


    private fun startTimer() {
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }

        timeLeftInMillis = when (intent.getStringExtra("level")) {
            "Intermediário" -> 30000L
            "Avançado" -> 20000L
            else -> 30000L
        }

        binding.timerProgressBar.max = 100
        binding.timerProgressBar.progress = 100

        countDownTimer = object : CountDownTimer(timeLeftInMillis, 100) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeInMillis = millisUntilFinished
                val progressPercent = (millisUntilFinished * 100 / timeLeftInMillis).toInt()

                val drawableRes = when (progressPercent) {
                    in 51..100 -> R.drawable.progress_green
                    in 21..50 -> R.drawable.progress_yellow
                    else -> R.drawable.progress_red
                }
                binding.timerProgressBar.progressDrawable =
                    ContextCompat.getDrawable(this@TestActivity, drawableRes)
            }

            override fun onFinish() {

                binding.timerProgressBar.progress = 0
                totalTime += timeLeftInMillis - remainingTimeInMillis
                questionsAnswered++
                currentQuestionIndex++
                displayQuestion()
                showEndOfGameDialog()
                handleAnswerTimeout()
            }

        }
        countDownTimer.start()
    }

    /**
     * Tratamento quando o tempo da pergunta acaba sem resposta.
     */
    private fun handleAnswerTimeout() {
        vibrateOnWrongAnswer()
        wrongAnswersCount++
        totalTime += timeLeftInMillis
        questionsAnswered++
        currentQuestionIndex++

        if (currentQuestionIndex >= questions.size || wrongAnswersCount >= maxWrongAnswers) {
            navigateToResultActivity("TEMPO ESGOTADO")
        } else {
            displayQuestion()
        }
    }


    private fun pauseTimer() {
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
            isTimerPaused = true
        }
    }

    private fun resumeTimer() {
        if (isTimerPaused) {
            startTimer()
            isTimerPaused = false
        }
    }

    private fun updateQuestionsRemaining() {
        val questionsRemaining = totalQuestions - currentQuestionIndex
        binding.questionsRemainingTextView.text = buildString {
            append("Perguntas restantes: ")
            append(questionsRemaining)
        }
    }


    @SuppressLint("ObsoleteSdkInt")
    private fun vibrateOnWrongAnswer() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(500)
        }
    }


    @OptIn(DelicateCoroutinesApi::class)
    private fun checkAnswer(selectedOptionIndex: Int) {
        val question = questions[currentQuestionIndex]
        val isCorrect = selectedOptionIndex == question.correctAnswerIndex
        updateScoreAndUI(isCorrect)

        updateButtonStyles(selectedOptionIndex, isCorrect)

        // Atualiza a pontuação e a UI usando ScoreManager
        if (isCorrect) {
            scoreManager.onCorrectAnswer()
            handleCorrectAnswer()
        } else {
            scoreManager.onWrongAnswer()
            handleWrongAnswer()
        }

        disableAnswerButtons() // Desativa os botões para evitar cliques adicionais

        // Avança para a próxima pergunta após um pequeno atraso
        advanceToNextQuestionWithDelay()
    }

    /**
     * Avalia se a resposta selecionada está correta.
     */
    private fun evaluateAnswer(selectedOptionIndex: Int, correctAnswerIndex: Int): Boolean {
        return selectedOptionIndex == correctAnswerIndex
    }

    private fun observeScoreManager() {
        scoreManager.currentPoints.observe(this) { points ->
            binding.scoreTextView.text = getString(R.string.pontuacao_format, points)
        }

        scoreManager.currentStreakLive.observe(this) { streak ->
            binding.streakTextView.text = "Streak: $streak"
        }
    }

    /**
     * Atualiza o estilo dos botões baseado na resposta.
     */
    private fun updateButtonStyles(selectedOptionIndex: Int, isCorrect: Boolean) {
        val buttons = listOf(
            binding.option1Button,
            binding.option2Button,
            binding.option3Button,
            binding.option4Button
        )

        val colorResId = if (isCorrect) R.color.correctAnswerColor else R.color.wrongAnswerColor
        buttons[selectedOptionIndex].setBackgroundColor(ContextCompat.getColor(this, colorResId))
    }

    /**
     * Atualiza pontuação e interface do usuário.
     */
    private fun updateScoreAndUI(isCorrect: Boolean) {
        val streak = scoreManager.currentStreakLive.value ?: 0 // Obtendo o streak atual
        val level = intent.getStringExtra("level") ?: "Iniciante" // Obtendo o nível

        // Passando os parâmetros corretos para calculatePoints
        val pointsEarned = scoreManager.calculatePoints(isCorrect, streak, level)
        score += pointsEarned

        binding.scoreTextView.text = getString(R.string.pontuacao_format, score)

        if (isCorrect) correctAnswersCount++ else wrongAnswersCount++
    }

    /**
     * Lida com uma resposta correta.
     */
    private fun handleCorrectAnswer() {
        correctSound.start()
        showMotivationalMessage(correctAnswersCount)

        // Avança para a próxima fase a cada 10 acertos
        if (correctAnswersCount % 10 == 0) {
            pauseTimer()

        }
    }

    /**
     * Lida com uma resposta incorreta.
     */
    private fun handleWrongAnswer() {
        wrongSound.start()
        vibrateOnWrongAnswer()

        if (wrongAnswersCount >= maxWrongAnswers) {
            showEndOfGameDialog()
        }
    }

    /**
     * Desativa os botões de resposta.
     */


    /**
     * Avança para a próxima pergunta após um atraso.
     */
    private fun advanceToNextQuestionWithDelay() {
        lifecycleScope.launch { // Usa lifecycleScope para evitar vazamentos
            delay(1000) // Aguarda 1 segundo antes de avançar
            totalTime += timeLeftInMillis - remainingTimeInMillis
            questionsAnswered++
            currentQuestionIndex++
            updateQuestionsRemaining() // Atualiza perguntas restantes

            if (currentQuestionIndex >= questions.size) {
                navigateToResultActivity("RESULT") // Navega para os resultados se acabou
            } else {
                displayQuestion() // Exibe a próxima pergunta
                resumeTimer() // Retoma o temporizador
            }
        }
    }

    private fun verificarDesbloqueio() {
        val niveisDesbloqueados = levelUnlockManager.getUnlockedLevelsList()
        for (nivel in niveisDesbloqueados) {
            Log.d("GameActivity", "Nível desbloqueado: $nivel")
        }

        // Se quiser também salvar o último nível desbloqueado:
        val ultimoNivel = niveisDesbloqueados.lastOrNull() ?: "Iniciante"
        levelUnlockManager.unlockLevel(ultimoNivel)


        println("Último nível desbloqueado salvo: ${levelUnlockManager.getUnlockedLevel()}")
    }


    private fun updatePlayerProgress(points: Int) {
        Log.d("UpdatePlayerProgress", "Adicionando $points pontos")

        val nivelAntes = levelUnlockManager.getUnlockedLevel()

        levelUnlockManager.addPoints(points) // já atualiza totalPoints e unlockedLevel se necessário

        val nivelDepois = levelUnlockManager.getUnlockedLevel()

        if (nivelDepois != nivelAntes) {
            Toast.makeText(this, "Você desbloqueou o nível: $nivelDepois", Toast.LENGTH_SHORT)
                .show()
        }

        try {
            lifecycleScope.launch(Dispatchers.Main) {
                val totalPoints = levelUnlockManager.getTotalPoints()
                binding.scoreTextView.text = getString(R.string.pontos_format, totalPoints)
            }
        } catch (e: Exception) {
            Log.e("UpdatePlayerProgress", "Erro ao atualizar a UI", e)
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == NEXT_PHASE_REQUEST_CODE && resultCode == RESULT_OK) {
            resumeTimer() // Retomar o temporizador quando o jogador continuar o jogo

        }
    }

    private fun showMotivationalMessage(correctAnswers: Int) {
        val message = when (correctAnswers) {
            5 -> "Você desbloqueou uma nova habilidade!"
            10 -> "Incrível!"
            15 -> "Você é imparável!"
            else -> null
        }
        message?.let {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
    }


    private fun disableAnswerButtons() {
        val buttons = listOf(
            binding.option1Button,
            binding.option2Button,
            binding.option3Button,
            binding.option4Button
        )
        buttons.forEach { it.isEnabled = false }
    }

    private fun calculateAverageTime(): Double {
        return if (questionsAnswered > 0) totalTime.toDouble() / questionsAnswered else 0.0
    }


    private fun showEndOfGameDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Game Over")
            .setMessage("Você atingiu o número máximo de respostas erradas. O que deseja fazer?")
            .setPositiveButton("Reiniciar Jogo") { _, _ ->
                restartGame()
            }
            .setNegativeButton("Ver Resultados") { _, _ ->
                navigateToResultActivity("GAME_OVER")
            }
            .create()

        dialog.show()

        // Iniciar um temporizador de 10 segundos
        val timer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Atualizar a mensagem do diálogo com o tempo restante
                dialog.setMessage(
                    "Você atingiu o número máximo de respostas erradas. O que deseja fazer?\nReiniciando em ${millisUntilFinished / 1000} segundos..."
                )
            }

            override fun onFinish() {
                // Reiniciar o jogo automaticamente se o jogador não fizer uma escolha
                if (dialog.isShowing) {
                    dialog.dismiss()
                    restartGame()
                }
            }
        }
        timer.start()
    }


    companion object {
        private const val NEXT_PHASE_REQUEST_CODE = 1
    }


    private fun loadCoinsFromPreferences() {
        val sharedPreferences = getSharedPreferences("game_prefs", MODE_PRIVATE)
        coins = sharedPreferences.getInt("coins", 0)
        binding.coinsTextView.text = buildString {
            append("Moedas: ")
            append(coins)
        }
    }


    private fun restartGame() {
        // Resetar estados do ScoreManager e variáveis relacionadas ao progresso

        currentQuestionIndex = 0
        wrongAnswersCount = 0
        questionsAnswered = 0
        score = 0

        // Reiniciar o jogo com as configurações iniciais


        // Finalizar a atividade atual e reiniciá-la
        val intent = intent // Salva a intenção atual
        finish() // Finaliza a atividade
        startActivity(intent) // Reinicia a atividade com a intenção salva
    }

    private fun navigateToResultActivity(screenType: String) {
        // Limpa o estado do ScoreManager

        val intent = Intent(this, ResultOrGameOverActivity::class.java).apply {
            putExtra("SCREEN_TYPE", screenType)
            putExtra("SCORE", score)
            putExtra("AVERAGE_TIME", calculateAverageTime())
            putExtra("WRONG_ANSWERS", wrongAnswersCount)
            putExtra("MAX_WRONG_ANSWERS", maxWrongAnswers)
            putExtra("TOTAL_QUESTIONS", totalQuestions)
        }

        startActivity(intent)
        finish()
    }

    override fun onPause() {
        super.onPause()
        // Pausar o temporizador se a atividade for pausada
        animationSound?.stop()
        animationSound?.release()
        animationSound = null
    }

    override fun onDestroy() {
        super.onDestroy() // Executa lógica padrão de onDestroy

        // Resetar estados do ScoreManager para evitar problemas de lógica
        scoreManager.reset()

        // Liberar recursos de áudio para prevenir vazamentos
        correctSound.release()
        wrongSound.release()

        // Cancelar o temporizador se estiver inicializado
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
        // Liberar recursos de animação
        animationSound?.stop()
        animationSound?.release()
        animationSound = null

    }
}
