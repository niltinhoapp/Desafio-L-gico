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
import android.util.Log
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
import com.desafiolgico.utils.PremiumCatalog
import com.desafiolgico.utils.PremiumItem
import com.desafiolgico.utils.PremiumManager
import com.desafiolgico.utils.PremiumPets
import com.desafiolgico.utils.PremiumUi
import com.desafiolgico.utils.ScoreManager
import com.desafiolgico.utils.SecurePrefs
import com.desafiolgico.utils.VictoryFx
import com.desafiolgico.utils.applyEdgeToEdge
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
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
import kotlin.math.max
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

    private var lastPremiumEvalScore = -1
    private var lastPremiumEvalStreak = -1

    // --- Anti-farm / Anti-repeat (premium) ---
    private val newThisRunKeys =
        HashSet<String>()            // “novas nesta rodada” (candidatas a pontuar)
    private var initialSeenKeys: Set<String> = emptySet()     // snapshot do visto ANTES da rodada

    // --- Baralho de perguntas ---
    private var shuffleSeed: Long = 0L

    // --- Curiosidades (fase secreta) ---
    private var secretCorrectCounter = 0
    private var secretHitsInRow = 0

    private val curiosities = listOf(
        "🌊 Sabia que o coração de um camarão fica na cabeça?",
        "🐘 O elefante é o único animal com quatro joelhos.",
        "🦋 As borboletas sentem o gosto com os pés!",
        "🔥 O Sol representa 99,86% da massa do Sistema Solar.",
        "💡 O cérebro humano gera eletricidade suficiente para acender uma lâmpada pequena.",
        "⚡ O relâmpago é mais quente que a superfície do Sol.",
        "🌎 A Terra não é perfeitamente redonda — é ligeiramente achatada nos polos.",
        "💓 Seu coração bate cerca de 100 mil vezes por dia.",
        "👀 Os olhos conseguem distinguir mais de 10 milhões de cores.",
        "🦵 O fêmur humano é mais forte que concreto.",
        "🧬 Cada célula do seu corpo contém cerca de 2 metros de DNA.",
        "🌌 Existem mais estrelas no universo do que grãos de areia na Terra.",
        "🐝 Abelhas reconhecem rostos humanos.",
        "🧠 Seu cérebro pesa cerca de 1,4 kg.",
        "🪶 O pinguim tem joelhos escondidos sob as penas.",
        "🦈 Tubarões existem antes dos dinossauros.",
        "🌧️ A chuva tem cheiro — chamado de petrichor.",
        "🌙 A Lua se afasta da Terra cerca de 3,8 cm por ano.",
        "🚀 Um foguete pode ultrapassar 28.000 km/h ao deixar a atmosfera.",
        "🐢 As tartarugas podem respirar pela cloaca (parte traseira do corpo)."
    )

    private val curiosityBag = ArrayDeque<String>()
    private var lastCuriosity: String? = null
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
    private var bannerLoaded = false


    // --- FX AAA leve ---
    private var scoreAnimator: ValueAnimator? = null
    private var fxOverlay: View? = null

    // Glow no card
    private var defaultQuestionStrokeColor: Int? = null
    private var defaultQuestionStrokeWidth: Int? = null

    // micro celebration
    private var lastCelebratedStreak = -1


        companion object {
            // Timer UI (mesmo do seu código)
            private const val GREEN_THRESHOLD_PERCENT = 50
            private const val YELLOW_THRESHOLD_PERCENT = 20

            // Anti-farm / seen+scored (agora salvos via SecurePrefs)
            private const val KEY_SEEN_PREFIX = "seen_"
            private const val KEY_SCORED_PREFIX = "scored_"

            // Reward
            const val REWARD_AD_COINS = 5

            // AdMob IDs
            private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
            private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
            private const val PROD_REWARDED_ID = "ca-app-pub-4958622518589705/3051012274"
            private const val PROD_BANNER_ID = "ca-app-pub-4958622518589705/1734854735"

            // Return flag (Intent)
            private const val EXTRA_RETURN_FROM_SECRET = "RETURN_FROM_SECRET"

            // =========================================================================================
            // ✅ FLUXO NORMAL + SECRETO (SecurePrefs)
            // =========================================================================================

            // Score contínuo (normal + secreto somam no mesmo placar)
            private const val KEY_SESSION_SCORE = "session_score"

            // Estado / flags do fluxo
            private const val KEY_BACKUP_AVAILABLE = "backup_available"
            private const val KEY_IN_SECRET = "in_secret"
            private const val KEY_SECRET_LEVEL = "secret_level"

            // Backup do NORMAL antes de entrar no SECRETO (para voltar exatamente)
            private const val KEY_NORMAL_LEVEL = "normal_level"
            private const val KEY_NORMAL_INDEX = "normal_index"
            private const val KEY_NORMAL_ERRORS = "normal_errors"
            private const val KEY_NORMAL_STREAK = "normal_streak"
            private const val KEY_NORMAL_RUN_SCORE = "normal_run_score"
            private const val KEY_NORMAL_SEED = "normal_seed"
        }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

    // =============================================================================================
    // CICLO DE VIDA
    // =============================================================================================

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        GameDataManager.init(this)
        // Se você já inicializa Ads em outra Activity, pode remover:
       //  MobileAds.initialize(this) {}

        overlayContainer = binding.overlayContainer
        rootLayout = binding.rootLayoutTest
        konfettiView = binding.konfettiView
        overlayContainer.elevation = dp(30).toFloat()
        ensureFxOverlay()

        questionManager = QuestionManager(LanguageHelper.getLanguage(this))
        scoreManager = ScoreManager(this)
        levelManager = LevelManager(this)

        configurarAudio()
        setupBannerAdaptive()
        loadRewardedAd()
        loadUserHeader()
        observeScoreManager()

        binding.logoImageView.setOnClickListener {
            startActivity(Intent(this, AvatarSelectionActivity::class.java))
        }

        // =========================================================================================
        // ✅ CHAVES (SecurePrefs)
        // =========================================================================================
        // Sessão contínua (normal + secreto somam no MESMO score)
        val KEY_SESSION_SCORE = "session_score"

        // Estado normal salvo antes de entrar no secreto (backup/retorno)
        val KEY_BACKUP_AVAILABLE = "backup_available"
        val KEY_IN_SECRET = "in_secret"
        val KEY_SECRET_LEVEL = "secret_level"

        val KEY_NORMAL_LEVEL = "normal_level"
        val KEY_NORMAL_INDEX = "normal_index"
        val KEY_NORMAL_ERRORS = "normal_errors"
        val KEY_NORMAL_STREAK = "normal_streak"
        val KEY_NORMAL_RUN_SCORE = "normal_run_score"
        val KEY_NORMAL_SEED = "normal_seed"

        // -----------------------------------------------------------------------------------------
        // Helpers Long via SecurePrefs.get(context) (porque seu SecurePrefs ainda não tem putLong/getLong)
        fun securePutLong(key: String, value: Long) {
            SecurePrefs.get(this).edit().putLong(key, value).apply()
        }

        fun secureGetLong(key: String, def: Long = 0L): Long {
            return SecurePrefs.get(this).getLong(key, def)
        }

        // =========================================================================================
        // ✅ INTENTS / FLAGS
        // =========================================================================================
        val levelFromIntent = intent.getStringExtra("level") ?: GameDataManager.Levels.INICIANTE
        val returnFromSecret = intent.getBooleanExtra(EXTRA_RETURN_FROM_SECRET, false)
        val launchingSecret = isSecretLevel(levelFromIntent)

        val backupAvailable = SecurePrefs.getBoolean(this, KEY_BACKUP_AVAILABLE, false)

        // =========================================================================================
        // ✅ VARS DE CONTROLE (defaults)
        // =========================================================================================
        var levelToLoad = levelFromIntent
        var startIndex = 0

        wrongAnswersCount = 0
        runScoreLevel = 0
        shuffleSeed = 0L

        // =========================================================================================
        // ✅ (A) SECRETO: sem streak, mas score continua somando
        // =========================================================================================
        if (launchingSecret) {
            // score da sessão continua (normal + secreto)
            val sessionScore = SecurePrefs.getInt(this, KEY_SESSION_SCORE, 0)
            scoreManager.setOverallScore(sessionScore)

            // secreto NÃO TEM streak
            scoreManager.setCurrentStreak(0)
            binding.streakTextView.visibility = View.GONE
            binding.secretProgressLayout.visibility = View.GONE

            // no secreto: erros/score da fase (runScoreLevel) você decide (aqui zerado)
            wrongAnswersCount = 0
            runScoreLevel = 0

            // seed novo no secreto
            shuffleSeed = newSeed()

            // marca estado "em secreto"
            SecurePrefs.putBoolean(this, KEY_IN_SECRET, true)
            SecurePrefs.putString(this, KEY_SECRET_LEVEL, levelToLoad)

            // FX / curiosidades do secreto
            secretCorrectCounter = 0
            secretHitsInRow = 0
            resetCuriosityBag()
            dispararEfeitoFaseSecreta(levelToLoad)
        }

        // =========================================================================================
        // ✅ (B) VOLTOU DO SECRETO: restaura NORMAL sem zerar score e mantendo streak normal
        // =========================================================================================
        else if (returnFromSecret && backupAvailable) {
            val normalLevel =
                SecurePrefs.getString(this, KEY_NORMAL_LEVEL, GameDataManager.Levels.INICIANTE)!!
            val normalIndex = SecurePrefs.getInt(this, KEY_NORMAL_INDEX, 0)
            val normalErrors = SecurePrefs.getInt(this, KEY_NORMAL_ERRORS, 0)
            val normalStreak = SecurePrefs.getInt(this, KEY_NORMAL_STREAK, 0)
            val normalRunScore = SecurePrefs.getInt(this, KEY_NORMAL_RUN_SCORE, 0)
            val normalSeed = secureGetLong(KEY_NORMAL_SEED, 0L)

            val sessionScore = SecurePrefs.getInt(this, KEY_SESSION_SCORE, 0)

            levelToLoad = normalLevel
            startIndex = normalIndex + 1
            wrongAnswersCount = normalErrors
            runScoreLevel = normalRunScore
            shuffleSeed = normalSeed.takeIf { it != 0L } ?: newSeed()

            // ✅ score continua (não zera)
            scoreManager.setOverallScore(sessionScore)

            // ✅ streak volta do normal (mantém!)
            scoreManager.setCurrentStreak(normalStreak)
            binding.streakTextView.visibility = View.VISIBLE

            // saiu do secreto
            SecurePrefs.putBoolean(this, KEY_IN_SECRET, false)
            SecurePrefs.putString(this, KEY_SECRET_LEVEL, "")

            // (opcional) não limpar backup aqui se quiser tolerância a crash
            // se quiser limpar, faça depois que a primeira pergunta carregar com sucesso.
            GameDataManager.isModoSecretoAtivo = false
            GameDataManager.clearUltimoNivelNormal(this)

            Toast.makeText(this, "✅ Voltando ao nível $levelToLoad", Toast.LENGTH_SHORT).show()
        }

        // =========================================================================================
        // ✅ (C) RESTORE NORMAL (caso crash no normal) - opcional, mas recomendado
        // =========================================================================================
        val isRestoringNormalBackup =
            (!launchingSecret && !returnFromSecret && backupAvailable &&
                !SecurePrefs.getString(this, KEY_NORMAL_LEVEL, "").isNullOrBlank())

        if (isRestoringNormalBackup) {
            val normalLevel = SecurePrefs.getString(this, KEY_NORMAL_LEVEL, null)
            if (!normalLevel.isNullOrBlank()) {
                val normalIndex = SecurePrefs.getInt(this, KEY_NORMAL_INDEX, 0)
                val normalErrors = SecurePrefs.getInt(this, KEY_NORMAL_ERRORS, 0)
                val normalStreak = SecurePrefs.getInt(this, KEY_NORMAL_STREAK, 0)
                val normalRunScore = SecurePrefs.getInt(this, KEY_NORMAL_RUN_SCORE, 0)
                val normalSeed = secureGetLong(KEY_NORMAL_SEED, 0L)
                val sessionScore = SecurePrefs.getInt(this, KEY_SESSION_SCORE, 0)

                levelToLoad = normalLevel
                startIndex = normalIndex + 1
                wrongAnswersCount = normalErrors
                runScoreLevel = normalRunScore
                shuffleSeed = normalSeed.takeIf { it != 0L } ?: newSeed()

                scoreManager.setOverallScore(sessionScore)
                scoreManager.setCurrentStreak(normalStreak)
                binding.streakTextView.visibility = View.VISIBLE
            }
        }

        // =========================================================================================
        // ✅ SETS / TARGETS / UI
        // =========================================================================================
        currentLevelLoaded = levelToLoad

        // UI: secreto não mostra streak/progresso, normal mostra
        if (isSecretLevel(currentLevelLoaded)) {
            binding.streakTextView.visibility = View.GONE
            binding.secretProgressLayout.visibility = View.GONE
        } else {
            binding.streakTextView.visibility = View.VISIBLE
        }

        // alvo pra fase secreta (somente níveis normais)
        secretTarget = if (!isSecretLevel(currentLevelLoaded)) {
            when (currentLevelLoaded) {
                GameDataManager.Levels.INICIANTE -> 10
                GameDataManager.Levels.INTERMEDIARIO -> 10
                GameDataManager.Levels.AVANCADO -> 8
                else -> 0
            }
        } else 0

        configurarNivel(levelToLoad)
        configurarTituloNivel(levelToLoad)

        // seed por partida (se ainda não existe)
        if (shuffleSeed == 0L) shuffleSeed = newSeed()

        // snapshot do visto (premium)
        initialSeenKeys = getSeenSet(levelToLoad).toSet()

        // =========================================================================================
        // ✅ LISTA DE PERGUNTAS
        // =========================================================================================
        questions = when {
            launchingSecret -> {
                // segredo: só perguntas novas (fresh) desse nível
                buildFreshRunQuestions(levelToLoad, shuffleSeed)
            }

            // retorno do secreto ou restore => mantém ordem estável para startIndex funcionar
            (returnFromSecret || isRestoringNormalBackup) -> {
                buildUniqueShuffledQuestions(levelToLoad, shuffleSeed)
            }

            else -> {
                // normal: premium run (novas primeiro, completa com revisão)
                buildPremiumRunQuestions(levelToLoad, shuffleSeed)
            }


        }
        if (!launchingSecret) {
            rebuildNewThisRunKeysFromQuestions()
        }

        totalQuestions = questions.size

        currentQuestionIndex =
            if (startIndex >= totalQuestions && totalQuestions > 0) totalQuestions else startIndex

        // progress UI só no normal
        setupSecretProgressUi()
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

        PremiumPets.applyPet(this, binding.petView)
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

        try { correctSound.release() } catch (e: Exception) {
            Log.w("TestActivity", "Falha ao liberar correctSound", e)
        }
        try { wrongSound.release() } catch (e: Exception) {
            Log.w("TestActivity", "Falha ao liberar wrongSound", e)
        }
        try { introSound?.stop(); introSound?.release() } catch (e: Exception) {
            Log.w("TestActivity", "Falha ao liberar introSound", e)
        }
        introSound = null

        countDownTimer?.cancel()
        scoreAnimator?.cancel()
        VictoryFx.release()
        super.onDestroy()
    }

    // =============================================================================================
    // PERGUNTAS: dedup + shuffle + premium run (30)
    // =============================================================================================

    private fun newSeed(): Long = System.currentTimeMillis() xor (System.nanoTime() shl 1)
    private fun seedToInt(seed: Long): Int = (seed xor (seed ushr 32)).toInt()

    private fun normalizeKey(input: String): String {
        val trimmed = input.trim().lowercase()
        if (trimmed.isBlank()) return ""
        val noAccents = Normalizer.normalize(trimmed, Normalizer.Form.NFD).replace("\\p{Mn}+".toRegex(), "")
        return noAccents.replace("[^a-z0-9]+".toRegex(), "")
    }

    private fun questionKey(q: Question): String {
        val base = normalizeKey(q.questionText)
        return if (base.isNotBlank()) base else "fallback_${q.hashCode()}"
    }

    private fun buildUniqueShuffledQuestions(level: String, seed: Long): List<Question> {
        val raw = questionManager.getQuestionsByLevel(level)

        val map = LinkedHashMap<String, Question>()
        for (q in raw) {
            val key = questionKey(q)
            if (!map.containsKey(key)) map[key] = q
        }

        return map.values.toList().shuffled(Random(seedToInt(seed)))
    }


    /**
     * FRESH RUN (sem repetição):
     * - somente perguntas não vistas (pool fresco)
     * - newThisRunKeys marca tudo que é novo nesta rodada
     */
    private fun buildFreshRunQuestions(level: String, seed: Long): List<Question> {
        val allUnique = buildUniqueShuffledQuestions(level, seed)
        newThisRunKeys.clear()

        val fresh = ArrayList<Question>()
        for (q in allUnique) {
            val k = questionKey(q)
            if (k !in initialSeenKeys) {
                fresh.add(q)
                newThisRunKeys.add(k)
            }
        }

        return fresh
    }


    /**
     * PREMIUM RUN (30):
     * - pega “novas” (não vistas no snapshot inicial) primeiro
     * - completa com revisão (vistas) se faltar
     * - newThisRunKeys marca exatamente o que é “novo desta rodada”
     */
    private fun buildPremiumRunQuestions(level: String, seed: Long): List<Question> {
        val allUnique = buildUniqueShuffledQuestions(level, seed)

        newThisRunKeys.clear()

        val newOnes = ArrayList<Question>()
        val review = ArrayList<Question>()

        for (q in allUnique) {
            val k = questionKey(q)
            if (k in initialSeenKeys) review.add(q) else newOnes.add(q)
        }

        val selected = ArrayList<Question>(49)

        // novas primeiro
        for (q in newOnes) {
            if (selected.size >= 49) break
            selected.add(q)
            newThisRunKeys.add(questionKey(q))
        }

        // completa com revisão
        if (selected.size < 49) {
            for (q in review) {
                if (selected.size >= 49) break
                selected.add(q)
            }
        }

        return selected
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

        // ✅ marca como vista assim que aparece (anti-exploit fechar/voltar)
        markSeen(currentLevel(), questionKey(q))

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

    /**
     * PREMIUM RULES:
     * - Se for fase secreta: sempre pontua.
     * - Se for normal:
     *   - só pontua se for “nova desta rodada” E for a primeira vez na vida (markScoredIfFirstTime).
     *   - revisão: +streak (feedback premium), +0 pts.
     */

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
            val isSecretNow = isSecretLevel(levelNow)
            val qKey = questionKey(q)

            // visual da resposta
            paintButtonsForAnswer(selectedIndex, q.correctAnswerIndex)

            // métricas
            val spent = (totalTimeInMillis - remainingTimeInMillis).coerceAtLeast(0L)
            questionsAnswered++
            totalTimeAccumulated += spent

            if (isCorrect) {
                val totalDaQuestao = when (levelNow) {
                    GameDataManager.Levels.INTERMEDIARIO -> 20_000L
                    GameDataManager.Levels.AVANCADO -> 15_000L
                    else -> 30_000L
                }

                if (isSecretNow) {
                    // =====================================================
                    // ✅ SECRETO: NÃO EXISTE STREAK
                    // - pontua sempre
                    // - não chama onCorrectAnswer()
                    // - não dispara iniciarModoSecreto()
                    // =====================================================
                    playCorrectSfx()

                    // Soma pontos sem streak (streakNow=0)
                    scoreManager.addScoreSecret(
                        remainingTimeInMillis = remainingTimeInMillis,
                        totalTimeInMillis = totalDaQuestao
                    )


                    val newScore = scoreManager.getOverallScore()
                    val gained = (newScore - oldScore).coerceAtLeast(0)

                    showFloatingChip("✔ +$gained pts", R.drawable.ic_check_circle, true)

                    // curiosidades / progress interno do secreto (se você quiser manter)
                    secretHitsInRow += 1

                } else {
                    // =====================================================
                    // ✅ NORMAL: sua regra premium (novo + primeira vez = pontua)
                    // revisão: +streak e +0 pts
                    // =====================================================
                    val eligibleForPoints =
                        newThisRunKeys.contains(qKey) && markScoredIfFirstTime(levelNow, qKey)

                    if (eligibleForPoints) {
                        // pontua normal (addScore incrementa streak)
                        handleCorrectAnswerAward(levelNow, totalDaQuestao)
                        if (isFinishing) return

                        val newScore = scoreManager.getOverallScore()
                        val gained = (newScore - oldScore).coerceAtLeast(0)
                        runScoreLevel += gained

                        val streakNow = scoreManager.currentStreakLive.value ?: 0
                        showFloatingChip("✔ +$gained pts • 🔥 $streakNow", R.drawable.ic_check_circle, true)
                    } else {
                        // revisão: +streak, +0 pts
                        scoreManager.onCorrectAnswer()
                        playCorrectSfx()

                        val streakNow = scoreManager.currentStreakLive.value ?: 0
                        showFloatingChip("✔ Revisão • +0 pts • 🔥 $streakNow", R.drawable.ic_check_circle, true)
                    }
                }

                glowQuestionCard(success = true)
                flashFx(success = true)

            } else {
                // =====================================================
                // ❌ ERRO
                // - secreto: não “reseta streak” porque streak não existe
                // - normal: seu fluxo padrão (zera streak etc.)
                // =====================================================
                if (isSecretNow) {
                    try {
                        wrongSound.seekTo(0)
                        wrongSound.start()
                    } catch (_: Exception) {}

                    secretHitsInRow = 0
                    vibrateWrong()

                    // NÃO chama scoreManager.onWrongAnswer() (pois ele zera streak e altera score global)
                    // Se você quiser penalidade no secreto, faça aqui uma dedução própria (opcional).
                    wrongAnswersCount++

                    showFloatingChip("✖ Errou", R.drawable.ic_delete, false)
                } else {
                    handleWrongAnswer()

                    val streakNow = scoreManager.currentStreakLive.value ?: 0
                    showFloatingChip("✖ Errou • 🔥 $streakNow", R.drawable.ic_delete, false)
                }

                glowQuestionCard(success = false)
                shake(selectedBtn)
                flashFx(success = false)
            }

            // ✅ Atualiza UI de “rumo ao secreto” só no normal
            if (!isSecretNow) {
                updateSecretProgressUi(scoreManager.currentStreakLive.value ?: 0)
            }

            advanceToNextQuestionWithDelayOrCuriosity(isCorrect)
        }



    private fun playCorrectSfx() {
        try {
            correctSound.seekTo(0)
            correctSound.start()
        } catch (_: Exception) {}
    }

        @SuppressLint("SetTextI18n")
        private fun handleCorrectAnswerAward(nivelAtual: String, totalDaQuestao: Long) {
            if (faseFinalizada) return

            // ✅ Segurança: dentro do secreto NÃO tem streak e NÃO entra em secreto de novo
            if (isSecretLevel(nivelAtual)) {
                // Se por algum motivo você chamar isso no secreto, ignora o gatilho.
                // (pontuação do secreto você faz no checkAnswer com addScoreNoStreak)
                playCorrectSfx()
                return
            }

            playCorrectSfx()

            // ✅ soma pontos (addScore incrementa streak + globais/marcos)
            scoreManager.addScore(remainingTimeInMillis, totalDaQuestao)

            // ✅ conta 1 acerto POR NÍVEL (somente níveis normais)
            GameDataManager.addCorrectForLevel(this, nivelAtual, 1)

            // ✅ modo secreto (apenas se NÃO existe jogo pausado / não está retornando etc.)
            val streakCalculo = scoreManager.currentStreakLive.value ?: 0
            val nenhumPausado = GameDataManager.getUltimoNivelNormal(this) == null

            if (!nenhumPausado) return

            when (nivelAtual) {
                GameDataManager.Levels.INICIANTE -> {
                    if (streakCalculo == 10) {
                        iniciarModoSecreto(GameDataManager.SecretLevels.RELAMPAGO)
                        return
                    }
                }

                GameDataManager.Levels.INTERMEDIARIO -> {
                    if (streakCalculo == 10) {
                        iniciarModoSecreto(GameDataManager.SecretLevels.PERFEICAO)
                        return
                    }
                }

                GameDataManager.Levels.AVANCADO -> {
                    if (streakCalculo == 8) {
                        iniciarModoSecreto(GameDataManager.SecretLevels.ENIGMA)
                        return
                    }
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

        // seu comportamento atual (zera streak + aplica onWrong)
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
                        text = nextCuriosity(),
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
        binding.timerProgressBar.progressDrawable = ContextCompat.getDrawable(this, currentTimerColorRes)

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
// PERSISTÊNCIA ANTI-FARM (seen + scored) - por usuário + por nível (SecurePrefs)
// =============================================================================================


    private fun userScopeKey(): String {
        val uid = GameDataManager.currentUserId
        return if (uid.isNullOrBlank()) "anon" else uid
    }



    private fun levelScopeKey(level: String): String =
        canonicalLevelKey(level).trim().lowercase()

    private fun seenStorageKey(level: String): String =
        "${KEY_SEEN_PREFIX}${userScopeKey()}_${levelScopeKey(level)}"

    private fun scoredStorageKey(level: String): String =
        "${KEY_SCORED_PREFIX}${userScopeKey()}_${levelScopeKey(level)}"

    private fun getSeenSet(level: String): MutableSet<String> {
        val raw = SecurePrefs.getStringSet(this, seenStorageKey(level), emptySet())
        return HashSet(raw)
    }

    private fun getScoredSet(level: String): MutableSet<String> {
        val raw = SecurePrefs.getStringSet(this, scoredStorageKey(level), emptySet())
        return HashSet(raw)
    }

    private fun markSeen(level: String, qKey: String) {
        if (qKey.isBlank()) return
        val set = getSeenSet(level)
        if (set.add(qKey)) {
            SecurePrefs.putStringSet(this, seenStorageKey(level), set)
        }
    }

    /**
     * Marca como "pontuada" apenas 1 vez na vida.
     * true = primeira vez (pode dar pontos)
     * false = já pontuada antes (revisão => +0 pts)
     */
    private fun markScoredIfFirstTime(level: String, qKey: String): Boolean {
        if (qKey.isBlank()) return false
        val set = getScoredSet(level)
        val first = set.add(qKey)
        if (first) {
            SecurePrefs.putStringSet(this, scoredStorageKey(level), set)
        }
        return first
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
                .setStartDelay(i * 55L)
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

        // ✅ Regra fixa: FOTO > AVATAR > fallback
        val hasPhoto = !photoUrl.isNullOrBlank()
        val hasAvatar = (avatarResId != null && avatarResId != 0)
        val avatarUnlocked = hasAvatar && CoinManager.isAvatarUnlocked(this, avatarResId!!)

        when {
            hasPhoto -> {
                Glide.with(this).load(photoUrl).circleCrop().into(binding.logoImageView)
            }
            avatarUnlocked -> {
                binding.logoImageView.setImageResource(avatarResId!!)
            }
            else -> {
                binding.logoImageView.setImageResource(R.drawable.avatar1)
            }
        }

        PremiumUi.applyFrameToAvatar(binding.logoImageView, this)
        PremiumUi.applyThemeToRoot(findViewById(android.R.id.content), this)
        PremiumUi.applyTitleToUsername(binding.welcomeUsername, this, displayName)
    }
    private fun rebuildNewThisRunKeysFromQuestions() {
        newThisRunKeys.clear()
        for (q in questions) {
            val k = questionKey(q)
            if (k.isNotBlank() && k !in initialSeenKeys) {
                newThisRunKeys.add(k)
            }
        }
    }



    private fun observeScoreManager() {
        scoreManager.overallScoreLive.observe(this) { total ->
            // mantém persistido o total atualizado (normal + secreto)
            SecurePrefs.putInt(this, KEY_SESSION_SCORE, total)

            configurarPontuacao()
            checkUnlocksAndNotifyInGame()
            checkPremiumUnlocksInGame()
        }


        scoreManager.currentStreakLive.observe(this) { streak ->
            // ✅ No secreto: não usa streak pra nada (não persiste, não mostra, não conta)
            if (isSecretLevel(currentLevel())) {
                // opcional: garantir UI "neutra" no secreto
                binding.streakTextView.visibility = View.GONE
                // também não atualiza barra "rumo ao secreto"
                // (no seu setupSecretProgressUi já deve esconder, mas aqui garante)
                return@observe
            }

            // ✅ Normal: continua como antes
            configurarPontuacao()
            updateSecretProgressUi(streak)

            LocalRecordsManager.updateBestStreakOfDay(this, streak)
            GameDataManager.updateHighestStreakIfNeeded(this, streak)

            checkPremiumUnlocksInGame()

            if ((streak == 5 || streak == 10) && streak != lastCelebratedStreak) {
                lastCelebratedStreak = streak
                microCelebrate()
            }
        }
    }




    private fun checkPremiumUnlocksInGame() {
        val ctx = applicationContext
        val newlyUnlocked = mutableListOf<PremiumItem>()

        PremiumCatalog.all().forEach { item ->
            val wasUnlocked = PremiumManager.isUnlocked(ctx, item)
            val isUnlockedNow = PremiumManager.unlockByAchievementIfPossible(ctx, item)
            if (!wasUnlocked && isUnlockedNow) newlyUnlocked.add(item)
        }

        if (newlyUnlocked.isEmpty()) return

        val names = newlyUnlocked.take(3).joinToString(", ") { it.name }
        val more = if (newlyUnlocked.size > 3) " +${newlyUnlocked.size - 3}" else ""
        val msg = "🎁 Premium liberado: $names$more"

        showFloatingChip(msg, R.drawable.ic_check_circle, true)
        microCelebrate()
    }

    private fun applySecretUiIfNeeded(level: String) {
        val isSecret = isSecretLevel(level)

        binding.streakTextView.visibility = if (isSecret) View.GONE else View.VISIBLE

        // se você tiver layout de progresso pro secreto:
        if (isSecret) {
            binding.secretProgressLayout.visibility = View.GONE
        }
    }

    private fun checkPremiumUnlocksIfAny() {
        val scoreNow = GameDataManager.getOverallTotalScore(this)
        val streakNow = scoreManager.currentStreakLive.value ?: 0

        if (scoreNow == lastPremiumEvalScore && streakNow == lastPremiumEvalStreak) return
        lastPremiumEvalScore = scoreNow
        lastPremiumEvalStreak = streakNow

        PremiumCatalog.all().forEach { item ->
            val unlockedNow = PremiumManager.unlockByAchievementIfPossible(applicationContext, item)
            if (unlockedNow) {
                showFloatingChip("🎁 Recompensa premium liberada!", R.drawable.ic_check_circle, true)
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

    private fun checkUnlocksAndNotifyInGame() {
        // ✅ Fonte única: LevelManager
        val unlockedNow = levelManager.checkAndSaveLevelUnlocks(showToast = false)
        if (unlockedNow.isEmpty()) return

        val labels = unlockedNow.joinToString(", ") { level ->
            when (level) {
                GameDataManager.Levels.INTERMEDIARIO -> "INTERMEDIÁRIO"
                GameDataManager.Levels.AVANCADO -> "AVANÇADO"
                GameDataManager.Levels.EXPERIENTE -> "EXPERIENTE"
                else -> "NOVO NÍVEL"
            }
        }

        val msg = if (unlockedNow.size == 1) "🔓 Nível desbloqueado: $labels"
        else "🔓 Níveis desbloqueados: $labels"

        showFloatingChip(msg, R.drawable.ic_check_circle, true)
        microCelebrate()
    }

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
    // GAME OVER / RESTART / RESULT / SECRET
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

        shuffleSeed = newSeed()

        // snapshot do visto antes do restart
        initialSeenKeys = getSeenSet(currentLevelLoaded).toSet()

        questions = buildPremiumRunQuestions(currentLevelLoaded, shuffleSeed)
        totalQuestions = questions.size

        configurarPontuacao()
        updateQuestionsRemaining()

        if (questions.isNotEmpty()) displayQuestion(true) else finish()
    }

    private fun avgTimeSeconds(): Double =
        if (questionsAnswered > 0) totalTimeAccumulated.toDouble() / questionsAnswered / 1000.0 else 0.0



    private fun iniciarModoSecreto(secretLevel: String) {
        countDownTimer?.cancel()

        val nivelAtual = currentLevel()
        val streakAtual = scoreManager.currentStreakLive.value ?: 0
        val scoreSessao = scoreManager.getOverallScore()

        // Mantém compat com seu fluxo atual (se você ainda usa isso em algum lugar)
        GameDataManager.saveLastQuestionIndex(this, nivelAtual, currentQuestionIndex)
        GameDataManager.setUltimoNivelNormal(this, nivelAtual)
        GameDataManager.isModoSecretoAtivo = true

        // ✅ SecurePrefs: salva o estado do NORMAL antes de entrar no SECRETO
        SecurePrefs.putInt(this, KEY_SESSION_SCORE, scoreSessao)

        SecurePrefs.putString(this, KEY_NORMAL_LEVEL, nivelAtual)
        SecurePrefs.putInt(this, KEY_NORMAL_INDEX, currentQuestionIndex)
        SecurePrefs.putInt(this, KEY_NORMAL_ERRORS, wrongAnswersCount)
        SecurePrefs.putInt(this, KEY_NORMAL_STREAK, streakAtual)
        SecurePrefs.putInt(this, KEY_NORMAL_RUN_SCORE, runScoreLevel)

        // seed (Long) — usa SharedPreferences interno do SecurePrefs
        SecurePrefs.get(this).edit().putLong(KEY_NORMAL_SEED, shuffleSeed).apply()

        // flags de fluxo
        SecurePrefs.putBoolean(this, KEY_BACKUP_AVAILABLE, true)
        SecurePrefs.putBoolean(this, KEY_IN_SECRET, true)
        SecurePrefs.putString(this, KEY_SECRET_LEVEL, secretLevel)

        Toast.makeText(
            this,
            "⚡ Fase secreta: ${secretLevel.uppercase()} desbloqueada!",
            Toast.LENGTH_LONG
        ).show()

        // ✅ Importante: NÃO manda currentStreak pro secreto, porque no secreto não existe streak.
        startActivity(Intent(this, SecretTransitionActivity::class.java).apply {
            putExtra("SECRET_LEVEL", secretLevel)
            putExtra("RETURN_TO_ACTIVE_GAME", true)
            // se a SecretTransitionActivity ainda espera esse extra, manda 0 só pra não quebrar:
            putExtra("currentStreak", 0)
        })

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }


    private fun navigateToResultActivity(screenType: String) {
        countDownTimer?.cancel()

        val isSecretNow = isSecretLevel(currentLevelLoaded) || GameDataManager.isModoSecretoAtivo
        val avgMs = (avgTimeSeconds() * 1000.0).toLong()

        val levelKey = if (currentLevelLoaded.isNotBlank()) currentLevelLoaded else currentLevel()

        // ✅ Recordes só fazem sentido para NÍVEIS NORMAIS
        if (!isSecretLevel(levelKey)) {
            // runScoreLevel = score DO NÍVEL (não o total global)
            LocalRecordsManager.updateBestScoreForLevel(this, levelKey, runScoreLevel)
            LocalRecordsManager.updateBestAvgTimeForLevel(this, levelKey, avgMs)
        }

        // ✅ Intent de resultado (apenas para NORMAL)
        val resultIntent = Intent(this, ResultOrGameOverActivity::class.java).apply {
            putExtra("SCREEN_TYPE", screenType)

            // Mostra separado (você decide na tela de resultado)
            putExtra("SCORE_TOTAL", scoreManager.getOverallScore()) // global (sessão)
            putExtra("SCORE_LEVEL", runScoreLevel)                  // só da fase

            putExtra("WRONG_ANSWERS", wrongAnswersCount)
            putExtra("MAX_WRONG_ANSWERS", maxWrongAnswers)
            putExtra("TOTAL_QUESTIONS", totalQuestions)
            putExtra("AVERAGE_TIME", avgTimeSeconds())
            putExtra("LEVEL_KEY", levelKey)
        }

        val onAdDismissed = {
            if (isSecretNow) {
                // ✅ SECRETO:
                // - NÃO soma score aqui (já somou durante o jogo)
                // - recompensa moedas
                CoinManager.rewardForSecretLevelCompletion(this)

                Toast.makeText(this, "⚡ Fase secreta concluída!", Toast.LENGTH_SHORT).show()

                // ✅ volta para o nível normal salvo (SecurePrefs)
                val nivelAnterior =
                    SecurePrefs.getString(this, KEY_NORMAL_LEVEL, null)
                        ?: (GameDataManager.getUltimoNivelNormal(this) ?: GameDataManager.Levels.INICIANTE)

                // opcional: marca que saiu do secreto
                SecurePrefs.putBoolean(this, KEY_IN_SECRET, false)
                SecurePrefs.putString(this, KEY_SECRET_LEVEL, "")

                startActivity(Intent(this, TestActivity::class.java).apply {
                    putExtra("level", nivelAnterior)
                    putExtra(EXTRA_RETURN_FROM_SECRET, true)
                })
                finish()
            } else {
                // ✅ NORMAL: mostra tela de resultado
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

    private fun rewardedUnitId(): String = if (BuildConfig.DEBUG) TEST_REWARDED_ID else PROD_REWARDED_ID



    private fun bannerUnitId(): String =
        if (BuildConfig.DEBUG) TEST_BANNER_ID else PROD_BANNER_ID

    // --- Ads ---

    private fun setupBannerAdaptive() {
        adView = binding.adView
        if (bannerLoaded) return
        bannerLoaded = true

        adView.visibility = View.INVISIBLE

        // ✅ setar unitId UMA vez (agora vem só do Kotlin)
        adView.adUnitId = bannerUnitId()

        val container = binding.adContainer
        container.post {
            // ✅ se a Activity já está morrendo, evita crash
            if (isFinishing || isDestroyed) return@post

            val widthPx = kotlin.math.max(1, container.width)
            val density = resources.displayMetrics.density
            val widthDp = (widthPx / density).toInt().coerceAtLeast(320)

            val adaptiveSize =
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, widthDp)

            // ✅ setAdSize UMA vez só
            adView.setAdSize(adaptiveSize)

            adView.adListener = object : AdListener() {
                override fun onAdLoaded() { adView.visibility = View.VISIBLE }
                override fun onAdFailedToLoad(error: LoadAdError) { adView.visibility = View.INVISIBLE }
            }

            adView.loadAd(AdRequest.Builder().build())
        }
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

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
                .withEndAction { overlay.postDelayed({ finish() }, durationMs) }
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
        binding.secretProgressBar.progress = streak

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
