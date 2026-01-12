package com.desafiolgico.main

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
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
import com.desafiolgico.utils.LocalRecordsManager
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
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.models.Size
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.text.Normalizer
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

@Suppress("DEPRECATION")
class TestActivity : AppCompatActivity() {

    // --- Binding / Views ---
    private lateinit var binding: ActivityTestBinding
    private lateinit var rootLayout: ViewGroup
    private lateinit var konfettiView: KonfettiView
    private val optionButtons = mutableListOf<MaterialButton>()
    private lateinit var overlayContainer: ViewGroup

    // --- Estado geral ---
    private var currentLevelLoaded: String = ""
    private var secretTarget: Int = 0
    private var faseFinalizada = false
    private var runScoreLevel = 0



    // --- Baralho de PERGUNTAS (100% sem repetir) ---
    private var shuffleSeed: Long = 0L

    // --- Curiosidades (fase secreta) ---
    private var secretCorrectCounter = 0
    private var secretHitsInRow = 0

    private val curiosities = listOf(
        "🌊 Sabia que o coração de um camarão fica na cabeça?",
        "🐘 O elefante é o único animal com quatro joelhos.",
        "🦋 As borboletas sentem o gosto com os pés!",
        "⚡ O relâmpago é mais quente que a superfície do Sol.",
        "🌙 A Lua se afasta da Terra cerca de 3,8 cm por ano."
    )

    // Baralho (sem repetir até acabar)
    private val curiosityBag = ArrayDeque<String>()
    private var lastCuriosity: String? = null

    // Overlay view atual (pra não empilhar)
    private var curiosityOverlayView: View? = null

    // --- Managers ---
    private lateinit var questionManager: QuestionManager
    private lateinit var scoreManager: ScoreManager
    private lateinit var levelManager: LevelManager

    // --- Questões ---
    private lateinit var questions: List<Question>
    private var currentQuestionIndex = 0
    private var wrongAnswersCount = 0
    private var maxWrongAnswers = 5
    private var totalQuestions = 0

    private var questionsAnswered = 0
    private var totalTimeAccumulated: Long = 0L

    // --- Timer ---
    private var countDownTimer: CountDownTimer? = null
    private var isTimerPaused = false
    private var totalTimeInMillis: Long = 0L
    private var remainingTimeInMillis: Long = 0L
    private val timerIntervalMillis: Long = 100L
    private var currentTimerColorRes: Int = R.drawable.progress_green
    private var lastCriticalSecond = -1

    // --- Anti double tap ---
    private var answerLocked = false

    // --- Áudio ---
    private lateinit var correctSound: MediaPlayer
    private lateinit var wrongSound: MediaPlayer
    private var introSound: MediaPlayer? = null

    // --- Ads ---
    private lateinit var adView: AdView
    private var rewardedAd: RewardedAd? = null

    // --- FX AAA leve ---
    private var scoreAnimator: ValueAnimator? = null
    private var fxOverlay: View? = null

    // Glow no card
    private var defaultQuestionStrokeColor: Int? = null
    private var defaultQuestionStrokeWidth: Int? = null

    // micro celebration
    private var lastCelebratedStreak = -1

    companion object {
        private const val GREEN_THRESHOLD_PERCENT = 50
        private const val YELLOW_THRESHOLD_PERCENT = 20


            private const val THRESHOLD_INTERMEDIATE = 3500
            private const val THRESHOLD_ADVANCED = 6000
            private const val THRESHOLD_EXPERT = 10000



        const val REWARD_AD_COINS = 5

        private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val PROD_REWARDED_ID = "ca-app-pub-4958622518589705/3051012274"
        private const val PROD_BANNER_ID = "ca-app-pub-4958622518589705/1734854735"

        private const val PREF_TEMP = "TempGameData"
        private const val KEY_SEED_BACKUP = "seed_backup"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

    // =============================================================================================
    // CICLO DE VIDA
    // =============================================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        GameDataManager.init(this)
        MobileAds.initialize(this) {}

        overlayContainer = binding.overlayContainer
        rootLayout = binding.rootLayoutTest
        konfettiView = binding.konfettiView

        // ajuda a ficar por cima do rodapé (timer/pontos)
        overlayContainer.elevation = dp(30).toFloat()

        ensureFxOverlay()

        questionManager = QuestionManager(LanguageHelper.getLanguage(this))
        scoreManager = ScoreManager(this)
        levelManager = LevelManager(this)

        configurarAudio()
        setupBanner()
        loadRewardedAd()
        loadUserHeader()
        observeScoreManager()

        binding.logoImageView.setOnClickListener {
            startActivity(Intent(this, AvatarSelectionActivity::class.java))
        }

        // --- restore streak se veio de secreto ---
        val restoredStreak = intent.getIntExtra("currentStreak", 0)
        if (restoredStreak > 0) scoreManager.setCurrentStreak(restoredStreak)
        binding.streakTextView.text = getString(R.string.streak_format, restoredStreak)

        // --- nível ---
        val nivelDaIntent = intent.getStringExtra("level") ?: GameDataManager.Levels.INICIANTE

        val isLaunchingSecretLevel =
            GameDataManager.isModoSecretoAtivo &&
                (nivelDaIntent == GameDataManager.SecretLevels.RELAMPAGO ||
                    nivelDaIntent == GameDataManager.SecretLevels.PERFEICAO ||
                    nivelDaIntent == GameDataManager.SecretLevels.ENIGMA)

        var levelToLoad = nivelDaIntent
        var startIndex = 0

        if (isLaunchingSecretLevel) {
            if (isSecretLevel(levelToLoad)) {
                secretCorrectCounter = 0
                secretHitsInRow = 0
                resetCuriosityBag()
            }
            dispararEfeitoFaseSecreta(levelToLoad)
        }

        // --- restore jogo (backup) ---
        val prefs = getSharedPreferences(PREF_TEMP, Context.MODE_PRIVATE)
        val nivelRetorno = GameDataManager.getUltimoNivelNormal(this)
        val backupDisponivel = prefs.getBoolean("is_backup_available", false)

        val isRestoringBackup = (nivelRetorno != null && backupDisponivel && !isLaunchingSecretLevel)

        if (isRestoringBackup) {
            levelToLoad = nivelRetorno!!
            startIndex = GameDataManager.loadLastQuestionIndex(this, levelToLoad) + 1

            wrongAnswersCount = prefs.getInt("errors_backup", 0)
            scoreManager.setCurrentStreak(prefs.getInt("streak_backup", 0))
            scoreManager.setOverallScore(prefs.getInt("score_backup", 0))

            // ✅ seed da partida (para voltar exatamente na mesma ordem)
            shuffleSeed = prefs.getLong(KEY_SEED_BACKUP, 0L)

            prefs.edit { clear() }
            GameDataManager.clearUltimoNivelNormal(this)
            GameDataManager.isModoSecretoAtivo = false

            Toast.makeText(
                this,
                "Continuando o jogo em $levelToLoad, Pergunta ${startIndex + 1}",
                Toast.LENGTH_LONG
            ).show()
        }

        currentLevelLoaded = levelToLoad

        // alvo pra desbloquear fase secreta (só nos níveis normais)
        secretTarget = when (currentLevelLoaded) {
            GameDataManager.Levels.INICIANTE -> 10
            GameDataManager.Levels.INTERMEDIARIO -> 10
            GameDataManager.Levels.AVANCADO -> 8
            else -> 0
        }

        setupSecretProgressUi()

        configurarNivel(levelToLoad)
        configurarTituloNivel(levelToLoad)

        // ✅ se não veio seed do backup, cria um novo seed (ordem aleatória por partida)
        if (shuffleSeed == 0L) shuffleSeed = newSeed()

        // ✅ 100% SEM REPETIR: dedup + shuffle por seed
        questions = buildUniqueShuffledQuestions(levelToLoad, shuffleSeed)
        totalQuestions = questions.size

        // se deduplicou e o startIndex ficou fora, encerra com resultado (evita crash)
        if (startIndex >= totalQuestions && totalQuestions > 0) {
            currentQuestionIndex = totalQuestions
        } else {
            currentQuestionIndex = startIndex
        }
        if (!isRestoringBackup) runScoreLevel = 0


        updateQuestionsRemaining()
        updateCoinsUI()

        if (questions.isEmpty()) {
            binding.questionTextView.text =
                getString(R.string.nenhuma_pergunta_dispon_vel_para_este_n_vel)
            setOptionsEnabled(false)
            return
        }

        inflateOptionButtons()
        setupAnswerButtons()

        cacheQuestionCardDefaults()
        playIntroThenStartGame()
    }

