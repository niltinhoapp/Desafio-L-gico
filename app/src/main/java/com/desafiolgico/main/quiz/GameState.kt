package com.desafiolgico.main.quiz

/**
 * Estado único do jogo.
 * - NORMAL usa score/streak
 * - WEEKLY usa WeeklyState (separado)
 * - TimerController lê/escreve remainingMs/totalMs/timerPaused/lastCriticalSecond
 */
data class GameState(
    // progresso
    var currentIndex: Int = 0,
    var wrongAnswers: Int = 0,
    var maxWrong: Int = 5,

    var questionsAnswered: Int = 0,
    var totalTimeAccumulatedMs: Long = 0L,

    // trava de resposta
    var answerLocked: Boolean = false,

    // timer (controlado pelo TimerController)
    var timerPaused: Boolean = false,
    var remainingMs: Long = 0L,
    var totalMs: Long = 0L,
    var lastCriticalSecond: Int = -1,

    // HUD NORMAL
    var score: Int = 0,
    var streak: Int = 0
) {

    /** Reseta tudo (útil quando começa uma partida nova) */
    fun resetAll(maxWrongDefault: Int = 5) {
        currentIndex = 0
        wrongAnswers = 0
        maxWrong = maxWrongDefault

        questionsAnswered = 0
        totalTimeAccumulatedMs = 0L

        answerLocked = false

        timerPaused = false
        remainingMs = 0L
        totalMs = 0L
        lastCriticalSecond = -1

        score = 0
        streak = 0
    }

    /** Reseta só o timer (quando troca de pergunta, por exemplo) */
    fun resetTimerState() {
        answerLocked = false
        timerPaused = false
        remainingMs = 0L
        totalMs = 0L
        lastCriticalSecond = -1
    }
}
