package com.desafiolgico.model


data class WeeklyEventConfig(
    val active: Boolean = false,
    val weekId: String = "",
    val attemptLimit: Int = 3,
    val minCorrect: Int = 13,
    val maxBackgroundSeconds: Int = 3
)