    override fun onResume() {
        super.onResume()
        if (::adView.isInitialized) adView.resume()

        introSound?.let { mp ->
            if (!mp.isPlaying && binding.lottieAnimationView.isAnimating) mp.start()
        }

        if (isTimerPaused && remainingTimeInMillis > 0L && !binding.lottieAnimationView.isAnimating) {
            resumeTimer()
        }

        updateCoinsUI()
        loadUserHeader()
    }

    override fun onPause() {
        if (::adView.isInitialized) adView.pause()
        super.onPause()
        introSound?.pause()
        pauseTimer()
    }

    override fun onDestroy() {
        if (::adView.isInitialized) adView.destroy()
        try { correctSound.release() } catch (_: Exception) {}
        try { wrongSound.release() } catch (_: Exception) {}
        try { introSound?.stop(); introSound?.release() } catch (_: Exception) {}
        introSound = null
        countDownTimer?.cancel()
        scoreAnimator?.cancel()
        super.onDestroy()
    }

    // =============================================================================================
    // PERGUNTAS: 100% SEM REPETIR (dedup agressivo por chave normalizada)
    // =============================================================================================

    private fun newSeed(): Long =
        System.currentTimeMillis() xor (System.nanoTime() shl 1)

    private fun seedToInt(seed: Long): Int =
        (seed xor (seed ushr 32)).toInt()

    private fun normalizeKey(input: String): String {
        val trimmed = input.trim().lowercase()
        if (trimmed.isBlank()) return ""
        val noAccents = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        // remove tudo que não for letra/número (fica “uma chave” estável)
        return noAccents.replace("[^a-z0-9]+".toRegex(), "")
    }

    private fun questionKey(q: Question): String {
        val base = normalizeKey(q.questionText)
        // se vier vazio por algum motivo, usa fallback bem estável
        return if (base.isNotBlank()) base else "fallback_${q.hashCode()}"
    }

    private fun buildUniqueShuffledQuestions(level: String, seed: Long): List<Question> {
        val raw = questionManager.getQuestionsByLevel(level)

        // ✅ remove duplicadas (mesma pergunta, mesmo com espaços/acentos/pontuação diferentes)
        val map = LinkedHashMap<String, Question>()
        for (q in raw) {
            val key = questionKey(q)
            if (!map.containsKey(key)) map[key] = q
        }

        val unique = map.values.toList()
        return unique.shuffled(Random(seedToInt(seed)))
    }

    // =============================================================================================
    // HELPERS DE NÍVEL
    // =============================================================================================

    private fun currentLevel(): String =
        if (currentLevelLoaded.isNotBlank()) currentLevelLoaded
        else intent.getStringExtra("level") ?: GameDataManager.Levels.INICIANTE

    private fun isSecretLevel(level: String): Boolean =
        level == GameDataManager.SecretLevels.RELAMPAGO ||
            level == GameDataManager.SecretLevels.PERFEICAO ||
            level == GameDataManager.SecretLevels.ENIGMA

    // =============================================================================================
    // SETUP
    // =============================================================================================

    private fun configurarAudio() {
        correctSound = MediaPlayer.create(this, R.raw.correct_sound)
        wrongSound = MediaPlayer.create(this, R.raw.wrong_sound)
    }

    private fun configurarNivel(level: String) {
        maxWrongAnswers = when (level) {
            GameDataManager.Levels.INTERMEDIARIO -> 3
            GameDataManager.Levels.AVANCADO -> 2
            GameDataManager.SecretLevels.RELAMPAGO -> 1
            GameDataManager.SecretLevels.PERFEICAO -> 1
            GameDataManager.SecretLevels.ENIGMA -> 2
            GameDataManager.Levels.EXPERIENTE -> 2
            else -> 5
        }
    }

    private fun configurarTituloNivel(level: String) {
        binding.levelTextView.text = getString(R.string.nivel_format_string, level)
    }

