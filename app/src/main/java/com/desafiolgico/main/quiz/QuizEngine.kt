package com.desafiolgico.main.quiz

import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityTestBinding
import com.desafiolgico.main.ui.FxController
import com.desafiolgico.main.ui.OverlayController
import com.desafiolgico.main.ui.ScrollController
import com.desafiolgico.model.Question
import com.desafiolgico.utils.ScoreManager
import com.desafiolgico.weekly.WeeklyState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class QuizEngine(
    private val activity: AppCompatActivity,
    private val binding: ActivityTestBinding,

    private val fx: FxController,
    private val overlay: OverlayController,
    private val scroll: ScrollController,
    private val timer: TimerController,

    private val optionButtons: List<AppCompatButton>,
    private val questionsProvider: () -> List<Question>,

    private val game: GameState,
    private val weekly: WeeklyState? = null,

    private val scoreManager: ScoreManager,
    private val isSecretNow: () -> Boolean,
    private val levelKeyProvider: () -> String,

    private val onScoreStreakChanged: (oldScore: Int, newScore: Int, oldStreak: Int, newStreak: Int) -> Unit,

    private val markSeenIfNormal: (Question) -> Unit,
    private val resetButtonStyles: () -> Unit,
    private val setOptionsEnabled: (Boolean) -> Unit,
    private val updateQuestionsRemaining: () -> Unit,
    private val paintButtonsForAnswer: (selectedIndex: Int, correctIndex: Int) -> Unit,

    private val onCorrectNormalOrSecret: (Question, Long, Int) -> Unit,
    private val onWrongNormalOrSecret: (Question, Long, Int) -> Unit,

    private val shouldLaunchSecretOfferIfPending: (Boolean) -> Boolean,
    private val maybeOfferReviveIfGameOver: () -> Boolean,
    private val showEndOfGameDialog: () -> Unit,

    private val navigateToResult: (screenType: String, returnToActiveGame: Boolean) -> Unit,

    private val submitWeeklyResult: (weekId: String, correct: Int, wrong: Int, timeMs: Long, bgCount: Int, bgTotalMs: Long) -> Unit,
    private val startWeeklyRanking: (weekId: String) -> Unit,

    private val shouldShowCuriosityNow: (Boolean) -> Boolean,
    private val onCuriosityConsumed: () -> Unit
) {

    fun displayQuestion(withEnterAnim: Boolean = false) {
        val questions = questionsProvider()
        val idx = game.currentIndex

        if (questions.isEmpty()) {
            binding.questionTextView.text = "Sem perguntas carregadas."
            optionButtons.forEach { b ->
                b.text = ""
                b.visibility = View.INVISIBLE
                b.isEnabled = false
                b.alpha = 0f
            }
            timer.pause()
            return
        }

        if (idx >= questions.size) {
            weekly?.let { w ->
                val weekId = w.weekId
                if (weekId.isBlank()) {
                    w.reset()
                    return
                }

                val finished = if (w.finishedAtMs == 0L) System.currentTimeMillis() else w.finishedAtMs
                w.finishedAtMs = finished
                if (w.startedAtMs == 0L) w.startedAtMs = finished

                val timeMs = (w.finishedAtMs - w.startedAtMs).coerceAtLeast(0L)

                submitWeeklyResult(
                    weekId,
                    w.correct,
                    w.wrong,
                    timeMs,
                    w.backgroundCount,
                    w.backgroundTotalMs
                )

                Toast.makeText(
                    activity,
                    "✅ Ranking enviado! Acertos: ${w.correct} • Tempo conta.",
                    Toast.LENGTH_LONG
                ).show()

                w.reset()
                startWeeklyRanking(weekId)
                return
            }

            navigateToResult("RESULT", isSecretNow())
            return
        }

        game.answerLocked = false
        game.timerPaused = false
        game.lastCriticalSecond = -1

        val q = questions[idx]

        if (weekly == null) markSeenIfNormal(q)

        resetButtonStyles()
        setOptionsEnabled(true)

        if (withEnterAnim) {
            binding.questionTextView.text = q.questionText
        } else {
            fx.animateQuestionSwap(q.questionText)
        }

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

        scroll.applyScrollBottomPadding(includeBannerAsComfort = false)
        binding.gameElementsScrollView.post { binding.gameElementsScrollView.scrollTo(0, 0) }

        if (withEnterAnim) fx.animateQuestionIn()
        fx.staggerOptions(optionButtons)

        weekly?.let { w ->
            if (w.startedAtMs == 0L) w.startedAtMs = System.currentTimeMillis()
        }

        timer.start()
        updateQuestionsRemaining()
    }

    fun checkAnswer(selectedIndex: Int) {
        val questions = questionsProvider()
        val idx = game.currentIndex
        if (questions.isEmpty()) return
        if (idx >= questions.size) return
        if (game.answerLocked) return

        val selectedBtn = optionButtons.getOrNull(selectedIndex)
        if (selectedBtn == null || selectedBtn.visibility != View.VISIBLE) return

        game.answerLocked = true
        timer.pause()
        setOptionsEnabled(false)

        val q = questions[idx]
        val isCorrect = selectedIndex == q.correctAnswerIndex

        paintButtonsForAnswer(selectedIndex, q.correctAnswerIndex)

        val spentMs = timer.spentMs().coerceAtLeast(0L)
        val remainingMs = game.remainingMs.coerceAtLeast(0L)
        val totalMs = game.totalMs.coerceAtLeast(0L)

        // =========================
        // WEEKLY
        // =========================

            weekly?.let { w ->
                if (w.startedAtMs == 0L) w.startedAtMs = System.currentTimeMillis()

                game.questionsAnswered += 1
                game.totalTimeAccumulatedMs += spentMs   // ✅ era spent (bug)

                val oldScore = game.score
                val oldStreak = game.streak

                if (isCorrect) {
                    w.correct += 1
                    fx.glowQuestionCard(true)
                    fx.flashFx(true)
                    overlay.showFloatingChip("✔ Certo (${w.correct})", R.drawable.ic_check_circle, true)
                } else {
                    w.wrong += 1
                    fx.glowQuestionCard(false)
                    fx.shake(selectedBtn)
                    fx.flashFx(false)
                    overlay.showFloatingChip("✖ Errado", R.drawable.ic_delete, false)
                }

                // ✅ IMPORTANTÍSSIMO: manter GameState coerente pro Result
                game.score = w.correct
                game.streak = 0
                game.wrongAnswers = w.wrong          // ✅ agora RESULT pega wrong certo

                onScoreStreakChanged(oldScore, game.score, oldStreak, game.streak)

                advanceToNextQuestionWithDelayOrCuriosity(isCorrect)
                return
            }

        // =========================
        // NORMAL / SECRETO
        // =========================
        game.questionsAnswered += 1
        game.totalTimeAccumulatedMs += spentMs

        val oldScore = game.score
        val oldStreak = game.streak

        val secret = isSecretNow()

        if (isCorrect) {
            if (secret) scoreManager.addScoreSecret(remainingMs, totalMs)
            else scoreManager.addScore(remainingMs, totalMs)
        } else {
            game.wrongAnswers += 1
            if (secret) scoreManager.onWrongAnswerSecret()
            else scoreManager.onWrongAnswer()
        }

        val newScore = scoreManager.getOverallScore().coerceAtLeast(0)
        val newStreak = (scoreManager.currentStreakLive.value ?: 0).coerceAtLeast(0)

        game.score = newScore
        game.streak = if (secret) 0 else newStreak

        onScoreStreakChanged(oldScore, game.score, oldStreak, game.streak)

        if (isCorrect) {
            onCorrectNormalOrSecret(q, spentMs, selectedIndex)
            fx.glowQuestionCard(true)
            fx.flashFx(true)
        } else {
            onWrongNormalOrSecret(q, spentMs, selectedIndex)
            fx.glowQuestionCard(false)
            fx.shake(selectedBtn)
            fx.flashFx(false)
        }

        if (shouldLaunchSecretOfferIfPending(isCorrect)) return

        if (game.wrongAnswers >= game.maxWrong) {
            if (maybeOfferReviveIfGameOver()) return
            showEndOfGameDialog()
            return
        }

        advanceToNextQuestionWithDelayOrCuriosity(isCorrect)
    }

    fun onTimeUp() {
        val questions = questionsProvider()
        val idx = game.currentIndex
        if (questions.isEmpty()) return
        if (idx >= questions.size) return
        if (game.answerLocked) return

        game.answerLocked = true
        timer.pause()
        setOptionsEnabled(false)

        val q = questions[idx]
        val spentMs = timer.spentMs().coerceAtLeast(0L)
        val totalMs = game.totalMs.coerceAtLeast(0L)

        // =========================
        // WEEKLY
        // =========================

            weekly?.let { w ->
                if (w.startedAtMs == 0L) w.startedAtMs = System.currentTimeMillis()

                game.questionsAnswered += 1
                game.totalTimeAccumulatedMs += spentMs   // ✅ era spent (bug)

                val oldScore = game.score
                val oldStreak = game.streak

                w.wrong += 1
                fx.glowQuestionCard(false)
                fx.flashFx(false)
                overlay.showFloatingChip("⏱ Tempo esgotado", R.drawable.ic_delete, false)

                game.score = w.correct
                game.streak = 0
                game.wrongAnswers = w.wrong            // ✅ RESULT certo

                onScoreStreakChanged(oldScore, game.score, oldStreak, game.streak)

                advanceToNextQuestionWithDelayOrCuriosity(false)
                return
            }

        // =========================
        // NORMAL / SECRETO
        // =========================
        val oldScore = game.score
        val oldStreak = game.streak

        game.wrongAnswers += 1

        val secret = isSecretNow()
        if (secret) scoreManager.onWrongAnswerSecret() else scoreManager.onWrongAnswer()

        val newScore = scoreManager.getOverallScore().coerceAtLeast(0)
        val newStreak = (scoreManager.currentStreakLive.value ?: 0).coerceAtLeast(0)

        game.score = newScore
        game.streak = if (secret) 0 else newStreak

        onScoreStreakChanged(oldScore, game.score, oldStreak, game.streak)

        onWrongNormalOrSecret(q, spentMs, -1)

        if (shouldLaunchSecretOfferIfPending(false)) return

        if (game.wrongAnswers >= game.maxWrong) {
            if (maybeOfferReviveIfGameOver()) return
            showEndOfGameDialog()
            return
        }

        advanceToNextQuestionWithDelayOrCuriosity(false)
    }

    private fun advanceToNextQuestionWithDelayOrCuriosity(wasCorrect: Boolean) {
        activity.lifecycleScope.launch {
            delay(if (wasCorrect) 520L else 880L)

            fun goNext() {
                game.currentIndex += 1

                val total = questionsProvider().size
                when {
                    total == 0 -> navigateToResult("RESULT", isSecretNow())
                    game.currentIndex < total && game.wrongAnswers < game.maxWrong -> displayQuestion(withEnterAnim = false)
                    game.wrongAnswers >= game.maxWrong -> showEndOfGameDialog()
                    else -> navigateToResult("RESULT", isSecretNow())
                }
            }

            if (shouldShowCuriosityNow(wasCorrect)) {
                onCuriosityConsumed()
                overlay.showCuriosityOverlay(
                    text = overlay.nextCuriosity(),
                    durationMs = 3000L
                ) { goNext() }
                return@launch
            }

            goNext()
        }
    }
}
