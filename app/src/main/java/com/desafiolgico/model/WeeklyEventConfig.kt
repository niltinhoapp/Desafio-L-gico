package com.desafiolgico.model

import com.google.firebase.Timestamp

data class WeeklyEventConfig(
    val active: Boolean = false,
    val weekId: String = "",

    // Regras do campeonato
    val attemptLimit: Int = 3,
    val questionsPerRun: Int = 15,
    val minCorrect: Int = 13,

    // Limites do campeonato
    val limitPlayers: Int = 150,

    // Tempo: ex. 30s por questão -> 15 * 30s = 450s
    val timePerQuestionMs: Long = 30_000L,
    val timeToleranceMs: Long = 2_000L, // tolerância pra variações

    // Anti-consulta / anti “sair do app”
    val maxBackgroundTotalMs: Long = 15_000L,
    val maxBackgroundCount: Int = 2,

    // Regras extras (se quiser)
    val maxWrongAllowed: Int = 2,

    // Datas
    val startAt: Timestamp? = null,
    val endAt: Timestamp? = null
) {
    fun maxTimeMs(): Long = questionsPerRun * timePerQuestionMs
}