    private fun inflateOptionButtons() {
        optionButtons.clear()
        binding.gameElements.removeAllViews()

        // mantém o card da pergunta (já existe no XML via binding)
        binding.gameElements.addView(binding.questionCard)

        repeat(4) {
            val item = layoutInflater.inflate(R.layout.item_option_button, binding.gameElements, false)
            val button = item.findViewById<MaterialButton>(R.id.optionButton)
            binding.gameElements.addView(item)
            optionButtons.add(button)
        }
    }

    private fun setupAnswerButtons() {
        optionButtons.forEachIndexed { index, btn ->
            btn.setOnClickListener {
                if (answerLocked) return@setOnClickListener
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                pop(btn)
                vibrateTap()
                checkAnswer(index)
            }
        }
    }

    private fun playIntroThenStartGame() {
        binding.gameElements.visibility = View.GONE
        answerLocked = true

        try { introSound?.stop(); introSound?.release() } catch (_: Exception) {}
        introSound = MediaPlayer.create(this, R.raw.background_music)

        binding.lottieAnimationView.apply {
            setAnimation(R.raw.airplane_explosion1)
            repeatCount = 0
            visibility = View.VISIBLE
            playAnimation()
        }

        introSound?.start()

        binding.lottieAnimationView.removeAllAnimatorListeners()
        binding.lottieAnimationView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) = finishIntroStartGame()
            override fun onAnimationCancel(animation: Animator) = finishIntroStartGame()
        })
    }

    private fun finishIntroStartGame() {
        binding.lottieAnimationView.visibility = View.GONE
        binding.gameElements.visibility = View.VISIBLE

        try { introSound?.stop(); introSound?.release() } catch (_: Exception) {}
        introSound = null

        answerLocked = false
        displayQuestion(withEnterAnim = true)
    }

    // =============================================================================================
    // QUIZ
    // =============================================================================================

    private fun displayQuestion(withEnterAnim: Boolean = false) {
        if (currentQuestionIndex >= questions.size) {
            faseFinalizada = true
            navigateToResultActivity("RESULT")
            return
        }

        answerLocked = false
        isTimerPaused = false
        lastCriticalSecond = -1

        val q = questions[currentQuestionIndex]

        resetButtonStyles()

        if (withEnterAnim) binding.questionTextView.text = q.questionText
        else animateQuestionSwap(q.questionText)

        optionButtons.forEachIndexed { i, btn ->
            if (i < q.options.size) {
                btn.text = q.options[i]
                btn.visibility = View.VISIBLE
                btn.alpha = 1f
                btn.isEnabled = true
            } else {
                btn.text = ""
                btn.visibility = View.INVISIBLE
                btn.alpha = 0f
                btn.isEnabled = false
            }
        }

        setOptionsEnabled(true)

        binding.gameElementsScrollView.post {
            binding.gameElementsScrollView.smoothScrollTo(0, 0)
        }

        if (withEnterAnim) animateQuestionIn()
        staggerOptions()

        binding.gameElementsScrollView.postDelayed({
            ensureLastOptionVisible(extraBottomDp = 18)
        }, 90L)

        startTimer()
        updateQuestionsRemaining()
    }

    private fun checkAnswer(selectedIndex: Int) {
        if (currentQuestionIndex >= questions.size) return
        if (answerLocked) return

        val selectedBtn = optionButtons.getOrNull(selectedIndex)
        if (selectedBtn == null || selectedBtn.visibility != View.VISIBLE) return

        answerLocked = true
        pauseTimer()
        setOptionsEnabled(false)

        val q = questions[currentQuestionIndex]
        val isCorrect = selectedIndex == q.correctAnswerIndex

        val oldScore = scoreManager.getOverallScore()

        val levelNow = currentLevel()
        secretHitsInRow = if (isSecretLevel(levelNow)) {
            if (isCorrect) secretHitsInRow + 1 else 0
        } else 0

        paintButtonsForAnswer(selectedIndex, q.correctAnswerIndex)

        val spent = (totalTimeInMillis - remainingTimeInMillis).coerceAtLeast(0L)
        questionsAnswered++
        totalTimeAccumulated += spent

        if (isCorrect) {
            handleCorrectAnswer()
            if (isFinishing) return

            val newScore = scoreManager.getOverallScore()
            val gained = (newScore - oldScore).coerceAtLeast(0)
            if (!isSecretLevel(levelNow)) {
                runScoreLevel += gained
            }

            val streakNow = scoreManager.currentStreakLive.value ?: 0

            showFloatingChip("✔ +$gained pts • 🔥 $streakNow", R.drawable.ic_check_circle, true)

            if (streakNow == 5 || streakNow == 10) {
                val coinsReward = 1
                CoinManager.addCoins(this, coinsReward, reason = "StreakBonus")
                updateCoinsUI()
                animateCoinToCounter(selectedBtn, coinsReward)
            }

            glowQuestionCard(success = true)
            flashFx(success = true)
        } else {
            handleWrongAnswer()

            val streakNow = scoreManager.currentStreakLive.value ?: 0
            showFloatingChip("✖ Errou • 🔥 $streakNow", R.drawable.ic_delete, false)

            glowQuestionCard(success = false)
            shake(selectedBtn)
            flashFx(success = false)
        }

        updateSecretProgressUi(scoreManager.currentStreakLive.value ?: 0)
        advanceToNextQuestionWithDelayOrCuriosity(isCorrect)
    }

    @SuppressLint("SetTextI18n")
    private fun handleCorrectAnswer() {
        if (faseFinalizada) return

        try {
            correctSound.seekTo(0)
            correctSound.start()
        } catch (_: Exception) {}

        val nivelAtual = currentLevel()
        val totalDaQuestao = when (nivelAtual) {
            GameDataManager.Levels.INTERMEDIARIO -> 20_000L
            GameDataManager.Levels.AVANCADO -> 15_000L
            else -> 30_000L
        }

        // score
        scoreManager.addScore(remainingTimeInMillis, totalDaQuestao)

        // ✅ conta 1 acerto POR NÍVEL (somente níveis normais)
        if (!isSecretLevel(nivelAtual)) {
            GameDataManager.addCorrectForLevel(this, nivelAtual, 1)
        }

        val streakCalculo = scoreManager.currentStreakLive.value ?: 0
        val nenhumPausado = GameDataManager.getUltimoNivelNormal(this) == null

        when (nivelAtual) {
            GameDataManager.Levels.INICIANTE -> if (streakCalculo == 10 && nenhumPausado) {
                iniciarModoSecreto(GameDataManager.SecretLevels.RELAMPAGO); return
            }
            GameDataManager.Levels.INTERMEDIARIO -> if (streakCalculo == 10 && nenhumPausado) {
                iniciarModoSecreto(GameDataManager.SecretLevels.PERFEICAO); return
            }
            GameDataManager.Levels.AVANCADO -> if (streakCalculo == 8 && nenhumPausado) {
                iniciarModoSecreto(GameDataManager.SecretLevels.ENIGMA); return
            }
        }
    }


    private fun handleWrongAnswer() {
        try {
            wrongSound.seekTo(0)
            wrongSound.start()
        } catch (_: Exception) {}

        secretHitsInRow = 0
        vibrateWrong()
        scoreManager.resetStreak()
        wrongAnswersCount++
        scoreManager.onWrongAnswer()
    }

    private fun advanceToNextQuestionWithDelayOrCuriosity(wasCorrect: Boolean) {
        lifecycleScope.launch {
            delay(if (wasCorrect) 520L else 880L)

            fun goNext() {
                currentQuestionIndex++
                when {
                    currentQuestionIndex < questions.size && wrongAnswersCount < maxWrongAnswers ->
                        displayQuestion(withEnterAnim = false)
                    wrongAnswersCount >= maxWrongAnswers ->
                        showEndOfGameDialog()
                    else ->
                        navigateToResultActivity("RESULT")
                }
            }

            val isSecretNow = isSecretLevel(currentLevel())

            if (isSecretNow && wasCorrect) {
                secretCorrectCounter++

                if (secretCorrectCounter % 2 == 0) {
                    showCuriosityOverlay(
                        text = nextCuriosity(), // ✅ SEM REPETIR
                        durationMs = 3000L
                    ) { goNext() }
                    return@launch
                }
            }

            goNext()
        }
    }

    // =============================================================================================
    // TIMER
    // =============================================================================================

    private fun startTimer() {
        countDownTimer?.cancel()

        val level = currentLevel()
        val baseTime = when (level) {
            GameDataManager.Levels.INTERMEDIARIO -> 20_000L
            GameDataManager.Levels.AVANCADO -> 15_000L
            else -> 30_000L
        }

        totalTimeInMillis = baseTime
        val duration = if (isTimerPaused && remainingTimeInMillis > 0L) remainingTimeInMillis else baseTime
        isTimerPaused = false
        remainingTimeInMillis = duration

        binding.timerTextView.text = formatTime(duration)
        binding.timerProgressBar.max = 100
        binding.timerProgressBar.progress =
            ((duration.toDouble() / baseTime.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)

        currentTimerColorRes = R.drawable.progress_green
        binding.timerProgressBar.progressDrawable =
            ContextCompat.getDrawable(this, currentTimerColorRes)

        countDownTimer = object : CountDownTimer(duration, timerIntervalMillis) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeInMillis = millisUntilFinished

                val progressPercent =
                    ((millisUntilFinished.toDouble() / baseTime.toDouble()) * 100.0)
                        .roundToInt().coerceIn(0, 100)

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

                val secLeft = (millisUntilFinished / 1000L).toInt()
                if (secLeft in 1..3 && secLeft != lastCriticalSecond) {
                    lastCriticalSecond = secLeft
                    tinyTickUi()
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

    private fun pauseTimer() {
        countDownTimer?.cancel()
        isTimerPaused = true
    }

    private fun resumeTimer() {
        if (isTimerPaused && remainingTimeInMillis > 0 && currentQuestionIndex < questions.size) startTimer()
    }

    private fun handleTimeUp() {
        pauseTimer()
        answerLocked = true
        setOptionsEnabled(false)

        secretHitsInRow = 0

        scoreManager.resetStreak()
        scoreManager.onWrongAnswer()
        wrongAnswersCount++

        vibrateWrong()
        glowQuestionCard(false)
        flashFx(false)

        questionsAnswered++
        totalTimeAccumulated += totalTimeInMillis

        updateQuestionsRemaining()

        if (wrongAnswersCount >= maxWrongAnswers) showEndOfGameDialog()
        else advanceToNextQuestionWithDelayOrCuriosity(false)
    }

    private fun formatTime(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format("%02d:%02d", m, s)
    }

    // =============================================================================================
    // UI / FX
    // =============================================================================================

    private fun ensureFxOverlay() {
        if (fxOverlay != null) return
        fxOverlay = View(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            alpha = 0f
            visibility = View.GONE
            isClickable = false
            isFocusable = false
        }
        rootLayout.addView(fxOverlay)
    }

    private fun flashFx(success: Boolean) {
        val v = fxOverlay ?: return
        v.setBackgroundColor(if (success) 0x224CAF50 else 0x22F44336)
        v.visibility = View.VISIBLE
        v.animate().cancel()
        v.alpha = 0f
        v.animate()
            .alpha(1f)
            .setDuration(70)
            .withEndAction {
                v.animate()
                    .alpha(0f)
                    .setDuration(160)
                    .withEndAction { v.visibility = View.GONE }
                    .start()
            }
            .start()
    }

    private fun animateQuestionIn() {
        binding.questionCard.alpha = 0f
        binding.questionCard.translationY = 10f
        binding.questionCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220)
            .setInterpolator(OvershootInterpolator(0.7f))
            .start()
    }

    private fun animateQuestionSwap(newText: String) {
        binding.questionCard.animate().cancel()
        binding.questionCard.animate()
            .alpha(0f)
            .translationY(-8f)
            .setDuration(130)
            .withEndAction {
                binding.questionTextView.text = newText
                binding.questionCard.translationY = 10f
                binding.questionCard.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(200)
                    .setInterpolator(OvershootInterpolator(0.6f))
                    .start()
            }
            .start()
    }

    private fun staggerOptions() {
        optionButtons.forEachIndexed { i, b ->
            if (b.visibility != View.VISIBLE) return@forEachIndexed
            b.animate().cancel()
            b.alpha = 0f
            b.translationY = 10f
            b.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((i * 55L))
                .setDuration(170)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    private fun pop(v: View) {
        v.animate().cancel()
        v.scaleX = 1f; v.scaleY = 1f
        v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(70).withEndAction {
            v.animate().scaleX(1f).scaleY(1f).setDuration(110).start()
        }.start()
    }

    private fun shake(v: View) {
        v.animate().cancel()
        val d = 8f
        v.translationX = 0f
        v.animate().translationX(d).setDuration(35).withEndAction {
            v.animate().translationX(-d).setDuration(35).withEndAction {
                v.animate().translationX(d * 0.6f).setDuration(35).withEndAction {
                    v.animate().translationX(0f).setDuration(55).start()
                }.start()
            }.start()
        }.start()
    }

    private fun tinyTickUi() {
        binding.timerProgressBar.animate().cancel()
        binding.timerProgressBar.scaleX = 1f
        binding.timerProgressBar.animate()
            .scaleX(1.02f)
            .setDuration(80)
            .withEndAction {
                binding.timerProgressBar.animate().scaleX(1f).setDuration(110).start()
            }.start()

        binding.timerTextView.animate().cancel()
        binding.timerTextView.alpha = 1f
        binding.timerTextView.animate()
            .alpha(0.6f)
            .setDuration(80)
            .withEndAction { binding.timerTextView.animate().alpha(1f).setDuration(120).start() }
            .start()

        vibrate(35)
    }

    private fun cacheQuestionCardDefaults() {
        val card = binding.questionCard
        if (defaultQuestionStrokeColor == null) defaultQuestionStrokeColor = card.strokeColor
        if (defaultQuestionStrokeWidth == null) defaultQuestionStrokeWidth = card.strokeWidth
    }

    private fun glowQuestionCard(success: Boolean) {
        val card: MaterialCardView = binding.questionCard
        val ok = ContextCompat.getColor(this, R.color.correctAnswerColor)
        val no = ContextCompat.getColor(this, R.color.wrongAnswerColor)
        val target = if (success) ok else no

        val baseColor = defaultQuestionStrokeColor ?: card.strokeColor
        val baseWidth = defaultQuestionStrokeWidth ?: card.strokeWidth

        card.strokeWidth = dp(2)

        val anim = ValueAnimator.ofInt(40, 200, 40).apply {
            duration = 320
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val a = it.animatedValue as Int
                card.strokeColor =
                    Color.argb(a, Color.red(target), Color.green(target), Color.blue(target))
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    card.strokeColor = baseColor
                    card.strokeWidth = baseWidth
                }
                override fun onAnimationCancel(animation: Animator) {
                    card.strokeColor = baseColor
                    card.strokeWidth = baseWidth
                }
            })
        }
        anim.start()
    }

    private fun paintButtonsForAnswer(selectedIndex: Int, correctIndex: Int) {
        val white = ContextCompat.getColor(this, android.R.color.white)
        val background = ContextCompat.getColor(this, R.color.background_color)

        val correctTint = ContextCompat.getColorStateList(this, R.color.correctAnswerColor)
        val wrongTint = ContextCompat.getColorStateList(this, R.color.wrongAnswerColor)
        val defaultTint = ContextCompat.getColorStateList(this, R.color.button_default)

        optionButtons.forEachIndexed { i, b ->
            if (b.visibility != View.VISIBLE) return@forEachIndexed

            when {
                i == correctIndex -> {
                    b.backgroundTintList = correctTint
                    b.setTextColor(white)
                    if (i == selectedIndex) pop(b)
                }
                i == selectedIndex -> {
                    b.backgroundTintList = wrongTint
                    b.setTextColor(white)
                }
                else -> {
                    b.backgroundTintList = defaultTint
                    b.setTextColor(background)
                }
            }
        }
    }

    private fun resetButtonStyles() {
        val defaultText = ContextCompat.getColor(this, R.color.background_color)
        val defaultTint = ContextCompat.getColorStateList(this, R.color.button_default)

        optionButtons.forEach { b ->
            if (b.visibility != View.VISIBLE) {
                b.isEnabled = false
                b.alpha = 0f
                return@forEach
            }

            b.isEnabled = true
            b.alpha = 1f
            b.setTextColor(defaultText)
            b.backgroundTintList = defaultTint
        }
    }

    private fun setOptionsEnabled(enabled: Boolean) {
        optionButtons.forEach { btn ->
            btn.isEnabled = enabled && btn.visibility == View.VISIBLE
        }
    }

    private fun updateQuestionsRemaining() {
        val left = (totalQuestions - currentQuestionIndex).coerceAtLeast(0)
        binding.questionsRemainingTextView.text =
            getString(R.string.perguntas_restantes_format, left)
    }

    private fun configurarPontuacao() {
        val oldScore = try {
            binding.scoreTextView.text.replace("[^0-9-]".toRegex(), "").toInt()
        } catch (_: Exception) { 0 }

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

        scoreAnimator?.cancel()

        val diff = abs(newScore - oldScore)
        val duration = (260 + diff * 10).coerceIn(420, 900).toLong()

        scoreAnimator = ValueAnimator.ofInt(oldScore, newScore).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { tv.text = getString(R.string.pontos_format, it.animatedValue as Int) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    tv.animate().scaleX(1.06f).scaleY(1.06f).setDuration(120).start()
                }
                override fun onAnimationEnd(animation: Animator) {
                    tv.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
                }
                override fun onAnimationCancel(animation: Animator) {
                    tv.scaleX = 1f; tv.scaleY = 1f
                }
            })
        }
        scoreAnimator?.start()
    }

    private fun updateCoinsUI() {
        val coins = CoinManager.getCoins(this)
        binding.coinsTextView.text = getString(R.string.moedas_format, coins)
    }

    private fun loadUserHeader() {
        val (username, photoUrl, avatarResId) = GameDataManager.loadUserData(this)
        val displayName =
            if (username.isNullOrBlank()) getString(R.string.default_username) else username
        binding.welcomeUsername.text = displayName

        val canUseAvatar = (avatarResId != null && CoinManager.isAvatarUnlocked(this, avatarResId))
        when {
            canUseAvatar -> binding.logoImageView.setImageResource(avatarResId!!)
            !photoUrl.isNullOrEmpty() ->
                Glide.with(this).load(photoUrl).circleCrop().into(binding.logoImageView)
            else -> binding.logoImageView.setImageResource(R.drawable.avatar1)
        }
    }

    private fun observeScoreManager() {
        scoreManager.overallScoreLive.observe(this) {
            configurarPontuacao()

            // ✅ aqui é o momento perfeito: o total acumulado já foi atualizado no GameDataManager
            checkUnlocksAndNotifyInGame()
        }

        scoreManager.currentStreakLive.observe(this) { streak ->
            configurarPontuacao()
            updateSecretProgressUi(streak)

            // ✅ salva o melhor streak do dia
            com.desafiolgico.utils.LocalRecordsManager.updateBestStreakOfDay(this, streak)

            if ((streak == 5 || streak == 10) && streak != lastCelebratedStreak) {
                lastCelebratedStreak = streak
                microCelebrate()
            }
        }
    }




    private fun microCelebrate() {
        konfettiView.visibility = View.VISIBLE
        val party = Party(
            speed = 6f,
            maxSpeed = 14f,
            damping = 0.92f,
            spread = 80,
            angle = 270,
            timeToLive = 1200L,
            shapes = listOf(Shape.Circle),
            size = listOf(Size(5)),
            position = Position.Relative(0.5, 0.0),
            emitter = Emitter(duration = 600, TimeUnit.MILLISECONDS).perSecond(22)
        )
        konfettiView.start(party)
        konfettiView.postDelayed({ konfettiView.visibility = View.GONE }, 900L)
    }



    /** chama e avisa in-game quando liberar nível (sem sair da fase) */
    private fun checkUnlocksAndNotifyInGame() {
        val total = GameDataManager.getOverallTotalScore(this)

        val unlockedNow = mutableListOf<String>()

        fun unlockIfNeeded(level: String, threshold: Int) {
            if (total >= threshold && !GameDataManager.isLevelUnlocked(this, level)) {
                GameDataManager.unlockLevel(this, level)
                unlockedNow.add(level)
            }
        }

        unlockIfNeeded(GameDataManager.Levels.INTERMEDIARIO, THRESHOLD_INTERMEDIATE)
        unlockIfNeeded(GameDataManager.Levels.AVANCADO, THRESHOLD_ADVANCED)
        unlockIfNeeded(GameDataManager.Levels.EXPERIENTE, THRESHOLD_EXPERT)

        if (unlockedNow.isEmpty()) return

        // AAA: chip + confetti (você já tem showFloatingChip e microCelebrate)
        unlockedNow.forEach { showUnlockedInGame(it) }
    }

    private fun showUnlockedInGame(level: String) {
        val label = when (canonicalLevelKey(level)) {
            GameDataManager.Levels.INTERMEDIARIO -> "INTERMEDIÁRIO"
            GameDataManager.Levels.AVANCADO -> "AVANÇADO"
            GameDataManager.Levels.EXPERIENTE -> "EXPERIENTE"
            else -> "NOVO NÍVEL"
        }

        showFloatingChip("🔓 Nível desbloqueado: $label", R.drawable.ic_check_circle, true)
        microCelebrate()
    }

    /** garante chave estável mesmo com acento/variação */
    private fun canonicalLevelKey(level: String): String {
        val t = level.trim().lowercase()
        return when (t) {
            "iniciante" -> GameDataManager.Levels.INICIANTE
            "intermediario", "intermediário" -> GameDataManager.Levels.INTERMEDIARIO
            "avancado", "avançado" -> GameDataManager.Levels.AVANCADO
            "experiente" -> GameDataManager.Levels.EXPERIENTE
            else -> level
        }
    }


    // =============================================================================================
    // GAME OVER
    // =============================================================================================

    private fun showEndOfGameDialog() {
        countDownTimer?.cancel()

        val overlay = View(this).apply {
            setBackgroundColor(Color.parseColor("#AA000000"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isClickable = true
            isFocusable = true
        }

        try {
            if (overlay.parent == null && rootLayout.indexOfChild(overlay) == -1) rootLayout.addView(overlay)
        } catch (_: Exception) {}

        val dialogView = layoutInflater.inflate(R.layout.dialog_game_over_small, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvGameOverTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvGameOverMessage)
        val btnRestart = dialogView.findViewById<MaterialButton>(R.id.btnRestart)
        val btnSeeResults = dialogView.findViewById<MaterialButton>(R.id.btnSeeResults)

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
        secretHitsInRow = 0
        secretCorrectCounter = 0
        runScoreLevel = 0

        GameDataManager.currentStreak = 0
        scoreManager.reset()

        // ✅ nova ordem aleatória (continua 100% sem repetir)
        shuffleSeed = newSeed()
        questions = buildUniqueShuffledQuestions(currentLevelLoaded, shuffleSeed)
        totalQuestions = questions.size

        configurarPontuacao()
        updateQuestionsRemaining()

        if (questions.isNotEmpty()) displayQuestion(true) else finish()
    }

    private fun avgTimeSeconds(): Double =
        if (questionsAnswered > 0) totalTimeAccumulated.toDouble() / questionsAnswered / 1000.0 else 0.0

    // =============================================================================================
    // MODO SECRETO / RESULT
    // =============================================================================================

    private fun iniciarModoSecreto(secretLevel: String) {
        countDownTimer?.cancel()

        val nivelAtual = currentLevel()
        val currentStreak = scoreManager.currentStreakLive.value ?: 0

        GameDataManager.saveLastQuestionIndex(this, nivelAtual, currentQuestionIndex)
        GameDataManager.setUltimoNivelNormal(this, nivelAtual)
        GameDataManager.isModoSecretoAtivo = true

        val prefs = getSharedPreferences(PREF_TEMP, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("score_backup", scoreManager.getOverallScore())
            putInt("errors_backup", wrongAnswersCount)
            putInt("streak_backup", currentStreak)
            putBoolean("is_backup_available", true)
            putInt("run_score_backup", runScoreLevel)



            // ✅ salva a seed da ordem das perguntas (volta sem bagunçar)
            putLong(KEY_SEED_BACKUP, shuffleSeed)

            apply()
        }

        Toast.makeText(
            this,
            "⚡ Fase secreta: ${secretLevel.uppercase()} desbloqueada!",
            Toast.LENGTH_LONG
        ).show()

        startActivity(Intent(this, SecretTransitionActivity::class.java).apply {
            putExtra("SECRET_LEVEL", secretLevel)
            putExtra("RETURN_TO_ACTIVE_GAME", true)
            putExtra("currentStreak", currentStreak)
        })
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun navigateToResultActivity(screenType: String) {
        countDownTimer?.cancel()
        val totalScoreDaFaseAtual = scoreManager.getOverallScore()
        val isSecretMode = GameDataManager.isModoSecretoAtivo
        val avgMs = (avgTimeSeconds() * 1000.0).toLong()

        val levelKey = if (currentLevelLoaded.isNotBlank()) currentLevelLoaded else currentLevel()

        if (!isSecretLevel(levelKey)) {
            LocalRecordsManager.updateBestScoreForLevel(this, levelKey, runScoreLevel)
            LocalRecordsManager.updateBestAvgTimeForLevel(this, levelKey, avgMs)
        }

        val resultIntent = Intent(this, ResultOrGameOverActivity::class.java).apply {
            putExtra("SCREEN_TYPE", screenType)
            putExtra("SCORE", totalScoreDaFaseAtual)
            putExtra("WRONG_ANSWERS", wrongAnswersCount)
            putExtra("MAX_WRONG_ANSWERS", maxWrongAnswers)
            putExtra("TOTAL_QUESTIONS", totalQuestions)
            putExtra("AVERAGE_TIME", avgTimeSeconds())
        }

        val onAdDismissed = {
            if (isSecretMode) {
                val nivelAnterior =
                    GameDataManager.getUltimoNivelNormal(this) ?: GameDataManager.Levels.INICIANTE

                Toast.makeText(this, "⚡ Fase secreta concluída! Bônus aplicados.", Toast.LENGTH_SHORT).show()

                val prefs = getSharedPreferences(PREF_TEMP, Context.MODE_PRIVATE)
                val oldScore = prefs.getInt("score_backup", 0)
                val scoreFinalTotal = oldScore + totalScoreDaFaseAtual

                CoinManager.rewardForSecretLevelCompletion(this)

                prefs.edit().apply {
                    putInt("score_backup", scoreFinalTotal)
                    apply()
                }

                startActivity(Intent(this, TestActivity::class.java).apply {
                    putExtra("level", nivelAnterior)
                })
                finish()
            } else {
                startActivity(resultIntent)
                finish()
            }
        }

        showRewardedAdIfAvailable(onAdDismissed)
    }

    private fun dispararEfeitoFaseSecreta(level: String) {
        val nomeFase = when (level) {
            GameDataManager.SecretLevels.RELAMPAGO -> "Fase Relâmpago ⚡"
            GameDataManager.SecretLevels.PERFEICAO -> "Fase Perfeição 💎"
            GameDataManager.SecretLevels.ENIGMA -> "Fase Enigma 🧩"
            else -> "Fase Secreta"
        }

        Toast.makeText(this, "Você entrou na $nomeFase!", Toast.LENGTH_SHORT).show()

        konfettiView.visibility = View.VISIBLE
        val party = Party(
            speed = 10f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            angle = 270,
            timeToLive = 3_000L,
            shapes = listOf(Shape.Square, Shape.Circle),
            size = listOf(Size(8)),
            position = Position.Relative(0.5, 0.0),
            emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(50)
        )
        konfettiView.start(party)
        konfettiView.postDelayed({ konfettiView.visibility = View.GONE }, 3500)
    }

    // =============================================================================================
    // ADMOB
    // =============================================================================================

    private fun rewardedUnitId(): String =
        if (BuildConfig.DEBUG) TEST_REWARDED_ID else PROD_REWARDED_ID

    private fun bannerUnitId(): String =
        if (BuildConfig.DEBUG) TEST_BANNER_ID else PROD_BANNER_ID

    private fun setupBanner() {
        adView = binding.adView
        adView.visibility = View.INVISIBLE

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                adView.visibility = View.VISIBLE
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                adView.visibility = View.INVISIBLE
            }
        }

        adView.loadAd(AdRequest.Builder().build())
    }

    private fun ensureLastOptionVisible(extraBottomDp: Int = 16) {
        val scroll = binding.gameElementsScrollView
        val lastVisible = optionButtons.lastOrNull { it.visibility == View.VISIBLE } ?: return

        scroll.post {
            val viewportTop = scroll.scrollY + scroll.paddingTop
            val viewportBottom = scroll.scrollY + scroll.height - scroll.paddingBottom

            val childTop = lastVisible.top
            val childBottom = lastVisible.bottom

            if (childBottom > viewportBottom - dp(6)) {
                val viewportHeight = scroll.height - scroll.paddingTop - scroll.paddingBottom
                val targetY = (childBottom - viewportHeight + dp(extraBottomDp)).coerceAtLeast(0)
                scroll.smoothScrollTo(0, targetY)
            }

            if (childTop < viewportTop) {
                scroll.smoothScrollTo(0, (childTop - dp(8)).coerceAtLeast(0))
            }
        }
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
            Toast.makeText(this, "💰 +$rewardAmount moedas!", Toast.LENGTH_SHORT).show()
        }
    }

    // =============================================================================================
    // HAPTIC / HELPERS
    // =============================================================================================

    private fun vibrateTap() = vibrate(55)
    private fun vibrateWrong() = vibrate(170)

    private fun vibrate(durationMs: Long) {
        val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(durationMs)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()

    // =============================================================================================
    // CHIPS / MOEDAS / OVERLAY CURIOSIDADE
    // =============================================================================================

    private fun showFloatingChip(text: String, iconRes: Int, positive: Boolean) {
        val chip = layoutInflater.inflate(R.layout.view_floating_chip, overlayContainer, false)

        val icon = chip.findViewById<android.widget.ImageView>(R.id.chipIcon)
        val tv = chip.findViewById<android.widget.TextView>(R.id.chipText)

        icon.setImageResource(iconRes)
        tv.text = text

        tv.setTextColor(if (positive) 0xFFE6FFFFFF.toInt() else 0xFFFFE3E3.toInt())
        icon.imageTintList = android.content.res.ColorStateList.valueOf(
            if (positive) 0xFFA5FFB1.toInt() else 0xFFFFA5A5.toInt()
        )

        chip.alpha = 0f
        chip.translationY = -12f
        overlayContainer.addView(chip)

        chip.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(140)
            .withEndAction {
                chip.animate()
                    .alpha(0f)
                    .translationY(-18f)
                    .setStartDelay(650)
                    .setDuration(180)
                    .withEndAction { overlayContainer.removeView(chip) }
                    .start()
            }
            .start()
    }

    private fun animateCoinToCounter(fromView: android.view.View?, amount: Int) {
        val coin = android.widget.ImageView(this).apply {
            setImageResource(R.drawable.ic_coin_small)
            imageTintList = android.content.res.ColorStateList.valueOf(0xFFFFD54F.toInt())
            layoutParams = android.widget.FrameLayout.LayoutParams(dp(22), dp(22))
            alpha = 0f
            scaleX = 0.7f
            scaleY = 0.7f
        }
        overlayContainer.addView(coin)

        fun locInWindow(v: android.view.View): IntArray {
            val a = IntArray(2)
            v.getLocationInWindow(a)
            return a
        }

        val startXY = if (fromView != null) locInWindow(fromView) else locInWindow(binding.questionCard)
        val endXY = locInWindow(binding.coinsTextView)

        val startX = startXY[0] + (fromView?.width ?: binding.questionCard.width) / 2f
        val startY = startXY[1] + (fromView?.height ?: binding.questionCard.height) / 2f
        val endX = endXY[0] + binding.coinsTextView.width * 0.8f
        val endY = endXY[1] + binding.coinsTextView.height / 2f

        val overlayXY = locInWindow(overlayContainer)
        coin.translationX = (startX - overlayXY[0])
        coin.translationY = (startY - overlayXY[1])

        coin.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(120)
            .start()

        coin.animate()
            .translationX(endX - overlayXY[0])
            .translationY(endY - overlayXY[1])
            .setDuration(520)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                overlayContainer.removeView(coin)

                binding.coinsTextView.animate().cancel()
                binding.coinsTextView.scaleX = 1f
                binding.coinsTextView.scaleY = 1f
                binding.coinsTextView.animate()
                    .scaleX(1.08f)
                    .scaleY(1.08f)
                    .setDuration(120)
                    .withEndAction {
                        binding.coinsTextView.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
                    }.start()

                showFloatingChip("💰 +$amount moedas", android.R.drawable.ic_input_add, true)
            }
            .start()
    }

    private fun showCuriosityOverlay(
        text: String,
        durationMs: Long = 3000L,
        onDone: () -> Unit
    ) {
        if (isFinishing || isDestroyed) return

        overlayContainer.post {
            if (isFinishing || isDestroyed) return@post

            curiosityOverlayView?.let {
                try { overlayContainer.removeView(it) } catch (_: Exception) {}
            }
            curiosityOverlayView = null

            overlayContainer.visibility = View.VISIBLE
            rootLayout.bringToFront()
            overlayContainer.bringToFront()
            overlayContainer.requestLayout()
            overlayContainer.invalidate()

            val overlay = layoutInflater.inflate(
                R.layout.view_curiosity_overlay,
                overlayContainer,
                false
            ).apply {
                isClickable = true
                isFocusable = true
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                elevation = 9999f
                translationZ = 9999f
            }

            curiosityOverlayView = overlay
            overlay.findViewById<TextView>(R.id.curiosityText).text = text

            overlay.alpha = 0f
            overlayContainer.addView(overlay)
            overlay.bringToFront()

            val finish = finish@{
                if (overlay.parent == null) return@finish
                overlay.animate().cancel()
                overlay.animate()
                    .alpha(0f)
                    .setDuration(160)
                    .withEndAction {
                        try { overlayContainer.removeView(overlay) } catch (_: Exception) {}
                        if (curiosityOverlayView === overlay) curiosityOverlayView = null
                        onDone()
                    }
                    .start()
            }

            overlay.setOnClickListener { finish() }

            overlay.animate()
                .alpha(1f)
                .setDuration(160)
                .withEndAction {
                    overlay.postDelayed({ finish() }, durationMs)
                }
                .start()
        }
    }

    // =============================================================================================
    // CURIOSIDADES (SEM REPETIR)
    // =============================================================================================

    private fun resetCuriosityBag() {
        curiosityBag.clear()
        curiosityBag.addAll(curiosities.shuffled())

        if (curiosityBag.size > 1 && curiosityBag.first() == lastCuriosity) {
            val first = curiosityBag.removeFirst()
            val second = curiosityBag.removeFirst()
            curiosityBag.addFirst(first)
            curiosityBag.addFirst(second)
        }
    }

    private fun nextCuriosity(): String {
        if (curiosityBag.isEmpty()) resetCuriosityBag()
        return curiosityBag.removeFirst().also { lastCuriosity = it }
    }

    // =============================================================================================
    // UI "Rumo à fase secreta"
    // =============================================================================================

    private fun setupSecretProgressUi() {
        val isSecret = isSecretLevel(currentLevelLoaded)

        if (isSecret || secretTarget <= 0) {
            binding.secretProgressLayout.visibility = View.GONE
            return
        }
        binding.secretProgressLayout.visibility = View.VISIBLE
        binding.secretProgressBar.max = secretTarget
        updateSecretProgressUi(scoreManager.currentStreakLive.value ?: 0)
    }

    private fun updateSecretProgressUi(streak: Int) {
        if (binding.secretProgressLayout.visibility != View.VISIBLE) return

        val s = streak.coerceIn(0, secretTarget)
        binding.secretProgressText.text = "Rumo à fase secreta: $s/$secretTarget"
        binding.secretProgressBar.progress = s

        if (s == secretTarget - 1) {
            binding.secretProgressText.animate().cancel()
            binding.secretProgressText.alpha = 1f
            binding.secretProgressText.animate()
                .alpha(0.6f)
                .setDuration(120)
                .withEndAction { binding.secretProgressText.animate().alpha(1f).setDuration(160).start() }
                .start()
        }
    }
}
