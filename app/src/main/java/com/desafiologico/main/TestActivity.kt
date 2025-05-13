package com.desafiologico.main

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.desafiologico.R
import com.desafiologico.databinding.ActivityTestBinding
import com.desafiologico.model.Question // Supondo que você tenha este modelo
import com.desafiologico.model.QuestionManager // Supondo que você tenha este manager
import com.desafiologico.utils.GameDataManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView // Import AdView
import com.google.android.gms.ads.LoadAdError // Import LoadAdError
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Suppress("DEPRECATION") // Para onActivityResult
class TestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTestBinding
    private lateinit var questions: List<Question>
    private var currentQuestionIndex = 0

    // Variáveis de estado do jogo
    private var score = 0
    private var coins: Int = 0
    private var wrongAnswersCount = 0
    private var maxWrongAnswers = 5
    private var totalTime: Long = 0
    private var questionsAnswered: Int = 0
    private var correctAnswersCount: Int = 0
    private var totalQuestions = 48

    // Configurações do Timer
    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 0
    private var remainingTimeInMillis: Long = 0
    private var isTimerPaused = false
    private val timerIntervalMillis: Long = 100L

    // Managers e UI
    private lateinit var scoreManager: ScoreManager
    private lateinit var levelManager: LevelManager
    private lateinit var questionManager: QuestionManager
    private lateinit var rootLayout: ViewGroup

    // MediaPlayers
    private lateinit var correctSound: MediaPlayer
    private lateinit var wrongSound: MediaPlayer
    private var animationSound: MediaPlayer? = null


    companion object {
        private const val GREEN_THRESHOLD_PERCENT = 50
        private const val YELLOW_THRESHOLD_PERCENT = 20
        private const val NEXT_PHASE_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rootLayout = binding.root
        questionManager = QuestionManager()
        levelManager = LevelManager(this)
        scoreManager = ScoreManager(this)

        val selectedLevel = intent.getStringExtra("level") ?: GameDataManager.Levels.INICIANTE
        Log.d("TestActivity", "Nível selecionado: $selectedLevel")
        questions = questionManager.getQuestionsByLevel(selectedLevel)
        totalQuestions = questions.size

        setupUI()
        setupGameLogic()
        setupAds()
        loadUserData()

        if (questions.isEmpty()) {
            binding.questionTextView.text = getString(R.string.nenhuma_pergunta_dispon_vel_para_este_n_vel)
            disableAnswerButtons()
        } else {
            setupAnswerButtons()
            displayQuestion()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentQuestionIndex", currentQuestionIndex)
        outState.putInt("score", score)
        outState.putInt("wrongAnswersCount", wrongAnswersCount)
        outState.putLong("remainingTimeInMillis", remainingTimeInMillis)
        outState.putBoolean("isTimerPaused", isTimerPaused)
        outState.putInt("correctAnswersCount", correctAnswersCount)
        outState.putInt("questionsAnswered", questionsAnswered)
        outState.putLong("totalTime", totalTime)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentQuestionIndex = savedInstanceState.getInt("currentQuestionIndex", 0)
        score = savedInstanceState.getInt("score", 0)
        wrongAnswersCount = savedInstanceState.getInt("wrongAnswersCount", 0)
        remainingTimeInMillis = savedInstanceState.getLong("remainingTimeInMillis", 0L)
        isTimerPaused = savedInstanceState.getBoolean("isTimerPaused", false)
        correctAnswersCount = savedInstanceState.getInt("correctAnswersCount", 0)
        questionsAnswered = savedInstanceState.getInt("questionsAnswered", 0)
        totalTime = savedInstanceState.getLong("totalTime", 0L)

        configurarPontuacao()
        updateQuestionsRemaining()
        // Se o jogo estava em andamento, onResume cuidará de chamar displayQuestion ou resumeTimer
    }

    private fun setupUI() {
        mostrarAnimacaoDeIntroducao()
        configurarAudio()
        configurarNivel()
    }

    private fun setupGameLogic() {
        updatePlayerProgress(intent.getIntExtra("points", 0))
        verificarDesbloqueio()
        observeScoreManager()
        configurarPontuacao()
        configurarPerguntas()
        updateQuestionsRemaining()
    }

    private fun setupAds() {
        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
        binding.adView.adListener = object : com.google.android.gms.ads.AdListener() {
            override fun onAdLoaded() {
                Log.d("AdMob", "Anúncio carregado com sucesso.")
                binding.adView.visibility = View.VISIBLE
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e("AdMob", "Erro ao carregar o anúncio: ${adError.message}")
                binding.adView.visibility = View.GONE
            }
        }
    }

    private fun loadUserData() {
        val (username, photoUrl, avatarId) = GameDataManager.loadUserData(this)
        binding.welcomeUsername.text = username
        Glide.with(this)
            .load(photoUrl)
            .placeholder(avatarId)
            .error(avatarId)
            .into(binding.logoImageView)
        Log.d("TestActivity", "Nome recebido: $username, Foto: $photoUrl, Avatar ID: $avatarId")

        coins = GameDataManager.loadCoins(this)
        binding.coinsTextView.text = getString(R.string.coins_format, coins)
    }

    private fun mostrarAnimacaoDeIntroducao() {
        binding.gameElements.visibility = View.GONE
        animationSound?.release()
        animationSound = MediaPlayer.create(this, R.raw.background_music)

        binding.lottieAnimationView.apply {
            setAnimation(R.raw.airplane_explosion1)
            repeatCount = 0 // Tocar uma vez

            visibility = View.VISIBLE
            playAnimation()
            animationSound?.start()

            addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) { Log.d("Lottie", "Animação de introdução iniciada.")}
                override fun onAnimationEnd(animation: Animator) {
                    Log.d("Lottie", "Animação de introdução finalizada.")
                    animationSound?.stop()
                    animationSound?.release()
                    animationSound = null
                    visibility = View.GONE
                    binding.gameElements.visibility = View.VISIBLE
                }
                override fun onAnimationCancel(animation: Animator) {
                    Log.d("Lottie", "Animação de introdução cancelada.")
                    animationSound?.stop()
                    animationSound?.release()
                    animationSound = null
                    binding.lottieAnimationView.visibility = View.GONE
                    binding.gameElements.visibility = View.VISIBLE
                }
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }
    }

    private fun configurarPontuacao() {
        binding.scoreTextView.text = getString(R.string.score_format_string, scoreManager.getTotalScore())
        binding.streakTextView.text = getString(R.string.streak_format_string, GameDataManager.currentStreak)
        Log.d("TestActivity", "UI de Pontuação Atualizada: Score=${scoreManager.getTotalScore()}, Streak=${GameDataManager.currentStreak}")
    }

    private fun configurarAudio() {
        correctSound = MediaPlayer.create(this, R.raw.correct_sound)
        wrongSound = MediaPlayer.create(this, R.raw.wrong_sound)
    }

    private fun configurarPerguntas() {
        binding.levelTextView.text = getString(R.string.n_vel, intent.getStringExtra("level") ?: GameDataManager.Levels.INICIANTE)
    }

    private fun configurarNivel() {
        val level = intent.getStringExtra("level") ?: GameDataManager.Levels.INICIANTE
        maxWrongAnswers = when (level) {
            GameDataManager.Levels.INTERMEDIARIO -> 3
            GameDataManager.Levels.AVANCADO -> 1
            else -> 5 // INICIANTE
        }
        Log.d("TestActivity", "Nível configurado: $level, Máximo de erros: $maxWrongAnswers")
    }

    private fun setupAnswerButtons() {
        val buttons = listOf(
            binding.option1Button,
            binding.option2Button,
            binding.option3Button,
            binding.option4Button
        )
        buttons.forEachIndexed { index, button ->
            button.setOnClickListener {
                vibrate()
                checkAnswer(index)
            }
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
                if (index < buttons.size) {
                    buttons[index].text = option
                    buttons[index].visibility = View.VISIBLE
                }
            }
            for (i in question.options.size until buttons.size) {
                buttons[i].visibility = View.GONE
            }

            isTimerPaused = false
            remainingTimeInMillis = 0L

            resetButtonStyles()
            enableAnswerButtons()
            startTimer()
            updateQuestionsRemaining()
        } else {
            Log.d("TestActivity", "Todas as perguntas respondidas.")
            navigateToResultActivity("RESULT")
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    /**
     * Reseta os estilos dos botões de opção para o estado padrão para uma nova pergunta.
     * A cor do texto é definida para contrastar com o fundo do card.
     * O backgroundTintList do botão é resetado para null para que o fundo do CardView seja visível.
     */
    private fun resetButtonStyles() {
        val buttons = listOf(
            binding.option1Button,
            binding.option2Button,
            binding.option3Button,
            binding.option4Button
        )
        buttons.forEach { button ->
            // Define a cor do texto para a cor padrão (que deve contrastar com o fundo do botão)
            button.setTextColor(ContextCompat.getColor(this, R.color.background_color))
            // Define o fundo do BOTÃO para a cor padrão @color/button_default
            button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_default)
        }
        Log.d("TestActivity", "Estilos dos botões resetados para a próxima pergunta.")
    }

    private fun startTimer() {
        countDownTimer?.cancel()

        timeLeftInMillis = when (intent.getStringExtra("level")) {
            GameDataManager.Levels.INTERMEDIARIO -> 20000L
            GameDataManager.Levels.AVANCADO -> 15000L
            else -> 30000L // INICIANTE
        }

        val durationForTimer = if (isTimerPaused && remainingTimeInMillis > 0) {
            Log.d("TestActivity", "Resumindo timer com $remainingTimeInMillis ms restantes.")
            remainingTimeInMillis
        } else {
            Log.d("TestActivity", "Iniciando novo timer com $timeLeftInMillis ms.")
            timeLeftInMillis
        }
        isTimerPaused = false


        binding.timerProgressBar.max = 100
        binding.timerProgressBar.progress = 100
        binding.timerProgressBar.progressDrawable = ContextCompat.getDrawable(this, R.drawable.progress_green)

        countDownTimer = object : CountDownTimer(durationForTimer, timerIntervalMillis) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeInMillis = millisUntilFinished
                val progressPercent = (millisUntilFinished * 100 / timeLeftInMillis).toInt()
                binding.timerProgressBar.progress = progressPercent.coerceIn(0,100)

                val drawableResId = when {
                    progressPercent > GREEN_THRESHOLD_PERCENT -> R.drawable.progress_green
                    progressPercent > YELLOW_THRESHOLD_PERCENT -> R.drawable.progress_yellow
                    else -> R.drawable.progress_red
                }
                binding.timerProgressBar.progressDrawable = ContextCompat.getDrawable(this@TestActivity, drawableResId)
            }

            override fun onFinish() {
                remainingTimeInMillis = 0
                binding.timerProgressBar.progress = 0
                binding.timerProgressBar.progressDrawable = ContextCompat.getDrawable(this@TestActivity, R.drawable.progress_red)
                Log.d("TestActivity", "Tempo esgotado para a questão!")
                handleTimeUp()
            }
        }.start()
    }


    private fun handleTimeUp() {
        pauseTimer()
        disableAnswerButtons()
        scoreManager.onWrongAnswer()
        wrongAnswersCount++
        configurarPontuacao()
        vibrateOnWrongAnswer()

        questionsAnswered++
        totalTime += timeLeftInMillis
        updateQuestionsRemaining()

        if (wrongAnswersCount >= maxWrongAnswers) {
            Log.d("TestActivity", "Limite de erros atingido em handleTimeUp.")
            showEndOfGameDialog()
        } else {
            Log.d("TestActivity", "Tempo esgotado, avançando para a próxima questão.")
            advanceToNextQuestionWithDelay(false)
        }
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        isTimerPaused = true
        Log.d("TestActivity", "Timer pausado. Tempo restante no momento da pausa: $remainingTimeInMillis")
    }

    private fun resumeTimer() {
        if (isTimerPaused && remainingTimeInMillis > 0 && questions.isNotEmpty() && currentQuestionIndex < questions.size) {
            startTimer()
            Log.d("TestActivity", "Timer resumido a partir de $remainingTimeInMillis ms.")
        } else if (isTimerPaused) {
            Log.d("TestActivity", "Tentativa de resumir, mas o tempo pode ter acabado ou não há questões.")
        }
    }

    private fun updateQuestionsRemaining() {
        val questionsLeft = totalQuestions - currentQuestionIndex
        binding.questionsRemainingTextView.text = getString(R.string.perguntas_restantes_format, questionsLeft.coerceAtLeast(0))
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun vibrateOnWrongAnswer() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(300)
        }
    }

    private fun checkAnswer(selectedOptionIndex: Int) {
        if (currentQuestionIndex >= questions.size) {
            Log.w("TestActivity", "checkAnswer chamado, mas não há mais questões.")
            return
        }

        pauseTimer()
        disableAnswerButtons()

        val question = questions[currentQuestionIndex]
        val isCorrect = selectedOptionIndex == question.correctAnswerIndex

        updateButtonStyles(selectedOptionIndex, isCorrect, question.correctAnswerIndex)

        if (isCorrect) {
            scoreManager.onCorrectAnswer()
            handleCorrectAnswer()
            correctAnswersCount++
        } else {
            scoreManager.onWrongAnswer()
            handleWrongAnswer()
        }

        GameDataManager.saveProgresso(
            context = this,
            nivel = (currentQuestionIndex + 1).toString(),
            pontuacao = scoreManager.getTotalScore(),
            erros = GameDataManager.totalErrors
        )
        configurarPontuacao()

        questionsAnswered++
        totalTime += (timeLeftInMillis - remainingTimeInMillis)

        advanceToNextQuestionWithDelay(isCorrect)
    }

    private fun updateButtonStyles(selectedOptionIndex: Int, isCorrect: Boolean, correctAnswerIndex: Int) {
        val buttons = listOf(
            binding.option1Button,
            binding.option2Button,
            binding.option3Button,
            binding.option4Button
        )

        buttons.forEachIndexed { index, button ->
            when {
                index == correctAnswerIndex -> { // Botão da resposta correta
                    button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.correctAnswerColor)
                    button.setTextColor(ContextCompat.getColor(this, R.color.correctAnswerColor))
                }
                index == selectedOptionIndex && !isCorrect -> { // Botão selecionado pelo utilizador, e estava errado
                    button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.wrongAnswerColor)
                    button.setTextColor(ContextCompat.getColor(this, R.color.wrongAnswerColor))
                }
                // Dentro de updateButtonStyles
                else -> { // Outros botões (não selecionados ou não eram a resposta correta, se o utilizador errou)
                    // Voltam para o estilo padrão.
                    button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_default)
                    button.setTextColor(ContextCompat.getColor(this, R.color.background_color))
                }
            }
        }
    }


    private fun handleCorrectAnswer() {
        correctSound.start()
        showMotivationalMessage(correctAnswersCount)
    }

    private fun handleWrongAnswer() {
        wrongSound.start()
        vibrateOnWrongAnswer()
        wrongAnswersCount++

        if (wrongAnswersCount >= maxWrongAnswers) {
            Log.d("TestActivity", "Limite de erros atingido em handleWrongAnswer.")
            showEndOfGameDialog()
        }
    }

    private fun advanceToNextQuestionWithDelay(wasCorrect: Boolean) {
        lifecycleScope.launch {
            delay(if (wasCorrect) 1000L else 2000L)
            currentQuestionIndex++
            if (currentQuestionIndex < questions.size && wrongAnswersCount < maxWrongAnswers) {
                displayQuestion()
            } else if (wrongAnswersCount >= maxWrongAnswers) {
                Log.d("TestActivity", "Não avança para próxima questão, limite de erros atingido.")
            }
            else {
                Log.d("TestActivity", "Fim das questões, navegando para resultados.")
                navigateToResultActivity("RESULT")
            }
        }
    }

    private fun verificarDesbloqueio() {
        val niveisDesbloqueadosSet = mutableSetOf(GameDataManager.Levels.INICIANTE)
        val unlockedFromDataManager = GameDataManager.getUnlockedLevels(this)
        niveisDesbloqueadosSet.addAll(unlockedFromDataManager)
        GameDataManager.saveUnlockedLevels(this, niveisDesbloqueadosSet)

        niveisDesbloqueadosSet.forEach { nivel -> Log.d("TestActivity", "Nível desbloqueado: $nivel") }

        val novoNivelDesbloqueadoParaToast = niveisDesbloqueadosSet.maxByOrNull {
            when (it) {
                GameDataManager.Levels.INICIANTE -> 1
                GameDataManager.Levels.INTERMEDIARIO -> 2
                GameDataManager.Levels.AVANCADO -> 3
                GameDataManager.Levels.ESPECIALISTA -> 4
                else -> 0
            }
        } ?: GameDataManager.Levels.INICIANTE

        Log.d("TestActivity", "Nível mais alto desbloqueado para Toast: $novoNivelDesbloqueadoParaToast")
    }

    private fun observeScoreManager() {
        scoreManager.currentPoints.observe(this) { points ->
            Log.d("TestActivity", "currentPoints observado: $points")
            configurarPontuacao()
        }
        scoreManager.currentStreakLive.observe(this) { streak ->
            Log.d("TestActivity", "currentStreakLive observado: $streak")
            configurarPontuacao()
        }
    }

    private fun updatePlayerProgress(points: Int) {
        if (points > 0) {
            Log.d("TestActivity", "UpdatePlayerProgress: Adicionando $points pontos (externos) ao LevelManager.")
            val levelUp = levelManager.addPointsAndCheckLevelUp(points)
            if (levelUp) {
                Toast.makeText(this, getString(R.string.novo_n_vel_desbloqueado_format_int, levelManager.getTotalPoints()), Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Deprecated("Este método foi descontinuado. Use as APIs Activity Result.", ReplaceWith("registerForActivityResult(...)"))
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == NEXT_PHASE_REQUEST_CODE && resultCode == RESULT_OK) {
            resumeTimer()
        }
    }

    private fun showMotivationalMessage(currentCorrectAnswers: Int) {
        val message = when (correctAnswersCount) { // Usa correctAnswersCount da classe
            5 -> getString(R.string.mensagem_habilidade_desbloqueada)
            10 -> getString(R.string.mensagem_incr_vel)
            15 -> getString(R.string.mensagem_impar_vel)
            else -> null
        }
        message?.let {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
    }

    private fun disableAnswerButtons() {
        val buttons = listOf(binding.option1Button, binding.option2Button, binding.option3Button, binding.option4Button)
        buttons.forEach { it.isEnabled = false }
    }

    private fun enableAnswerButtons() {
        val buttons = listOf(binding.option1Button, binding.option2Button, binding.option3Button, binding.option4Button)
        buttons.forEach { it.isEnabled = true }
    }

    private fun calculateAverageTime(): Double {
        return if (questionsAnswered > 0) totalTime.toDouble() / questionsAnswered / 1000.0
        else 0.0
    }

    private fun showEndOfGameDialog() {
        countDownTimer?.cancel()

        val overlayView = View(this).apply {
            setBackgroundColor(Color.parseColor("#AA000000"))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            isClickable = true
            isFocusable = true
        }
        try {
            if (overlayView.parent == null && rootLayout.indexOfChild(overlayView) == -1) {
                rootLayout.addView(overlayView)
            }
        } catch (e: Exception) {
            Log.e("TestActivity", "Erro ao adicionar overlay: ${e.message}")
        }

        val dialogBuilder = AlertDialog.Builder(this, R.style.TransparentDialogTheme)
            .setTitle(getString(R.string.game_over_title))
            .setMessage(getString(R.string.game_over_message))
            .setPositiveButton(getString(R.string.reiniciar_jogo)) { dialogInterface, _ ->
                if(overlayView.parent != null) rootLayout.removeView(overlayView)
                dialogInterface.dismiss()
                restartGame()
            }
            .setNegativeButton(getString(R.string.ver_resultados)) { dialogInterface, _ ->
                if(overlayView.parent != null) rootLayout.removeView(overlayView)
                dialogInterface.dismiss()
                navigateToResultActivity("GAME_OVER")
            }
            .setOnCancelListener {
                if(overlayView.parent != null) rootLayout.removeView(overlayView)
                navigateToResultActivity("GAME_OVER")
            }

        val dialog = dialogBuilder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()

        val autoActionTimer = object : CountDownTimer(10000, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                if (dialog.isShowing) {
                    dialog.setMessage(
                        "${getString(R.string.game_over_message)}\n\n${getString(R.string.reiniciando_em)} ${millisUntilFinished / 1000} ${getString(R.string.segundos)}"
                    )
                } else {
                    this.cancel()
                }
            }
            override fun onFinish() {
                if (dialog.isShowing) {
                    dialog.dismiss()
                    if(overlayView.parent != null) rootLayout.removeView(overlayView)
                    navigateToResultActivity("GAME_OVER")
                }
            }
        }
        autoActionTimer.start()
    }

    private fun restartGame() {
        currentQuestionIndex = 0
        wrongAnswersCount = 0
        questionsAnswered = 0
        score = 0
        correctAnswersCount = 0
        totalTime = 0L
        remainingTimeInMillis = 0L
        isTimerPaused = false

        scoreManager.reset()
        configurarPontuacao()
        updateQuestionsRemaining()

        if (questions.isNotEmpty()) {
            displayQuestion()
        } else {
            Log.e("TestActivity", "Tentativa de reiniciar o jogo sem questões carregadas.")
            finish()
        }
    }

    private fun navigateToResultActivity(screenType: String) {
        countDownTimer?.cancel()

        val intent = Intent(this, ResultOrGameOverActivity::class.java).apply {
            putExtra("SCREEN_TYPE", screenType)
            putExtra("SCORE", scoreManager.getTotalScore())
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
        animationSound?.pause()
        pauseTimer()
        Log.d("TestActivity", "onPause chamado, timer e som de animação pausados.")
    }

    override fun onResume() {
        super.onResume()
        animationSound?.let {
            if (!it.isPlaying && binding.lottieAnimationView.isAnimating) {
                it.start()
            }
        }
        if (isTimerPaused && questions.isNotEmpty() && currentQuestionIndex < questions.size) {
            resumeTimer()
        }
        Log.d("TestActivity", "onResume chamado.")
    }

    override fun onDestroy() {
        super.onDestroy()
        correctSound.release()
        wrongSound.release()
        countDownTimer?.cancel()
        animationSound?.stop()
        animationSound?.release()
        animationSound = null
        Log.d("TestActivity", "onDestroy chamado, recursos liberados.")
    }
}
