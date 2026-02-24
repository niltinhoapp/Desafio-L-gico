package com.desafiolgico.main.quiz

/**
 * Estado único do jogo (NORMAL).
 *
 * - NORMAL usa score/streak + maxWrong/wrongAnswers.
 * - WEEKLY usa WeeklyState (separado). Em WEEKLY, este estado serve mais como apoio de UI/timer.
 * - TimerController lê/escreve remainingMs/totalMs/timerPaused/lastCriticalSecond.
 *
 * Observação:
 * - currentIndex é controlado pelo engine (GameState não avança sozinho).
 */
data class GameState(
    // progresso
    var currentIndex: Int = 0,
    var questionsAnswered: Int = 0,
    var totalTimeAccumulatedMs: Long = 0L,

    // regras NORMAL
    var wrongAnswers: Int = 0,
    var maxWrong: Int = 5,

    // trava de resposta
    var answerLocked: Boolean = false,

    // timer (controlado pelo TimerController)
    var timerPaused: Boolean = false,
    var remainingMs: Long = 0L,
    var totalMs: Long = 0L,
    var lastCriticalSecond: Int = -1,

    // HUD NORMAL
    var score: Int = 0,
    var streak: Int = 0,

    // debug/telemetria (opcional)
    var sessionStartedAtMs: Long = 0L,
    var sessionFinishedAtMs: Long = 0L
) {

    // =========================
    // Helpers de progresso
    // =========================

    /** Quantas questões faltam, incluindo a questão atual (currentIndex aponta para a atual). */
    fun remainingQuestions(total: Int): Int =
        (total - currentIndex).coerceAtLeast(0)

    fun hasEnded(total: Int): Boolean =
        currentIndex >= total

    // =========================
    // NORMAL rules helpers
    // =========================

    fun isGameOverByWrong(): Boolean =
        wrongAnswers >= maxWrong

    /**
     * Registra uma resposta no modo NORMAL:
     * - incrementa contadores e acumula tempo
     * - NÃO avança currentIndex (isso é responsabilidade do engine)
     */
    fun registerAnswerNormal(isCorrect: Boolean, spentMs: Long) {
        questionsAnswered += 1
        totalTimeAccumulatedMs += spentMs.coerceAtLeast(0L)
        if (!isCorrect) wrongAnswers += 1
    }

    // =========================
    // Sessão (opcional)
    // =========================

    fun markSessionStarted(nowMs: Long = System.currentTimeMillis()) {
        if (sessionStartedAtMs == 0L) sessionStartedAtMs = nowMs
        sessionFinishedAtMs = 0L
    }

    fun markSessionFinished(nowMs: Long = System.currentTimeMillis()) {
        if (sessionStartedAtMs == 0L) sessionStartedAtMs = nowMs
        sessionFinishedAtMs = nowMs.coerceAtLeast(sessionStartedAtMs)
    }

    // =========================
    // Resets
    // =========================

    /** Reseta tudo (útil quando começa uma partida nova) */
    fun resetAll(maxWrongDefault: Int = 5) {
        currentIndex = 0
        questionsAnswered = 0
        totalTimeAccumulatedMs = 0L

        wrongAnswers = 0
        maxWrong = maxWrongDefault.coerceAtLeast(1)

        answerLocked = false

        timerPaused = false
        remainingMs = 0L
        totalMs = 0L
        lastCriticalSecond = -1

        score = 0
        streak = 0

        sessionStartedAtMs = 0L
        sessionFinishedAtMs = 0L
    }

    /** Reseta só o “round progresso” (índice/erros/tempo acumulado). */
    fun resetRunProgress(maxWrongDefault: Int = maxWrong) {
        currentIndex = 0
        questionsAnswered = 0
        totalTimeAccumulatedMs = 0L

        wrongAnswers = 0
        maxWrong = maxWrongDefault.coerceAtLeast(1)

        answerLocked = false
    }

    /** Reseta só o HUD normal (score/streak). */
    fun resetHudNormal() {
        score = 0
        streak = 0
    }

    /** Reseta só o timer (quando troca de pergunta, por exemplo). */
    fun resetTimerState() {
        answerLocked = false
        timerPaused = false
        remainingMs = 0L
        totalMs = 0L
        lastCriticalSecond = -1
    }
}
