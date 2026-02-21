package com.desafiolgico.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
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
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LanguageHelper
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
    private val optionButtons = mutableListOf<MaterialButton>()

    private val game = GameState()
    private val weeklyState = WeeklyState()

    private var questions: List<Question> = emptyList()
    private var runSeed: Long = 0L
    private var isWeeklyMode: Boolean = false

    private lateinit var scoreManager: ScoreManager
    private lateinit var timer: TimerController
    private lateinit var ads: AdsController
    private lateinit var weekly: WeeklyController
    private lateinit var fx: FxController
    private lateinit var overlay: OverlayController
    private lateinit var scoreUi: ScoreUiController
    private lateinit var scroll: ScrollController
    private lateinit var engine: QuizEngine

    // ⚠️ não deixa lateinit crashar
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

        // ✅ cria cedo
        scoreManager = ScoreManager(this)

        // 1) Views primeiro
        inflateOptionButtons()

        // 2) UI controllers
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
        ).also { it.init(score = game.score, streak = game.streak) }

        scroll = ScrollController(
            activity1 = binding.gameElementsScrollView,
            activity = this,
            binding = binding
        ).also { it.setupScrollBottomInsetFix() }

        // 3) Timer
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

        // 4) Ads
        ads = AdsController(
            activity = this,
            binding = binding,
            dp = ::dp,
            onCoinsChanged = { /* se tiver */ },
            adContainer = binding.adContainer,
            bannerUnitId = getString(R.string.banner_ad_unit_id)
        ).also { it.initAds() }

        // 5) Engine (weekly entra/saí via state)
        engine = createEngine()

        // 6) Weekly Controller
        weekly = WeeklyController(
            activity = this,
            state = weeklyState,
            onQuestionsLoaded = { loaded ->
                questions = loaded
                resetRunState(keepHud = false)

                if (questions.isEmpty()) {
                    showToast("WEEKLY sem perguntas (Firestore retornou vazio).")
                    finish()
                    return@WeeklyController
                }

                binding.root.post { engine.displayQuestion(withEnterAnim = true) }
            },
            onRankingOpen = { /* TODO */ },
            onHudUpdate = { _, _ -> /* TODO */ }
        )

        // 7) Coordinator
        coordinator = TestCoordinator(
            activity = this,
            ui = this,
            engine = engine,
            timer = timer,
            ads = ads,
            weekly = weekly
        )

        // 8) Clicks
        bindOptionClicks()

        // 9) Start
        coordinator?.start(intent)

        // 10) NORMAL: carrega perguntas e mostra a 1ª
        if (!isWeeklyMode) {
            val levelKey = readLevelFromIntent()
            questions = loadNormalQuestions(levelKey, seed = runSeed)

            resetRunState(keepHud = false)

            if (questions.isEmpty()) {
                showToast("NORMAL sem perguntas para o nível: $levelKey")
                finish()
                return
            }

            binding.root.post { engine.displayQuestion(withEnterAnim = true) }
        }
        // WEEKLY: quem chama displayQuestion é o callback onQuestionsLoaded
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(STATE_SEED, runSeed)
        super.onSaveInstanceState(outState)
    }

    // =========================================================
    // LEVEL / QUESTIONS
    // =========================================================

    private fun readLevelFromIntent(): String {
        val raw = intent.getStringExtra(EXTRA_LEVEL)
            ?: intent.getStringExtra("level")
            ?: GameDataManager.Levels.INICIANTE
        return GameDataManager.canonicalLevel(raw)
    }

    private fun loadNormalQuestions(level: String, seed: Long): List<Question> {
        val langCode = Locale.getDefault().language
        val qm = QuestionManager(langCode)
        return qm.getQuestionsByLevel(level, seed) // ✅ level é String
    }

    private fun resetRunState(keepHud: Boolean) {
        game.currentIndex = 0
        game.wrongAnswers = 0
        game.answerLocked = false
        game.timerPaused = false
        game.remainingMs = 0L
        game.lastCriticalSecond = -1
        game.questionsAnswered = 0
        game.totalTimeAccumulatedMs = 0L

        if (!keepHud) {
            game.score = 0
            game.streak = 0
            scoreManager.reset()
            scoreUi.init(game.score, game.streak)
        }

        updateQuestionsRemaining()
    }

    // =========================================================
    // ENGINE
    // =========================================================

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

                // ⚠️ function type: NÃO usar named args dentro do engine
                navigateToResult = { screenType, returnToActiveGame ->
                    navigateToResult(screenType, returnToActiveGame)
                },

                // ✅ AQUI: salvar resultado do weekly (best + lastRun + tempo)
                submitWeeklyResult = { weekId, correct, wrong, timeMs, bgCount, bgTotalMs ->
                    weekly.submitWeeklyResult(
                        weekId = weekId,
                        correct = correct,
                        wrong = wrong,
                        timeMs = timeMs,
                        bgCount = bgCount,
                        bgTotalMs = bgTotalMs,
                        onDone = {
                            // opcional: nada (você já volta pra tela)
                        },
                        onFail = { msg ->
                            showToast(msg)
                        }
                    )
                },

                // ✅ após enviar, volta pra tela do campeonato (ou abre ranking)
                startWeeklyRanking = { _ ->
                    finish() // volta para WeeklyChampionshipActivity
                },

                shouldShowCuriosityNow = { false },
                onCuriosityConsumed = { }
            )
        }


    // =========================================================
    // NAV
    // =========================================================

    private fun navigateToResult(screenType: String, returnToActiveGame: Boolean) {
        val levelKey = readLevelFromIntent()

        val scoreLevel = game.score.coerceAtLeast(0)
        val scoreTotal = GameDataManager.getOverallTotalScore(this).coerceAtLeast(0)

        val mode = intent.getStringExtra(EXTRA_MODE) ?: "NORMAL"
        val isWeekly = mode.equals("WEEKLY", ignoreCase = true)

        val totalQ = questions.size.coerceAtLeast(0)

        // ✅ WEEKLY usa contadores corretos (agora game.wrongAnswers já está certo, mas deixa seguro)
        val wrong = if (isWeekly) weeklyState.wrong.coerceAtLeast(0) else game.wrongAnswers.coerceAtLeast(0)
        val correct = if (isWeekly) weeklyState.correct.coerceAtLeast(0) else (totalQ - wrong).coerceAtLeast(0)

        // ✅ tempo total da prova (ranking)
        val totalTimeMs = if (isWeekly) {
            // se já tiver started/finished no WeeklyState, use. senão usa o acumulado.
            val s = weeklyState.startedAtMs
            val f = weeklyState.finishedAtMs
            if (s > 0L && f > 0L && f >= s) (f - s) else game.totalTimeAccumulatedMs.coerceAtLeast(0L)
        } else {
            game.totalTimeAccumulatedMs.coerceAtLeast(0L)
        }

        val avgTimeSec = if (totalQ > 0) (totalTimeMs.toDouble() / totalQ.toDouble()) / 1000.0 else 0.0

        val maxWrong = game.maxWrong.coerceAtLeast(1)

        startActivity(
            Intent(this, ResultOrGameOverActivity::class.java).apply {
                putExtra(ResultOrGameOverActivity.EXTRA_SCREEN_TYPE, screenType)
                putExtra(ResultOrGameOverActivity.EXTRA_SCORE_TOTAL, scoreTotal)
                putExtra(ResultOrGameOverActivity.EXTRA_SCORE_LEVEL, scoreLevel)
                putExtra(ResultOrGameOverActivity.EXTRA_AVERAGE_TIME, avgTimeSec)
                putExtra(ResultOrGameOverActivity.EXTRA_TOTAL_TIME_MS, totalTimeMs) // ✅ novo
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
    }    // =========================================================
    // UI helpers
    // =========================================================

    private fun updateQuestionsRemaining() {
        val total = questions.size
        val idx = game.currentIndex.coerceAtLeast(0)
        val remaining = (total - idx).coerceAtLeast(0)

        // ✅ compatível com string que aceita 1 OU 2 args:
        binding.questionsRemainingTextView.text = try {
            getString(R.string.perguntas_restantes_format, remaining, total)
        } catch (_: Exception) {
            getString(R.string.perguntas_restantes_format, remaining)
        }
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

    // =========================================================
    // Buttons
    // =========================================================

    private fun inflateOptionButtons() {
        optionButtons.clear()

        // mantém seu comportamento original:
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

    // =========================================================
    // lifecycle
    // =========================================================

    override fun onResume() {
        super.onResume()
        coordinator?.onResume()
    }

    override fun onPause() {
        coordinator?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        coordinator?.onDestroy()
        scoreUi.release()
        super.onDestroy()
    }

    // =========================================================
    // TestUi
    // =========================================================

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
