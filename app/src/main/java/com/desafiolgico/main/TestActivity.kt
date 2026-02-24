package com.desafiolgico.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityTestBinding
import com.desafiolgico.main.ads.AdsController
import com.desafiolgico.main.flow.TestCoordinator
import com.desafiolgico.main.quiz.GameState
import com.desafiolgico.main.quiz.QuizEngine
import com.desafiolgico.main.quiz.TimerController
import com.desafiolgico.main.ui.FxController
import com.desafiolgico.main.ui.OverlayController
import com.desafiolgico.main.ui.ScoreUiController
import com.desafiolgico.main.ui.ScrollController
import com.desafiolgico.main.ui.TestUi
import com.desafiolgico.model.Question
import com.desafiolgico.model.QuestionManager
import com.desafiolgico.utils.CoinManager
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.LocalRecordsManager
import com.desafiolgico.utils.ScoreManager
import com.desafiolgico.utils.applyEdgeToEdge
import com.desafiolgico.utils.applySystemBarsPadding
import com.desafiolgico.weekly.WeeklyController
import com.desafiolgico.weekly.WeeklyState
import com.google.android.material.button.MaterialButton
import java.util.Locale
import kotlin.math.roundToInt

class TestActivity : AppCompatActivity(), TestUi {

    companion object {
        const val EXTRA_MODE = "MODE" // "NORMAL" | "WEEKLY"
        const val EXTRA_LEVEL = "LEVEL" // compat com "level"
        const val EXTRA_RETURN_FROM_SECRET = "RETURN_FROM_SECRET"
        private const val STATE_SEED = "state_seed"
    }

    private lateinit var binding: ActivityTestBinding
    private lateinit var sfx: com.desafiolgico.main.ui.SfxPlayer
    private val optionButtons = mutableListOf<MaterialButton>()

    private val game = GameState()
    private val weeklyState = WeeklyState()

    private var questions: List<Question> = emptyList()
    private var runSeed: Long = 0L
    private var isWeeklyMode: Boolean = false

    // ===== WEEKLY anti-consulta (FONTE ÚNICA AQUI) =====
    private var bgStartedAtMs: Long? = null
    private var bgCountLocal: Int = 0
    private var bgTotalLocalMs: Long = 0L

    private lateinit var scoreManager: ScoreManager
    private lateinit var timer: TimerController
    private lateinit var ads: AdsController
    private lateinit var weekly: WeeklyController
    private lateinit var fx: FxController
    private lateinit var overlay: OverlayController
    private lateinit var scoreUi: ScoreUiController
    private lateinit var scroll: ScrollController
    private lateinit var engine: QuizEngine

