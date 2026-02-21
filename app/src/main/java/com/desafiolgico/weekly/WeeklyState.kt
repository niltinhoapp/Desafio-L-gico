package com.desafiolgico.weekly

data class WeeklyState(
    var enabled: Boolean = false,

    var weekId: String = "",
    var roundId: String = "R1",
    var attemptNo: Int = 1,

    var correct: Int = 0,
    var wrong: Int = 0,

    var startedAtMs: Long = 0L,
    var finishedAtMs: Long = 0L,

    var backgroundCount: Int = 0,
    var backgroundTotalMs: Long = 0L,
    var wentBackgroundAtMs: Long = 0L,

    var questionIds: List<String> = emptyList(),
    var updateHud: ((correct: Int, wrong: Int) -> Unit)? = null

) {
    fun reset() {
        enabled = false
        weekId = ""
        roundId = "R1"
        attemptNo = 1
        correct = 0
        wrong = 0
        startedAtMs = 0L
        finishedAtMs = 0L
        backgroundCount = 0
        backgroundTotalMs = 0L
        wentBackgroundAtMs = 0L
        questionIds = emptyList()
    }
}
