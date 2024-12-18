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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityTestBinding
import com.desafiolgico.model.Question
import com.desafiolgico.model.QuestionManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
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
    private var currentPhase = 1
    private var isTimerPaused = false
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var gameElements: View
    private val streakBonusMultiplier = 2 // Exemplo de multiplicador para streak
    private val penaltyPoints = 20         // Pontos a serem deduzidos para resposta errada
    val newPointsValue = 10 // Defina o valor dos novos pontos aqui


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)


        lottieAnimationView = findViewById(R.id.lottieAnimationView)
        gameElements = findViewById(R.id.gameElements)

        // Configurar a animação Lottie
        lottieAnimationView.setAnimation(R.raw.ic_animationcerebro)
        lottieAnimationView.repeatCount = 1
        lottieAnimationView.playAnimation()

        // Listener para quando a animação terminar
        lottieAnimationView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                lottieAnimationView.visibility = View.GONE
                gameElements.visibility = View.VISIBLE
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        val savedLevel = intent.getStringExtra("level")
        val savedPoints = intent.getIntExtra("points", 0)

        updatePlayerProgress(savedPoints, savedLevel.toString())


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
        questions = questionManager.getQuestions(level)

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


    private fun calculatePoints(isCorrect: Boolean, streak: Int): Int {
        val basePoints = when (intent.getStringExtra("level")) {
            "Intermediário" -> 60
            "Avançado" -> 90
            else -> 30
        }
        val streakBonus = streak * streakBonusMultiplier
        return if (isCorrect) basePoints + streakBonus else -penaltyPoints
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

    private fun handleAnswerTimeout() {
        totalTime += timeLeftInMillis
        questionsAnswered++
        currentQuestionIndex++
        if (currentQuestionIndex >= questions.size) {
            navigateToResultActivity("RESULT")
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
        val buttons = listOf(
            binding.option1Button,
            binding.option2Button,
            binding.option3Button,
            binding.option4Button
        )

        val isCorrect = selectedOptionIndex == question.correctAnswerIndex

        // Calcular pontos com base na resposta correta e na streak
        val pointsEarned = calculatePoints(isCorrect, correctAnswersCount)


        // Atualizar pontos do usuário


        if (isCorrect) {
            buttons[selectedOptionIndex].setBackgroundColor(
                ContextCompat.getColor(this, R.color.correctAnswerColor)
            )
            correctSound.start()
            score += pointsEarned // Adiciona os pontos calculados
            binding.scoreTextView.text = buildString {
                append("Pontuação: ")
                append(score)
            }
            correctAnswersCount++
            showMotivationalMessage(correctAnswersCount)

            // Verifica se é necessário mudar de fase
            if (correctAnswersCount % 10 == 0) {
                pauseTimer()
                showNextPhaseScreen()
                return
            }
        } else {
            buttons[selectedOptionIndex].setBackgroundColor(
                ContextCompat.getColor(this, R.color.wrongAnswerColor)
            )
            wrongSound.start()
            score += pointsEarned // Deduz os pontos de penalidade
            binding.scoreTextView.text = buildString {
                append("Pontuação: ")
                append(score)
            }
            wrongAnswersCount++
            vibrateOnWrongAnswer()

            if (wrongAnswersCount >= maxWrongAnswers) {
                showEndOfGameDialog()
                return
            }
        }
// Desativa os botoes e avança para a proxima pergunta apos um atraso
        buttons.forEach { it.isEnabled = false }

        GlobalScope.launch(Dispatchers.Main) {
            delay(1000)
            totalTime += timeLeftInMillis - remainingTimeInMillis
            questionsAnswered++
            currentQuestionIndex++
            updateQuestionsRemaining() // Atualizar o campo de perguntas restantes
            if (currentQuestionIndex >= questions.size) {
                navigateToResultActivity("RESULT")
            } else {
                displayQuestion()
                resumeTimer()
            }
        }
    }


    private fun showNextPhaseScreen() {
        val intent = Intent(this, NextPhaseActivity::class.java)
        val nextPhaseDialog = AlertDialog.Builder(this)
            .setTitle("Parabéns!")
            .setMessage("Você avançou para a próxima fase.")
            .setPositiveButton("Continuar") { _, _ ->
                intent.putExtra("PHASE", currentPhase)
                intent.putExtra("level", intent.getStringExtra("level"))
                startActivity(intent)
                currentPhase++
                resumeTimer()
            }
            .setCancelable(false)
            .create()
        nextPhaseDialog.show()
    }

    private fun updatePlayerProgress(points: Int, level: String) {
        Log.d("UpdatePlayerProgress", "Pontos: $points, Nível: $level")

        // Atualiza a UI
        try {
            lifecycleScope.launch(Dispatchers.Main) {
                val pointsTextView = findViewById<TextView>(R.id.pointsTextView)
                val levelTextView = findViewById<TextView>(R.id.levelText)

                pointsTextView?.text = getString(R.string.pontos_format, points)
                levelTextView?.text = getString(R.string.n_vel_format, level)
            }
        } catch (e: Exception) {
            Log.e("UpdatePlayerProgress", "Erro ao atualizar a UI", e)
        }

        // SharedPreferences
        val sharedPreferences = getSharedPreferences("game_prefs", MODE_PRIVATE)
        val totalPoints = sharedPreferences.getInt("totalPoints", 0)
        val newTotalPoints = totalPoints + points

        sharedPreferences.edit().apply {
            putInt("playerPoints", points)
            putInt("totalPoints", newTotalPoints)
            putString("unlockedLevel", level)
            apply()
        }
    }


    @Deprecated("Use newFunction() instead", ReplaceWith("newFunction()"))
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == NEXT_PHASE_REQUEST_CODE && resultCode == RESULT_OK) {
            resumeTimer() // Retomar o temporizador quando o jogador continuar o jogo

        }
    }

    private fun showMotivationalMessage(correctAnswers: Int) {
        val messages = listOf(
            "Ótimo trabalho! Continue assim!",
            "Você está arrasando!",
            "Mais um pouco e você será um mestre!"
        )
        if (correctAnswers > 0 && correctAnswers % 5 == 0) {
            Toast.makeText(this, messages.random(), Toast.LENGTH_SHORT).show()
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
        finish()
        startActivity(intent)
    }

    private fun navigateToResultActivity(screenType: String) {
        val intent = Intent(this, ResultOrGameOverActivity::class.java)
        intent.putExtra("SCREEN_TYPE", screenType)
        intent.putExtra("SCORE", score)
        intent.putExtra("AVERAGE_TIME", calculateAverageTime())
        intent.putExtra("WRONG_ANSWERS", wrongAnswersCount)
        intent.putExtra("MAX_WRONG_ANSWERS", maxWrongAnswers)
        intent.putExtra("TOTAL_QUESTIONS", totalQuestions)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        correctSound.release()
        wrongSound.release()
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
    }
}