    private var coordinator: TestCoordinator? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).roundToInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge(lightSystemBarIcons = false)

        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rootLayoutTest.applySystemBarsPadding(applyTop = true, applyBottom = false)
        binding.adContainer.applySystemBarsPadding(applyTop = false, applyBottom = true)

        GameDataManager.init(this)

        runSeed = savedInstanceState?.getLong(STATE_SEED) ?: System.currentTimeMillis()

        val mode = (intent.getStringExtra(EXTRA_MODE) ?: "NORMAL").uppercase(Locale.getDefault())
        isWeeklyMode = (mode == "WEEKLY")

        sfx = com.desafiolgico.main.ui.SfxPlayer(this)
        scoreManager = ScoreManager(this)

        bindHeaderUserAndPet()
        inflateOptionButtons()

        fx = FxController(
            activity = this,
            binding = binding,
            rootLayout = binding.rootLayoutTest,
            dp = ::dp
        ).also {
            it.ensureFxOverlay()
            it.cacheQuestionCardDefaults()
        }

        overlay = OverlayController(
            activity = this,
            rootLayout = binding.rootLayoutTest,
            overlayContainer = binding.overlayContainer,
            binding = binding
        )

        scoreUi = ScoreUiController(
            activity = this,
            binding = binding
        ).also { it.init(score = 0, streak = 0) }

        scroll = ScrollController(
            activity1 = binding.gameElementsScrollView,
            activity = this,
            binding = binding
        ).also { it.setupScrollBottomInsetFix() }

        timer = TimerController(
            activity = this,
            binding = binding,
            getLevel = { readLevelFromIntent() },
            isTimerPaused = { game.timerPaused },
            getRemainingMs = { game.remainingMs },
            getLastCriticalSecond = { game.lastCriticalSecond },
            setTimerPaused = { game.timerPaused = it },
            setRemainingMs = { game.remainingMs = it },
            setTotalMs = { game.totalMs = it },
            setLastCriticalSecond = { game.lastCriticalSecond = it },
            onTinyTickUi = {
                fx.tinyTickUi {
                    window.decorView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
            },
            onTimeUp = { engine.onTimeUp() }
        )

        ads = AdsController(
            activity = this,
            binding = binding,
            dp = ::dp,
            onCoinsChanged = { /* se tiver */ },
            adContainer = binding.adContainer,
            bannerUnitId = getString(R.string.banner_ad_unit_id)
        ).also { it.initAds() }

        engine = createEngine()

        weekly = WeeklyController(
            activity = this,
            state = weeklyState,
            onQuestionsLoaded = { loaded ->
                questions = loaded

                // ✅ WEEKLY reset separado (não encosta no ScoreManager)
                resetRunStateWeekly()

                if (questions.isEmpty()) {
                    showToast("WEEKLY sem perguntas (Firestore retornou vazio).")
                    finish()
                    return@WeeklyController
                }

                binding.root.post { engine.displayQuestion(withEnterAnim = true) }
            },
            onRankingOpen = { /* não usado aqui */ },
            onHudUpdate = { _, _ -> /* se você tiver HUD weekly, atualiza aqui */ }
        )

        coordinator = TestCoordinator(
            activity = this,
            ui = this,
            engine = engine,
            timer = timer,
            ads = ads,
            weekly = weekly
        )

        bindOptionClicks()
        coordinator?.start(intent)

        if (!isWeeklyMode) {
            val levelKey = readLevelFromIntent()
            questions = loadNormalQuestions(levelKey, seed = runSeed)

            resetRunStateNormal()

            if (questions.isEmpty()) {
                showToast("NORMAL sem perguntas para o nível: $levelKey")
                finish()
                return
            }

            binding.root.post { engine.displayQuestion(withEnterAnim = true) }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(STATE_SEED, runSeed)
        super.onSaveInstanceState(outState)
    }

    private fun readLevelFromIntent(): String {
        val raw = intent.getStringExtra(EXTRA_LEVEL)
            ?: intent.getStringExtra("level")
            ?: GameDataManager.Levels.INICIANTE
        return GameDataManager.canonicalLevel(raw)
    }

    private fun loadNormalQuestions(level: String, seed: Long): List<Question> {
        val langCode = Locale.getDefault().language
        val qm = QuestionManager(langCode)
        return qm.getQuestionsByLevel(level, seed)
    }

    // =========================
    // Resets separados (NORMAL x WEEKLY)
    // =========================

    private fun resetRunStateCommon() {
        game.currentIndex = 0
        game.answerLocked = false

        game.timerPaused = false
        game.remainingMs = 0L
        game.lastCriticalSecond = -1

        game.questionsAnswered = 0
        game.totalTimeAccumulatedMs = 0L

        updateQuestionsRemaining()
    }

    private fun resetRunStateNormal() {
        resetRunStateCommon()

        game.wrongAnswers = 0
        game.score = 0
        game.streak = 0

        scoreManager.reset()
        scoreUi.init(game.score, game.streak)
    }

    private fun resetRunStateWeekly() {
        resetRunStateCommon()

        // WEEKLY: score/streak do HUD normal não deve “mandar”
        game.wrongAnswers = 0
        game.score = 0
        game.streak = 0
        scoreUi.init(0, 0)

        // anti-consulta local (FONTE ÚNICA AQUI)
        bgStartedAtMs = null
        bgCountLocal = 0
        bgTotalLocalMs = 0L
    }

    private fun createEngine(): QuizEngine {
        return QuizEngine(
            activity = this,
            binding = binding,

            fx = fx,
            overlay = overlay,
            scroll = scroll,
            timer = timer,

            optionButtons = optionButtons,
            questionsProvider = { questions },

            game = game,
            weekly = if (isWeeklyMode) weeklyState else null,

            scoreManager = scoreManager,
            isSecretNow = { GameDataManager.isModoSecretoAtivo },
            levelKeyProvider = { readLevelFromIntent() },

            onScoreStreakChanged = { oldScore, newScore, oldStreak, newStreak ->
                scoreUi.animateScoreChange(oldScore, newScore)
                scoreUi.animateStreakChange(oldStreak, newStreak)
            },

            markSeenIfNormal = { q ->
                if (!isWeeklyMode) {
                    val lvl = readLevelFromIntent()
                    GameDataManager.markSeen(this, lvl, q.questionText.trim())
                }
            },

            resetButtonStyles = { resetOptionButtonsStyle() },
            setOptionsEnabled = { enabled -> setOptionsEnabled(enabled) },
            updateQuestionsRemaining = { updateQuestionsRemaining() },
            paintButtonsForAnswer = { selected, correct -> paintButtonsForAnswer(selected, correct) },

            onCorrectNormalOrSecret = { _, _, _ -> },
            onWrongNormalOrSecret = { _, _, _ -> },

            shouldLaunchSecretOfferIfPending = { false },
            maybeOfferReviveIfGameOver = { false },
            showEndOfGameDialog = {
                navigateToResult(
                    ResultOrGameOverActivity.SCREEN_TYPE_GAME_OVER,
                    GameDataManager.isModoSecretoAtivo
                )
            },

            // ✅ vibra só no erro
            vibrateWrong = { vibrateWrong() },

            navigateToResult = { screenType, returnToActiveGame ->
                navigateToResult(screenType, returnToActiveGame)
            },

            // ✅ WEEKLY submit
            submitWeeklyResult = { weekId, correct, wrong, timeMs, _, _ ->
                val maxTimeMs = intent.getLongExtra("MAX_TIME_MS", 15 * 30_000L)
                val timeToleranceMs = 2_000L
                val maxWrongAllowedInclusive = 2

                val finalBgCount = bgCountLocal
                val finalBgTotal = bgTotalLocalMs

                val disqByTime = timeMs > (maxTimeMs + timeToleranceMs)
                val disqByWrong = wrong > maxWrongAllowedInclusive
                val disqByBackground = (finalBgTotal > 15_000L) || (finalBgCount >= 3)

                val disqualified = disqByTime || disqByWrong || disqByBackground
                val reason = when {
                    disqByWrong -> "WRONG_LIMIT"
                    disqByTime -> "TIME_LIMIT"
                    disqByBackground -> "BACKGROUND"
                    else -> ""
                }

                weekly.submitWeeklyResult(
                    weekId = weekId,
                    correct = correct,
                    wrong = wrong,
                    timeMs = timeMs,
                    bgCount = finalBgCount,
                    bgTotalMs = finalBgTotal,
                    disqualified = disqualified,
                    disqualifyReason = reason,
                    onDone = {
                        startActivity(
                            Intent(this, com.desafiolgico.weekly.WeeklyResultActivity::class.java)
                                .putExtra("WEEK_ID", weekId)
                        )
                        finish()
                    },
                    onFail = { msg -> showToast(msg) }
                )
            },

            startWeeklyRanking = { _ ->
                finish()
            },

            shouldShowCuriosityNow = { false },
            onCuriosityConsumed = { },

            // ✅ SFX
            playSfxCorrect = { sfx.playCorrect() },
            playSfxWrong = { sfx.playWrong() }
        )
    }

    private fun navigateToResult(screenType: String, returnToActiveGame: Boolean) {
        val levelKey = readLevelFromIntent()

        val scoreLevel = game.score.coerceAtLeast(0)
        val scoreTotal = GameDataManager.getOverallTotalScore(this).coerceAtLeast(0)

        val mode = intent.getStringExtra(EXTRA_MODE) ?: "NORMAL"
        val isWeekly = mode.equals("WEEKLY", ignoreCase = true)

        val totalQ = questions.size.coerceAtLeast(0)

        val wrong = if (isWeekly) {
            weeklyState.wrong.coerceAtLeast(0)
        } else {
            game.wrongAnswers.coerceAtLeast(0)
        }

        // ✅ answered real (se você não atualiza ainda, cai no totalQ)
        val answered = if (isWeekly) {
            (weeklyState.correct + weeklyState.wrong).coerceAtLeast(0).takeIf { it > 0 } ?: totalQ.coerceAtLeast(1)
        } else {
            game.questionsAnswered.takeIf { it > 0 } ?: totalQ.coerceAtLeast(1)
        }

        val correct = if (isWeekly) {
            weeklyState.correct.coerceAtLeast(0)
        } else {
            (answered - wrong).coerceAtLeast(0)
        }

        val totalTimeMs = if (isWeekly) {
            val s = weeklyState.startedAtMs
            val f = weeklyState.finishedAtMs
            if (s > 0L && f > 0L && f >= s) (f - s) else game.totalTimeAccumulatedMs.coerceAtLeast(0L)
        } else {
            game.totalTimeAccumulatedMs.coerceAtLeast(0L)
        }

        val avgTimeSec = if (answered > 0) (totalTimeMs.toDouble() / answered.toDouble()) / 1000.0 else 0.0
        val maxWrong = game.maxWrong.coerceAtLeast(1)

        // ✅ salva records locais SOMENTE no NORMAL
        if (!isWeekly) {
            val timeMsForAvg = totalTimeMs.takeIf { it > 0L } ?: 1L
            val avgMs = (timeMsForAvg / answered.toLong()).coerceAtLeast(1L)

            LocalRecordsManager.updateBestScoreForLevel(this, levelKey, scoreLevel)
            LocalRecordsManager.updateBestAvgTimeForLevel(this, levelKey, avgMs)
            LocalRecordsManager.updateBestStreakOfDay(this, game.streak.coerceAtLeast(0))
        }

        startActivity(
            Intent(this, ResultOrGameOverActivity::class.java).apply {
                putExtra(ResultOrGameOverActivity.EXTRA_SCREEN_TYPE, screenType)
                putExtra(ResultOrGameOverActivity.EXTRA_SCORE_TOTAL, scoreTotal)
                putExtra(ResultOrGameOverActivity.EXTRA_SCORE_LEVEL, scoreLevel)
                putExtra(ResultOrGameOverActivity.EXTRA_AVERAGE_TIME, avgTimeSec)
                putExtra(ResultOrGameOverActivity.EXTRA_TOTAL_TIME_MS, totalTimeMs)
                putExtra(ResultOrGameOverActivity.EXTRA_LEVEL_KEY, levelKey)
                putExtra(ResultOrGameOverActivity.EXTRA_WRONG_ANSWERS, wrong)
                putExtra(ResultOrGameOverActivity.EXTRA_MAX_WRONG_ANSWERS, maxWrong)
                putExtra(ResultOrGameOverActivity.EXTRA_TOTAL_QUESTIONS, totalQ)
                putExtra(ResultOrGameOverActivity.EXTRA_RETURN_TO_ACTIVE_GAME, returnToActiveGame)

                putExtra(ResultOrGameOverActivity.EXTRA_MODE, mode)
                putExtra(ResultOrGameOverActivity.EXTRA_CORRECT, correct)
                putExtra(ResultOrGameOverActivity.EXTRA_WRONG, wrong)
            }
        )
        finish()
    }
    private fun updateQuestionsRemaining() {
        val total = questions.size
        val idx = game.currentIndex.coerceAtLeast(0)
        val remaining = (total - idx).coerceAtLeast(0)

        binding.questionsRemainingTextView.text =
            if (total > 0) getString(R.string.perguntas_restantes_format_2, remaining, total)
            else getString(R.string.perguntas_restantes_format_1, remaining)
    }

    private fun resetOptionButtonsStyle() {
        optionButtons.forEach { btn ->
            btn.alpha = 1f
            btn.isEnabled = btn.isVisible
            btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_default)
            btn.setTextColor(ContextCompat.getColor(this, R.color.background_color))
        }
    }

    private fun paintButtonsForAnswer(selectedIndex: Int, correctIndex: Int) {
        optionButtons.forEachIndexed { i, btn ->
            if (!btn.isVisible) return@forEachIndexed

            val isCorrect = i == correctIndex
            val isSelected = i == selectedIndex

            when {
                isCorrect -> {
                    btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.progress_green)
                    btn.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                }
                isSelected && !isCorrect -> {
                    btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.portal_red)
                    btn.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                }
                else -> btn.alpha = 0.85f
            }
        }
    }

    private fun inflateOptionButtons() {
        optionButtons.clear()
        binding.gameElements.removeAllViews()
        binding.gameElements.addView(binding.questionCard)

        repeat(4) {
            val item = layoutInflater.inflate(R.layout.item_option_button, binding.gameElements, false)
            val btn = item.findViewById<MaterialButton>(R.id.optionButton)
            binding.gameElements.addView(item)
            optionButtons.add(btn)
        }
    }

    private fun bindOptionClicks() {
        optionButtons.forEachIndexed { index, btn ->
            btn.setOnClickListener { v ->
                if (game.answerLocked) return@setOnClickListener
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                engine.checkAnswer(index)
            }
        }
    }

    private fun bindHeaderUserAndPet() {
        val (username, photoUrl, avatarResId) = GameDataManager.loadUserData(this)

        val displayName = if (!username.isNullOrBlank()) username.trim() else "Jogador"
        binding.welcomeUsername.text = displayName

        when {
            !photoUrl.isNullOrBlank() -> {
                Glide.with(this)
                    .load(Uri.parse(photoUrl))
                    .placeholder(R.drawable.avatar1)
                    .error(R.drawable.avatar1)
                    .circleCrop()
                    .into(binding.logoImageView)
            }

            avatarResId != null && CoinManager.isAvatarUnlocked(this, avatarResId) -> {
                Glide.with(this)
                    .load(avatarResId)
                    .circleCrop()
                    .into(binding.logoImageView)
            }

            else -> {
                Glide.with(this)
                    .load(R.drawable.avatar1)
                    .circleCrop()
                    .into(binding.logoImageView)
            }
        }

        val petResId = runCatching { GameDataManager.getSelectedPetResId(this) }.getOrDefault(0)
        if (petResId != 0) {
            binding.petView.visibility = View.VISIBLE
            binding.petView.setImageResource(petResId)
        } else {
            binding.petView.visibility = View.GONE
        }
    }

    private fun vibrateMs(ms: Long) {
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)
        } else null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val v = vm.defaultVibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(effect) else v.vibrate(ms)
        } else {
            @Suppress("DEPRECATION")
            val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(effect) else v.vibrate(ms)
        }
    }

    private fun vibrateCorrect() = vibrateMs(18) // leve
    private fun vibrateWrong() = vibrateMs(70)   // mais forte

    override fun onResume() {
        super.onResume()
        coordinator?.onResume()
        bindHeaderUserAndPet()

        if (isWeeklyMode) {
            bgStartedAtMs?.let { started ->
                val delta = (System.currentTimeMillis() - started).coerceAtLeast(0L)
                bgStartedAtMs = null
                bgCountLocal += 1
                bgTotalLocalMs += delta
            }
        }
    }

    override fun onPause() {
        if (isWeeklyMode) bgStartedAtMs = System.currentTimeMillis()
        coordinator?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        coordinator?.onDestroy()
        scoreUi.release()
        sfx.release()
        super.onDestroy()
    }

    override fun binding(): ActivityTestBinding = binding
    override fun optionButtons(): List<MaterialButton> = optionButtons
    override fun setAnswerLocked(locked: Boolean) { game.answerLocked = locked }
    override fun isAnswerLocked(): Boolean = game.answerLocked

    override fun setOptionsEnabled(enabled: Boolean) {
        optionButtons.forEach { it.isEnabled = enabled && it.isVisible }
    }

    override fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
