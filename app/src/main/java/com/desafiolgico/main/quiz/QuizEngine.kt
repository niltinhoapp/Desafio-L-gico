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

/**
 * QuizEngine:
 * - Controla fluxo de perguntas/respostas/tempo
 * - Funciona em 2 modos:
 *   NORMAL/SECRETO: pontuação via ScoreManager, limite de erros, records locais, etc.
 *   WEEKLY: contabiliza correct/wrong + tempo total + anti-fraude (via Activity), e envia resultado.
 *
 * ✅ Ajuste importante (bugfix):
 * - Quando o usuário perde por TEMPO no modo NORMAL, antes não incrementava:
 *   game.questionsAnswered e game.totalTimeAccumulatedMs
 *   Isso fazia avgTime ficar 0 e records não salvarem.
 * - Agora o onTimeUp() também contabiliza como pergunta respondida.
 */
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

    private val vibrateWrong: () -> Unit,
    private val navigateToResult: (screenType: String, returnToActiveGame: Boolean) -> Unit,

    private val submitWeeklyResult: (
        weekId: String,
        correct: Int,
        wrong: Int,
        timeMs: Long,
        bgCount: Int,
        bgTotalMs: Long
    ) -> Unit,
    private val startWeeklyRanking: (weekId: String) -> Unit,

    private val shouldShowCuriosityNow: (Boolean) -> Boolean,
    private val onCuriosityConsumed: () -> Unit,

    // ✅ SFX callbacks (injetado pela Activity)
    private val playSfxCorrect: () -> Unit,
    private val playSfxWrong: () -> Unit
) {

    private fun isWeeklyMode(): Boolean = weekly != null

    private fun currentQuestionOrNull(): Question? {
        val questions = questionsProvider()
        val idx = game.currentIndex
        if (questions.isEmpty()) return null
        if (idx !in questions.indices) return null
        return questions[idx]
    }

    /**
     * Finaliza o Weekly:
     * - Calcula timeMs total
     * - Envia pro Firestore via callback submitWeeklyResult()
     * - Mostra toast
     * - Reseta estado weekly e abre ranking
     */
    private fun endWeeklyIfNeeded() {
        val w = weekly ?: return

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
            "✅ Resultado enviado! Acertos: ${w.correct} • Tempo conta.",
            Toast.LENGTH_LONG
        ).show()

        w.reset()
        startWeeklyRanking(weekId)
    }

    /**
     * WEEKLY: espelha correct/wrong para o HUD normal.
     * Importante: isso é apenas visual / ResultActivity, não manda regras do fluxo.
     */
    private fun syncGameHudFromWeekly() {
        val w = weekly ?: return
        val oldScore = game.score
        val oldStreak = game.streak

        // espelha para HUD/ResultActivity
        game.score = w.correct
        game.streak = 0
        game.wrongAnswers = w.wrong

        onScoreStreakChanged(oldScore, game.score, oldStreak, game.streak)
    }

    /**
     * Renderiza pergunta atual na tela.
     * Se acabou as perguntas:
     * - WEEKLY -> endWeeklyIfNeeded()
     * - NORMAL -> navigateToResult()
     */
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

        // terminou o set de perguntas
        if (idx >= questions.size) {
            if (isWeeklyMode()) {
                endWeeklyIfNeeded()
            } else {
                navigateToResult("RESULT", isSecretNow())
            }
            return
        }

        // prepara estado para nova pergunta
        game.answerLocked = false
        game.timerPaused = false
        game.lastCriticalSecond = -1

        val q = questions[idx]
        if (!isWeeklyMode()) markSeenIfNormal(q)

        resetButtonStyles()
        setOptionsEnabled(true)

        if (withEnterAnim) binding.questionTextView.text = q.questionText
        else fx.animateQuestionSwap(q.questionText)

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

        // conforto de scroll e volta pro topo
        scroll.applyScrollBottomPadding(includeBannerAsComfort = false)
        binding.gameElementsScrollView.post { binding.gameElementsScrollView.scrollTo(0, 0) }

        if (withEnterAnim) fx.animateQuestionIn()
        fx.staggerOptions(optionButtons)

        // WEEKLY: marca start se ainda não marcou
        weekly?.let { w ->
            if (w.startedAtMs == 0L) w.startedAtMs = System.currentTimeMillis()
        }

        timer.start()
        updateQuestionsRemaining()
    }

    /**
     * Clique em uma alternativa.
     * - Trava input
     * - Pausa timer
     * - Aplica feedback visual
     * - Atualiza contadores/pontuação
     * - Avança com delay
     */
    fun checkAnswer(selectedIndex: Int) {
        val q = currentQuestionOrNull() ?: return
        if (game.answerLocked) return

        val selectedBtn = optionButtons.getOrNull(selectedIndex)
        if (selectedBtn == null || selectedBtn.visibility != View.VISIBLE) return

        game.answerLocked = true
        timer.pause()
        setOptionsEnabled(false)

        val isCorrect = selectedIndex == q.correctAnswerIndex
        paintButtonsForAnswer(selectedIndex, q.correctAnswerIndex)

        val spentMs = timer.spentMs().coerceAtLeast(0L)
        val remainingMs = game.remainingMs.coerceAtLeast(0L)
        val totalMs = game.totalMs.coerceAtLeast(0L)

        // =========================================================================================
        // WEEKLY
        // =========================================================================================
        if (isWeeklyMode()) {
            val w = weekly!!
            if (w.startedAtMs == 0L) w.startedAtMs = System.currentTimeMillis()

            // ✅ conta pergunta e tempo (WEEKLY usa isso pro totalTimeMs no Result)
            game.questionsAnswered += 1
            game.totalTimeAccumulatedMs += spentMs

            if (isCorrect) {
                playSfxCorrect()
                w.correct += 1
                fx.glowQuestionCard(true)
                fx.flashFx(true)
                overlay.showFloatingChip("✔ Certo (${w.correct})", R.drawable.ic_check_circle, true)
            } else {
                w.wrong += 1
                playSfxWrong()
                vibrateWrong()
                fx.glowQuestionCard(false)
                fx.shake(selectedBtn)
                fx.flashFx(false)
                overlay.showFloatingChip("✖ Errado", R.drawable.ic_delete, false)
            }

            syncGameHudFromWeekly()
            advanceToNextQuestionWithDelayOrCuriosity(wasCorrect = isCorrect)
            return
        }

        // =========================================================================================
        // NORMAL / SECRETO
        // =========================================================================================
        game.questionsAnswered += 1
        game.totalTimeAccumulatedMs += spentMs

        val oldScore = game.score
        val oldStreak = game.streak

        val secret = isSecretNow()

        if (isCorrect) {
            playSfxCorrect()
            if (secret) scoreManager.addScoreSecret(remainingMs, totalMs)
            else scoreManager.addScore(remainingMs, totalMs)
        } else {
            playSfxWrong()
            vibrateWrong()
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

        advanceToNextQuestionWithDelayOrCuriosity(wasCorrect = isCorrect)
    }

    /**
     * Tempo zerou.
     *
     * ✅ Ajuste (bugfix):
     * - Agora NO NORMAL também soma:
     *   game.questionsAnswered += 1
     *   game.totalTimeAccumulatedMs += spentMs
     *
     * Sem isso, partidas com time-up ficavam com totalTime=0 e avgTime=0,
     * quebrando records e tela de recordes.
     */
    fun onTimeUp() {
        val q = currentQuestionOrNull() ?: return
        if (game.answerLocked) return

        game.answerLocked = true
        timer.pause()
        setOptionsEnabled(false)

        val spentMs = timer.spentMs().coerceAtLeast(0L)

        // =========================================================================================
        // WEEKLY
        // =========================================================================================
        if (isWeeklyMode()) {
            val w = weekly!!
            if (w.startedAtMs == 0L) w.startedAtMs = System.currentTimeMillis()

            // ✅ conta pergunta e tempo
            game.questionsAnswered += 1
            game.totalTimeAccumulatedMs += spentMs

            w.wrong += 1
            fx.glowQuestionCard(false)
            fx.flashFx(false)
            overlay.showFloatingChip("⏱ Tempo esgotado", R.drawable.ic_delete, false)

            syncGameHudFromWeekly()
            advanceToNextQuestionWithDelayOrCuriosity(wasCorrect = false)
            return
        }

        // =========================================================================================
        // NORMAL / SECRETO
        // =========================================================================================

        // ✅ BUGFIX: conta pergunta + tempo mesmo no time-up
        game.questionsAnswered += 1
        game.totalTimeAccumulatedMs += spentMs

        val oldScore = game.score
        val oldStreak = game.streak

        game.wrongAnswers += 1
        playSfxWrong()
        vibrateWrong()

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

        advanceToNextQuestionWithDelayOrCuriosity(wasCorrect = false)
    }

    /**
     * Avança com um delay (mais curto quando acerta, mais longo quando erra)
     * e opcionalmente mostra curiosidade.
     */
    private fun advanceToNextQuestionWithDelayOrCuriosity(wasCorrect: Boolean) {
        activity.lifecycleScope.launch {
            delay(if (wasCorrect) 520L else 880L)

            fun goNext() {
                game.currentIndex += 1
                val total = questionsProvider().size

                // ✅ WEEKLY: só termina quando acabar as perguntas
                if (isWeeklyMode()) {
                    if (total == 0) {
                        endWeeklyIfNeeded()
                        return
                    }
                    if (game.currentIndex < total) {
                        displayQuestion(withEnterAnim = false)
                    } else {
                        endWeeklyIfNeeded()
                    }
                    return
                }

                // ✅ NORMAL: respeita limite de erro e final
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
