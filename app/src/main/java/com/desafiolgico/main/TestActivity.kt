package com.desafiolgico.main

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.desafiolgico.BuildConfig
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityTestBinding
import com.desafiolgico.model.Question
import com.desafiolgico.model.QuestionManager
import com.desafiolgico.profile.AvatarSelectionActivity
import com.desafiolgico.utils.CoinManager
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.ScoreManager
import com.desafiolgico.utils.applyEdgeToEdge
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.models.Size
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class TestActivity : AppCompatActivity() {

    // --- Instâncias de Classes e Views ---
    private lateinit var binding: ActivityTestBinding
    private lateinit var questionManager: QuestionManager
    private lateinit var scoreManager: ScoreManager
    private lateinit var levelManager: LevelManager
    private val optionButtons = mutableListOf<MaterialButton>()
    private lateinit var rootLayout: ViewGroup

    // --- Variáveis de Estado do Jogo ---
    private lateinit var questions: List<Question>
    private var currentQuestionIndex = 0
    private var wrongAnswersCount = 0
    private var maxWrongAnswers = 5
    private var totalQuestions = 48
    private var faseFinalizada = false
    private var questionsAnswered = 0
    private var totalTimeAccumulated: Long = 0L
    private var totalTimeInMillis: Long = 0L
    private var currentTimerColorRes: Int = R.drawable.progress_green


    private var scoreAnimator: ValueAnimator? = null

    private lateinit var konfettiView: KonfettiView


    // --- Variáveis de Timer ---
    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 0L
    private var remainingTimeInMillis: Long = 0L
    private var isTimerPaused = false
    private val timerIntervalMillis: Long = 100L

    // --- Variáveis de Mídia e Ads ---
    private lateinit var correctSound: MediaPlayer
    private lateinit var wrongSound: MediaPlayer
    private var introSound: MediaPlayer? = null
    private lateinit var adView: AdView
    private var rewardedAd: RewardedAd? = null

    // --- Constantes e Runnables ---
    companion object {
        private const val GREEN_THRESHOLD_PERCENT = 50
        private const val YELLOW_THRESHOLD_PERCENT = 20

        // Adicione aqui a constante de recompensa do CoinManager, se existir, ou use um valor fixo.
        const val REWARD_AD_COINS = 5

        private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val TEST_BANNER_ID   = "ca-app-pub-3940256099942544/6300978111"
        private const val PROD_REWARDED_ID = "ca-app-pub-4958622518589705/3051012274"
        private const val PROD_BANNER_ID   = "ca-app-pub-4958622518589705/1734854735"
    }

    private val hideInspirationRunnable = Runnable {
        findViewById<TextView>(R.id.inspirationText)?.clearAnimation()
        findViewById<View>(R.id.inspirationBox)?.visibility = View.GONE
    }

    // =============================================================================================
    // CICLO DE VIDA
    // =============================================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialização de Managers e Ads
        GameDataManager.init(this)
        MobileAds.initialize(this) {}
        rootLayout = binding.rootLayoutTest

        questionManager = QuestionManager(LanguageHelper.getLanguage(this))
        scoreManager = ScoreManager(this)

        // 🔹 Se veio de uma fase normal para uma fase secreta, restaura o streak
        val restoredStreak = intent.getIntExtra("currentStreak", 0)
        if (restoredStreak > 0) {
            scoreManager.setCurrentStreak(restoredStreak)
        }
        // 🎯 Já atualiza o texto de streak
        binding.streakTextView.text = getString(R.string.streak_format, restoredStreak)

// Observa mudanças de streak
        scoreManager.currentStreakLive.observe(this) { streak ->
            binding.streakTextView.text = getString(R.string.streak_format, streak)
        }

        observeScoreManager()





        levelManager = LevelManager(this)

        setupBanner()
        loadRewardedAd()
        loadUserHeader()

        konfettiView = binding.konfettiView

        binding.logoImageView.setOnClickListener {
            val intent = Intent(this, AvatarSelectionActivity::class.java)
            startActivity(intent)
        }


        // 1. Variáveis de Nível e Índice
        val nivelDaIntent = intent.getStringExtra("level") ?: GameDataManager.Levels.INICIANTE

// Detecta se estamos ABRINDO uma fase secreta agora
        val isLaunchingSecretLevel =
            GameDataManager.isModoSecretoAtivo &&
                (nivelDaIntent == GameDataManager.SecretLevels.RELAMPAGO ||
                    nivelDaIntent == GameDataManager.SecretLevels.PERFEICAO ||
                    nivelDaIntent == GameDataManager.SecretLevels.ENIGMA)


// Por padrão, carrega o nível da Intent.
        var levelToLoad = nivelDaIntent



        var startIndex = 0

        // depois que você calculou isLaunchingSecretLevel e levelToLoad...
        if (isLaunchingSecretLevel) {
            dispararEfeitoFaseSecreta(levelToLoad)
        }





        val prefs = getSharedPreferences("TempGameData", Context.MODE_PRIVATE)
        val nivelRetorno = GameDataManager.getUltimoNivelNormal(this)
        val backupDisponivel = prefs.getBoolean("is_backup_available", false)

// --- LÓGICA DE RETOMADA (Voltar para o Nível Normal pausado) ---
// Só executa se NÃO estivermos lançando uma fase secreta agora
        if (nivelRetorno != null && backupDisponivel && !isLaunchingSecretLevel) {

            levelToLoad = nivelRetorno

            // Carrega o índice salvo (+1 para ir para a próxima pergunta)
            startIndex = GameDataManager.loadLastQuestionIndex(this, levelToLoad) + 1

            val errorsBackup = prefs.getInt("errors_backup", 0)
            val streakBackup = prefs.getInt("streak_backup", 0)
            val scoreBackup = prefs.getInt("score_backup", 0)

            wrongAnswersCount = errorsBackup
            scoreManager.setCurrentStreak(streakBackup)
            scoreManager.setOverallScore(scoreBackup)

            prefs.edit { clear() }
            GameDataManager.clearUltimoNivelNormal(this)
            GameDataManager.isModoSecretoAtivo = false


        Toast.makeText(
                this,
                "Continuando o jogo em $levelToLoad, Pergunta ${startIndex + 1}",
                Toast.LENGTH_LONG
            ).show()
        }

        questions = questionManager.getQuestionsByLevel(levelToLoad)
        totalQuestions = questions.size
        currentQuestionIndex = startIndex

        setupUI()
        setupGameLogic()
        loadUserData()

        if (questions.isEmpty()) {
            binding.questionTextView.text =
                getString(R.string.nenhuma_pergunta_dispon_vel_para_este_n_vel)
            setOptionsEnabled(false)
        } else {
            inflateOptionButtons()
            setupAnswerButtons()
            displayQuestion() // Exibe a pergunta no índice correto
        }
        updateCoinsUI()
    }

    override fun onPause() {
        if (::adView.isInitialized) adView.pause()
        super.onPause()
        introSound?.pause()
        countDownTimer?.cancel()
        pauseTimer()
    }

    override fun onResume() {
        super.onResume()
        if (::adView.isInitialized) adView.resume()
        introSound?.let { if (!it.isPlaying && binding.lottieAnimationView.isAnimating) it.start() }
        if (isTimerPaused && questions.isNotEmpty() && currentQuestionIndex < questions.size) resumeTimer()

        updateCoinsUI()
        loadUserHeader()
    }

    override fun onDestroy() {
        if (::adView.isInitialized) adView.destroy()
        findViewById<View>(R.id.inspirationBox)?.removeCallbacks(hideInspirationRunnable)
        if (::correctSound.isInitialized) correctSound.release()
        if (::wrongSound.isInitialized) wrongSound.release()
        countDownTimer?.cancel()
        introSound?.stop()
        introSound?.release()
        introSound = null
        super.onDestroy()
    }

    // =============================================================================================
    // INICIALIZAÇÃO E SETUP
    // =============================================================================================

    private fun setupUI() {
        mostrarAnimacaoDeIntroducao()
        configurarAudio()
        configurarNivel()
    }

    private fun setupGameLogic() {

        configurarPontuacao()
        configurarTituloNivel()
        updateQuestionsRemaining()
    }

    private fun loadUserData() {
        val (username, photoUrl, avatarId) = GameDataManager.loadUserData(this)

        binding.welcomeUsername.text =
            if (username.isNullOrBlank()) getString(R.string.username) else username

        // ✅ regra: avatar só aparece se estiver desbloqueado (avatar1 é grátis)
        val canUseAvatar = (avatarId != null && CoinManager.isAvatarUnlocked(this, avatarId))

        when {
            canUseAvatar -> {
                binding.logoImageView.setImageResource(avatarId!!)
            }

            !photoUrl.isNullOrEmpty() -> {
                Glide.with(this)
                    .load(photoUrl)
                    .circleCrop()
                    .into(binding.logoImageView)
            }

            else -> {
                // fallback: grátis
                binding.logoImageView.setImageResource(R.drawable.avatar1)
            }
        }
    }


    private fun configurarAudio() {
        correctSound = MediaPlayer.create(this, R.raw.correct_sound)
        wrongSound = MediaPlayer.create(this, R.raw.wrong_sound)
    }

    private fun configurarNivel() {
        val level = intent.getStringExtra("level") ?: GameDataManager.Levels.INICIANTE
        maxWrongAnswers = when (level) {
            GameDataManager.Levels.INTERMEDIARIO -> 3
            GameDataManager.Levels.AVANCADO -> 2
            GameDataManager.SecretLevels.RELAMPAGO -> 1
            GameDataManager.SecretLevels.PERFEICAO -> 1
            GameDataManager.SecretLevels.ENIGMA -> 2
            GameDataManager .Levels.EXPERIENTE -> 2
            else -> 5
        }
    }

    private fun configurarTituloNivel() {
        val level = intent.getStringExtra("level") ?: GameDataManager.Levels.INICIANTE
        binding.levelTextView.text = getString(R.string.nivel_format_string, level)
    }

    private fun inflateOptionButtons() {
        optionButtons.clear()
        repeat(4) {
            val item =
                layoutInflater.inflate(R.layout.item_option_button, binding.gameElements, false)
            val button = item.findViewById<MaterialButton>(R.id.optionButton)
            binding.gameElements.addView(item)
            optionButtons.add(button)
        }
    }

    private fun setupAnswerButtons() {
        optionButtons.forEachIndexed { index, btn ->
            btn.setOnClickListener {
                vibrateTap()
                checkAnswer(index)
            }
        }
    }

    private fun mostrarAnimacaoDeIntroducao() {
        binding.gameElements.visibility = View.GONE
        try {
            introSound?.release()
        } catch (_: Exception) {
        }
        introSound = MediaPlayer.create(this, R.raw.background_music)
        binding.lottieAnimationView.apply {
            setAnimation(R.raw.airplane_explosion1)
            repeatCount = 0
            visibility = View.VISIBLE
            playAnimation()
            introSound?.start()
            removeAllAnimatorListeners()
            addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                    binding.gameElements.visibility = View.VISIBLE
                    try {
                        introSound?.stop(); introSound?.release()
                    } catch (_: Exception) {
                    }
                    introSound = null
                    displayQuestion()
                }

                override fun onAnimationCancel(animation: Animator) {
                    visibility = View.GONE
                    binding.gameElements.visibility = View.VISIBLE
                    try {
                        introSound?.stop(); introSound?.release()
                    } catch (_: Exception) {
                    }
                    introSound = null
                    displayQuestion()
                }

                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }
    }

    // =============================================================================================
    // LÓGICA DO QUIZ E FLUXO
    // =============================================================================================

    private fun displayQuestion() {
        if (currentQuestionIndex >= questions.size) {
            faseFinalizada = true
            navigateToResultActivity("RESULT")
            return
        }

        val q = questions[currentQuestionIndex]
        binding.questionTextView.text = q.questionText

        if (optionButtons.isEmpty()) {
            inflateOptionButtons()
            setupAnswerButtons()
        }
        optionButtons.forEachIndexed { i, btn ->
            if (i < q.options.size) {
                btn.text = q.options[i]; btn.visibility = View.VISIBLE
            } else btn.visibility = View.GONE
        }

        isTimerPaused = false
        remainingTimeInMillis = 0L
        resetButtonStyles()
        setOptionsEnabled(true)
        startTimer()
        updateQuestionsRemaining()
    }

    private fun checkAnswer(selectedIndex: Int) {
        if (currentQuestionIndex >= questions.size) return
        pauseTimer()
        setOptionsEnabled(false)

        val q = questions[currentQuestionIndex]
        val isCorrect = selectedIndex == q.correctAnswerIndex

        // ✅ Agora só pinta o botão escolhido (verde ou vermelho)
        paintButtonsForAnswer(selectedIndex, isCorrect)

        if (isCorrect) handleCorrectAnswer() else handleWrongAnswer()

        configurarPontuacao()
        questionsAnswered++
        totalTimeAccumulated += (timeLeftInMillis - remainingTimeInMillis)
        advanceToNextQuestionWithDelay(isCorrect)
    }

    private fun loadUserHeader() {
        val (username, photoUrl, avatarResId) = GameDataManager.loadUserData(this)

        val displayName = if (username.isNullOrBlank()) {
            getString(R.string.default_username)
        } else username

        binding.welcomeUsername.text = displayName

        val canUseAvatar = (avatarResId != null && CoinManager.isAvatarUnlocked(this, avatarResId))

        when {
            canUseAvatar -> binding.logoImageView.setImageResource(avatarResId!!)
            !photoUrl.isNullOrEmpty() -> {
                Glide.with(this).load(photoUrl).circleCrop().into(binding.logoImageView)
            }
            else -> binding.logoImageView.setImageResource(R.drawable.avatar1)
        }
    }


    @SuppressLint("SetTextI18n")
    private fun handleCorrectAnswer() {
        if (faseFinalizada) return

        // 1. Áudio e Feedback
        try {
            MediaPlayer.create(this, R.raw.correct_sound)?.apply {
                setOnCompletionListener { it.release() }; start()
            }
        } catch (_: Exception) {
        }

        // 2. Cálculo do Tempo Total para enviar ao ScoreManager
        val nivelAtual = intent.getStringExtra("level") ?: GameDataManager.Levels.INICIANTE

        // Obter o tempo total da questão (lógica espelhada do startTimer())
        val totalTimeInMillis = when (nivelAtual) {
            GameDataManager.Levels.INTERMEDIARIO -> 20000L
            GameDataManager.Levels.AVANCADO -> 15000L
            else -> 30000L // Inclui INICIANTE e todos os modos secretos (que usam o default)
        }

        // 3. ENVIAR PARA O SCORE MANAGER (Cálculo Centralizado)
        // O ScoreManager calcula os pontos (Base + Streak + Tempo + Ouro) e incrementa a Streak.
        scoreManager.addScore(
            remainingTimeInMillis,
            totalTimeInMillis
        ) // <-- CHAMADA SIMPLIFICADA E CORRETA

        // 4. Lógica de Desbloqueio/Gatilho

        // NOTA: Agora, a streak é incrementada DENTRO do ScoreManager.
        // Pegamos o valor ATUALIZADO (newStreak) para o gatilho.
        val streakCalculo = scoreManager.currentStreakLive.value ?: 0

        val nenhumPausado = GameDataManager.getUltimoNivelNormal(this) == null

        when (nivelAtual) {
            // Níveis normais podem ser interrompidos para a fase secreta
            GameDataManager.Levels.INICIANTE -> if (streakCalculo == 10 && nenhumPausado) {
                iniciarModoSecreto(GameDataManager.SecretLevels.RELAMPAGO); return
            }

            GameDataManager.Levels.INTERMEDIARIO -> if (streakCalculo == 10 && nenhumPausado) {
                iniciarModoSecreto(GameDataManager.SecretLevels.PERFEICAO); return
            }

            GameDataManager.Levels.AVANCADO -> if (streakCalculo == 8 && nenhumPausado) {
                iniciarModoSecreto(GameDataManager.SecretLevels.ENIGMA); return
            }

            // Fases Secretas NUNCA devem ter gatilhos para outras fases secretas.
            // Se o nível é Relâmpago/Perfeição/Enigma, a lógica de retorno já está em navigateToResultActivity.
        }
    }
    private fun handleWrongAnswer() {
        try {
            MediaPlayer.create(this, R.raw.wrong_sound)
                ?.apply {
                    setOnCompletionListener { it.release() }
                    start()
                }
        } catch (_: Exception) {
            // Se quiser, loga no Crashlytics depois
        }

        vibrateWrong()

        scoreManager.resetStreak()
        wrongAnswersCount++
        scoreManager.onWrongAnswer() // mantém sua lógica de pontuação

        // ❌ NÃO chama showEndOfGameDialog() aqui.
        // Quem decide isso agora é advanceToNextQuestionWithDelay()
    }


    private fun advanceToNextQuestionWithDelay(wasCorrect: Boolean) {
        lifecycleScope.launch {
            delay(if (wasCorrect) 900L else 1600L)
            currentQuestionIndex++
            when {
                currentQuestionIndex < questions.size && wrongAnswersCount < maxWrongAnswers -> displayQuestion()
                wrongAnswersCount >= maxWrongAnswers -> showEndOfGameDialog()
                else -> navigateToResultActivity("RESULT")
            }
        }
    }

    // =============================================================================================
    // LÓGICA FASE SECRETA
    // =============================================================================================

    private fun iniciarModoSecreto(secretLevel: String) {
        // Cancela o timer atual
        countDownTimer?.cancel()

        val nivelAtual = intent.getStringExtra("level") ?: GameDataManager.Levels.INICIANTE
        val currentStreak = scoreManager.currentStreakLive.value ?: 0

        // 1. SALVA O PROGRESSO DA PERGUNTA ATUAL
        GameDataManager.saveLastQuestionIndex(this, nivelAtual, currentQuestionIndex)

        // 2. SALVA FLAGS DE RETORNO
        GameDataManager.setUltimoNivelNormal(this, nivelAtual)
        GameDataManager.isModoSecretoAtivo = true

        // 3. SALVA O ESTADO ATUAL NO BACKUP TEMPORÁRIO
        val prefs = getSharedPreferences("TempGameData", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("score_backup", scoreManager.getOverallScore())
            putInt("errors_backup", wrongAnswersCount)
            putInt("streak_backup", currentStreak) // usa o streak atual
            putBoolean("is_backup_available", true)
            apply()
        }

        // ❌ NÃO zerar a streak aqui
        // scoreManager.resetStreak()  // REMOVER

        Toast.makeText(
            this,
            "⚡ Fase secreta: ${secretLevel.uppercase()} desbloqueada!",
            Toast.LENGTH_LONG
        ).show()

        // 4. Abre a fase secreta levando o streak atual
        val intent = Intent(this, TestActivity::class.java).apply {
            putExtra("level", secretLevel)
            putExtra("RETURN_TO_ACTIVE_GAME", true)
            putExtra("currentStreak", currentStreak)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun navigateToResultActivity(screenType: String) {
        countDownTimer?.cancel()
        val totalScoreDaFaseAtual = scoreManager.getOverallScore()
        val isSecretLevel = GameDataManager.isModoSecretoAtivo

        val resultIntent = Intent(this, ResultOrGameOverActivity::class.java).apply {
            putExtra("SCREEN_TYPE", screenType)
            putExtra("SCORE", totalScoreDaFaseAtual)
            putExtra("WRONG_ANSWERS", wrongAnswersCount)
            putExtra("MAX_WRONG_ANSWERS", maxWrongAnswers)
            putExtra("TOTAL_QUESTIONS", totalQuestions)
            putExtra("AVERAGE_TIME", avgTimeSeconds())
        }

        val onAdDismissed = {
            if (isSecretLevel) {
                // FLUXO DE RETORNO DO MODO SECRETO
                val nivelAnterior =
                    GameDataManager.ultimoNivelNormal ?: GameDataManager.Levels.INICIANTE
                Toast.makeText(
                    this,
                    "⚡ Fase Relâmpago concluída! Bônus aplicados.",
                    Toast.LENGTH_SHORT
                ).show()

                val prefs = getSharedPreferences("TempGameData", Context.MODE_PRIVATE)
                val oldScore = prefs.getInt("score_backup", 0)

                // 1. Calcula e atualiza o score total
                val scoreFinalTotal = oldScore + totalScoreDaFaseAtual

                // 2. Aplica Bônus Permanentes
                // Supondo que CoinManager.rewardSecretStage() também aplica XP
                CoinManager.rewardForSecretLevelCompletion(this)

                // 3. ATUALIZA O BACKUP TEMPORÁRIO com a PONTUAÇÃO FINAL
                // O onCreate usará este valor
                prefs.edit().apply {
                    putInt("score_backup", scoreFinalTotal)
                    apply()
                }

                // Inicia o Quiz do Nível Normal. O onCreate dele fará a retomada.
                startActivity(Intent(this, TestActivity::class.java).apply {
                    putExtra("level", nivelAnterior)
                })
                finish()
            } else {
                // FLUXO NORMAL
                startActivity(resultIntent)
                finish()
            }
        }

        showRewardedAdIfAvailable(onAdDismissed)
    }

    // =============================================================================================
    // TIMER E PROGRESSO
    // =============================================================================================


    private fun startTimer() {
        countDownTimer?.cancel()

        val level = intent.getStringExtra("level")
        val baseTime = when (level) {
            GameDataManager.Levels.INTERMEDIARIO -> 20_000L
            GameDataManager.Levels.AVANCADO -> 15_000L
            else -> 30_000L
        }

        // ✅ base fixa da pergunta
        totalTimeInMillis = baseTime
        timeLeftInMillis = baseTime

        // ✅ duração real do timer (se retomou, continua do restante)
        val duration = if (isTimerPaused && remainingTimeInMillis > 0L) {
            remainingTimeInMillis
        } else {
            baseTime
        }

        isTimerPaused = false
        remainingTimeInMillis = duration

        // Ajusta UI inicial
        binding.timerTextView.text = formatTime(duration)
        binding.timerProgressBar.max = 100
        val startPercent = (duration * 100 / baseTime).toInt().coerceIn(0, 100)
        binding.timerProgressBar.progress = startPercent

        currentTimerColorRes = R.drawable.progress_green
        binding.timerProgressBar.progressDrawable =
            ContextCompat.getDrawable(this, currentTimerColorRes)

        countDownTimer = object : CountDownTimer(duration, timerIntervalMillis) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeInMillis = millisUntilFinished

                val progressPercent =
                    (millisUntilFinished * 100 / baseTime).toInt().coerceIn(0, 100)

                binding.timerTextView.text = formatTime(millisUntilFinished)
                binding.timerProgressBar.progress = progressPercent

                val newColorRes = when {
                    progressPercent > GREEN_THRESHOLD_PERCENT -> R.drawable.progress_green
                    progressPercent > YELLOW_THRESHOLD_PERCENT -> R.drawable.progress_yellow
                    else -> R.drawable.progress_red
                }

                if (newColorRes != currentTimerColorRes) {
                    currentTimerColorRes = newColorRes
                    binding.timerProgressBar.progressDrawable =
                        ContextCompat.getDrawable(this@TestActivity, newColorRes)
                }
            }

            override fun onFinish() {
                remainingTimeInMillis = 0L
                isTimerPaused = false
                binding.timerProgressBar.progress = 0
                binding.timerProgressBar.progressDrawable =
                    ContextCompat.getDrawable(this@TestActivity, R.drawable.progress_red)
                binding.timerTextView.text = "00:00"
                handleTimeUp()
            }
        }.start()
    }


    private fun formatTime(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format("%02d:%02d", m, s)
    }


    private fun pauseTimer() {
        countDownTimer?.cancel(); isTimerPaused = true
    }

    private fun resumeTimer() {
        if (isTimerPaused && remainingTimeInMillis > 0 && currentQuestionIndex < questions.size) startTimer()
    }

    private fun handleTimeUp() {
        pauseTimer()
        setOptionsEnabled(false)
        scoreManager.resetStreak()
        scoreManager.onWrongAnswer()
        wrongAnswersCount++
        configurarPontuacao()
        vibrateWrong()
        questionsAnswered++
        totalTimeAccumulated += timeLeftInMillis
        updateQuestionsRemaining()
        if (wrongAnswersCount >= maxWrongAnswers) showEndOfGameDialog()
        else advanceToNextQuestionWithDelay(false)
    }

    // =============================================================================================
    // UI, Animações e Dialogs
    // =============================================================================================




        private fun paintButtonsForAnswer(selected: Int, isCorrect: Boolean) {

        val whiteColor = ContextCompat.getColor(this, android.R.color.white)
        val backgroundColor = ContextCompat.getColor(this, R.color.background_color)
        val correctTint = ContextCompat.getColorStateList(this, R.color.correctAnswerColor)
        val wrongTint = ContextCompat.getColorStateList(this, R.color.wrongAnswerColor)
        val defaultTint = ContextCompat.getColorStateList(this, R.color.button_default)

        optionButtons.forEachIndexed { i, b ->
            when {
                // Acertou → botão selecionado fica verde
                i == selected && isCorrect -> {
                    b.backgroundTintList = correctTint
                    b.setTextColor(whiteColor)
                }

                // Errou → botão selecionado fica vermelho
                i == selected && !isCorrect -> {
                    b.backgroundTintList = wrongTint
                    b.setTextColor(whiteColor)
                }

                // Todos os outros voltam pro estilo padrão
                else -> {
                    b.backgroundTintList = defaultTint
                    b.setTextColor(backgroundColor)
                }
            }
        }
    }

    private fun resetButtonStyles() {
        val defaultTextColor = ContextCompat.getColor(this, R.color.background_color)
        val defaultTint = ContextCompat.getColorStateList(this, R.color.button_default)
        val defaultStroke = resources.getDimensionPixelSize(R.dimen.option_card_stroke)

        optionButtons.forEach { b ->
            // Reset visual do botão
            b.isEnabled = true
            b.setTextColor(defaultTextColor)
            b.backgroundTintList = defaultTint

            // Reset visual do card pai
            val parentCard = b.parent as? com.google.android.material.card.MaterialCardView
            parentCard?.apply {
                setCardBackgroundColor(
                    ContextCompat.getColor(context, android.R.color.transparent)
                )
                strokeWidth = defaultStroke
            }
        }
    }


    private fun setOptionsEnabled(enabled: Boolean) {
        optionButtons.forEach { it.isEnabled = enabled }
    }

    private fun updateQuestionsRemaining() {
        val left = (totalQuestions - currentQuestionIndex).coerceAtLeast(0)
        binding.questionsRemainingTextView.text =
            getString(R.string.perguntas_restantes_format, left)
    }

    private fun configurarPontuacao() {
        val oldScore = try {
            binding.scoreTextView.text.replace("[^0-9-]".toRegex(), "").toInt()
        } catch (_: Exception) {
            0
        }
        val newScore = scoreManager.getOverallScore()
        animateScoreChange(oldScore, newScore)

        val currentStreak = scoreManager.currentStreakLive.value ?: 0
        binding.streakTextView.text = getString(R.string.streak_format, currentStreak)
        binding.streakTextView.visibility = View.VISIBLE
    }


    private fun animateScoreChange(oldScore: Int, newScore: Int) {
        val tv = binding.scoreTextView

        if (oldScore == newScore) {
            tv.text = getString(R.string.pontos_format, newScore)
            return
        }

        // Cancela animação anterior se ainda estiver rodando
        scoreAnimator?.cancel()

        val diff = kotlin.math.abs(newScore - oldScore)

        // Duração baseada na diferença, mas com limites
        val duration = (300 + diff * 12)
            .coerceIn(500, 1200) // deixei no máx 1.2s pra não ficar arrastado demais
            .toLong()

        scoreAnimator = ValueAnimator.ofInt(oldScore, newScore).apply {
            this.duration = duration
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()

            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                tv.text = getString(R.string.pontos_format, value)
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    // pulso de crescimento
                    tv.animate()
                        .scaleX(1.08f)
                        .scaleY(1.08f)
                        .setDuration(140)
                        .start()
                }

                override fun onAnimationEnd(animation: Animator) {
                    // volta ao normal suave
                    tv.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(160)
                        .start()
                }

                override fun onAnimationCancel(animation: Animator) {
                    // Garante que não fique "travado" em escala errada
                    tv.scaleX = 1f
                    tv.scaleY = 1f
                }
            })
        }

        scoreAnimator?.start()
    }

    private fun updateCoinsUI() {
        val coins = CoinManager.getCoins(this)
        binding.coinsTextView.text = getString(R.string.moedas_format, coins)
    }

    private fun showEndOfGameDialog() {
        // Para o timer
        countDownTimer?.cancel()

        // Overlay escuro por trás da dialog, como você já fazia
        val overlay = View(this).apply {
            setBackgroundColor("#AA000000".toColorInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isClickable = true
            isFocusable = true
        }

        try {
            if (overlay.parent == null && rootLayout.indexOfChild(overlay) == -1) {
                rootLayout.addView(overlay)
            }
        } catch (_: Exception) {
        }

        // Infla o layout customizado
        val dialogView = layoutInflater.inflate(R.layout.dialog_game_over_small, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvGameOverTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvGameOverMessage)
        val btnRestart = dialogView.findViewById<MaterialButton>(R.id.btnRestart)
        val btnSeeResults = dialogView.findViewById<MaterialButton>(R.id.btnSeeResults)

        // Garante que está usando as strings (caso queira mudar depois por idioma)
        tvTitle.text = getString(R.string.game_over_title)
        tvMessage.text = getString(R.string.game_over_message)

        val dialog = AlertDialog.Builder(this, R.style.TransparentDialogTheme)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnRestart.setOnClickListener {
            try { rootLayout.removeView(overlay) } catch (_: Exception) {}
            dialog.dismiss()
            restartGame()
        }

        btnSeeResults.setOnClickListener {
            try { rootLayout.removeView(overlay) } catch (_: Exception) {}
            dialog.dismiss()
            navigateToResultActivity("GAME_OVER")
        }

        dialog.setOnCancelListener {
            try { rootLayout.removeView(overlay) } catch (_: Exception) {}
            navigateToResultActivity("GAME_OVER")
        }

        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    private fun restartGame() {
        faseFinalizada = false
        currentQuestionIndex = 0
        wrongAnswersCount = 0
        questionsAnswered = 0
        totalTimeAccumulated = 0L
        remainingTimeInMillis = 0L
        isTimerPaused = false
        GameDataManager.currentStreak = 0
        scoreManager.reset()
        configurarPontuacao()
        updateQuestionsRemaining()
        if (questions.isNotEmpty()) displayQuestion() else finish()
    }

    // =============================================================================================
    // Helpers, Vibracao e Observadores
    // =============================================================================================

    private fun avgTimeSeconds(): Double =
        if (questionsAnswered > 0) totalTimeAccumulated.toDouble() / questionsAnswered / 1000.0 else 0.0

    private fun vibrateTap() = vibrate(80)
    private fun vibrateWrong() = vibrate(300)
    private fun vibrate(durationMs: Long) {
        val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") v.vibrate(durationMs)
    }


    private fun observeScoreManager() {
        scoreManager.overallScoreLive.observe(this) { configurarPontuacao() }
        scoreManager.currentStreakLive.observe(this) { configurarPontuacao() }

        scoreManager.highestStreakLive.observe(this) { /* Atualizar UI se necessário */ }

    }

    // =============================================================================================
    // ADMOB
    // =============================================================================================

    // ✅ FUNÇÕES (tem que existir, senão dá Unresolved reference)
    private fun rewardedUnitId(): String =
        if (BuildConfig.DEBUG) TEST_REWARDED_ID else PROD_REWARDED_ID

    private fun bannerUnitId(): String =
        if (BuildConfig.DEBUG) TEST_BANNER_ID else PROD_BANNER_ID

    private fun setupBanner() {
        adView = binding.adView



        adView.adListener = object : AdListener() {
            override fun onAdLoaded() { binding.adView.visibility = View.VISIBLE }
            override fun onAdFailedToLoad(error: LoadAdError) { binding.adView.visibility = View.GONE }
        }
        adView.loadAd(AdRequest.Builder().build())
    }

    private fun loadRewardedAd() {
        RewardedAd.load(
            this,
            rewardedUnitId(),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { rewardedAd = null }
            }
        )
    }



    private fun dispararEfeitoFaseSecreta(level: String) {
        val nomeFase = when (level) {
            GameDataManager.SecretLevels.RELAMPAGO -> "Fase Relâmpago ⚡"
            GameDataManager.SecretLevels.PERFEICAO -> "Fase Perfeição 💎"
            GameDataManager.SecretLevels.ENIGMA    -> "Fase Enigma 🧩"
            else -> "Fase Secreta"
        }

        Toast.makeText(this, "Você entrou na $nomeFase!", Toast.LENGTH_SHORT).show()

        // Garante que a view está visível
        konfettiView.visibility = View.VISIBLE

        val party = Party(
            speed = 10f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            angle = 270, // de cima pra baixo
            timeToLive = 3_000L,
            shapes = listOf(Shape.Square, Shape.Circle),
            size = listOf(Size(8)),
            position = Position.Relative(0.5, 0.0), // centro no topo
            emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(50)
        )

        konfettiView.start(party)

        // opcional: esconder depois de alguns segundos
        konfettiView.postDelayed({
            konfettiView.visibility = View.GONE
        }, 3500)
    }


    private fun showRewardedAdIfAvailable(onFinish: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            onFinish(); return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null; loadRewardedAd(); onFinish()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null; loadRewardedAd(); onFinish()
            }
        }
        ad.show(this) { rewardItem ->
            val rewardAmount = rewardItem.amount.takeIf { it > 0 } ?: REWARD_AD_COINS

            CoinManager.addCoins(this, rewardAmount, reason = "AdReward")
            updateCoinsUI()

            Toast.makeText(
                this,
                "💰 +$rewardAmount moedas!",
                Toast.LENGTH_SHORT
            ).show()
        }

    }
}
