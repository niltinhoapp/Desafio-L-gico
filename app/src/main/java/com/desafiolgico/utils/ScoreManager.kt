package com.desafiolgico.utils

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.random.Random

/**
 * Classe responsável pela lógica de pontuação, streak, bônus e recompensas.
 *
 * REGRAS (2026):
 * - NORMAL: streak existe e influencia bônus.
 * - SECRETO: NÃO EXISTE STREAK (não incrementa, não reseta, não entra no cálculo).
 * - Pontuação da sessão sempre soma e também atualiza o total global (mapa/geral).
 * - Marco de pontuação (500 pts) dá moedas.
 */
class ScoreManager(private val context: Context) {

    companion object {
        // Bases
        private const val BASE_POINTS_PER_CORRECT = 20
        private const val STREAK_BONUS_INCREMENT = 5
        private const val TIME_BONUS_MAX = 10
        private const val POINTS_DEDUCTION_WRONG = 5

        // Marco / moedas
        private const val COINS_PER_MILESTONE = 50
        private const val SCORE_MILESTONE_POINTS = 500

        // Ouro
        private const val GOLD_BONUS_CHANCE_PERCENT = 20

        // Tempo bônus
        private const val GREEN_THRESHOLD_PERCENT = 70 // 70% restante => +10
        private const val YELLOW_THRESHOLD_PERCENT = 40 // 40% restante => +5
    }

    private var totalCorrectAnswers = 0

    // controle de marco (baseado no score da sessão)
    private var lastMilestoneCheck = 0

    private val _overallScoreLive = MutableLiveData(0)
    val overallScoreLive: LiveData<Int> get() = _overallScoreLive

    private val _currentStreakLive = MutableLiveData(0)
    val currentStreakLive: LiveData<Int> get() = _currentStreakLive

    private val _highestStreakLive = MutableLiveData(GameDataManager.getHighestStreak(context))
    val highestStreakLive: LiveData<Int> get() = _highestStreakLive

    var onBonusVisual: ((bonus: Int) -> Unit)? = null
    var onNewRecord: ((newRecord: Int) -> Unit)? = null

    fun getOverallScore(): Int = _overallScoreLive.value ?: 0
    fun getTotalCorrectAnswers(): Int = totalCorrectAnswers

    fun setOverallScore(score: Int) {
        _overallScoreLive.value = score.coerceAtLeast(0)
    }

    fun setCurrentStreak(streak: Int) {
        _currentStreakLive.value = streak.coerceAtLeast(0)
    }

    fun resetStreak() {
        _currentStreakLive.value = 0
    }

    // =====================================================
    // BÔNUS
    // =====================================================

    private fun calculateTimeBonus(remainingTimeInMillis: Long, totalTimeInMillis: Long): Int {
        if (totalTimeInMillis <= 0L) return 0
        val percentRemaining = (remainingTimeInMillis * 100 / totalTimeInMillis).toInt()

        return when {
            percentRemaining > GREEN_THRESHOLD_PERCENT -> TIME_BONUS_MAX
            percentRemaining > YELLOW_THRESHOLD_PERCENT -> TIME_BONUS_MAX / 2
            else -> 0
        }
    }

    private fun calculateGoldBonus(streakForCalc: Int): Int {
        // Ouro só existe no NORMAL e com streak alta (como você tinha)
        if (streakForCalc < 7) return 0

        val shouldGiveBonus = Random.nextInt(100) < GOLD_BONUS_CHANCE_PERCENT
        if (!shouldGiveBonus) return 0

        val bonus = (25..50).random()
        Log.d("ScoreManager", "GoldBonus aplicado: streak=$streakForCalc, bonus=$bonus")
        return bonus
    }

    // =====================================================
    // MARCOS (500 pts) => moedas
    // =====================================================

    private fun handleMilestoneReward(updatedSessionScore: Int) {
        val milestoneIndex = updatedSessionScore / SCORE_MILESTONE_POINTS
        val lastMilestoneIndex = lastMilestoneCheck / SCORE_MILESTONE_POINTS
        if (milestoneIndex <= lastMilestoneIndex) return

        val numMilestones = milestoneIndex - lastMilestoneIndex
        val rewardCoins = numMilestones * COINS_PER_MILESTONE
        val milestonePoints = milestoneIndex * SCORE_MILESTONE_POINTS

        CoinManager.addCoins(context, rewardCoins, reason = "Marco $milestonePoints pts")

        lastMilestoneCheck = updatedSessionScore
        Log.d("ScoreManager", "🏆 Marco atingido: $milestonePoints pts => +$rewardCoins moedas")
    }

    // =====================================================
    // NORMAL (com streak)
    // =====================================================

    /**
     * Acerto NORMAL: incrementa streak e soma pontos (base + streak + tempo + ouro).
     * Também incrementa o total de acertos global para o mapa.
     */
    fun addScore(remainingTimeInMillis: Long, totalTimeInMillis: Long) {
        val oldStreak = _currentStreakLive.value ?: 0
        val newStreak = oldStreak + 1

        totalCorrectAnswers++
        GameDataManager.incrementTotalCorrectGlobal(context, 1)

        val streakBonus = newStreak * STREAK_BONUS_INCREMENT
        val timeBonus = calculateTimeBonus(remainingTimeInMillis, totalTimeInMillis)
        val goldBonus = calculateGoldBonus(newStreak)

        val totalPointsEarned = BASE_POINTS_PER_CORRECT + streakBonus + timeBonus + goldBonus
        applyPointsInternal(totalPointsEarned)

        // atualiza streak e record
        _currentStreakLive.value = newStreak
        updateHighestStreakIfNeeded(newStreak)

        Log.d("ScoreManager", "✅ ACERTO NORMAL: streak=$newStreak, total=+$totalPointsEarned")
    }

    /**
     * Revisão NORMAL: +streak, +0 pts (como você tinha).
     */
    fun onCorrectAnswer() {
        val old = _currentStreakLive.value ?: 0
        val newStreak = old + 1
        _currentStreakLive.value = newStreak
        updateHighestStreakIfNeeded(newStreak)
        Log.d("ScoreManager", "✅ REVIEW NORMAL: streak=$newStreak (+0 pts)")
    }

    /**
     * Erro NORMAL: deduz pontos e reseta streak.
     */
    fun onWrongAnswer() {
        applyPointsInternal(-POINTS_DEDUCTION_WRONG)
        _currentStreakLive.value = 0
        Log.d("ScoreManager", "❌ ERRADO NORMAL: -$POINTS_DEDUCTION_WRONG pts (streak resetado)")
    }

    // =====================================================
    // SECRETO (sem streak)
    // =====================================================

    /**
     * Acerto SECRETO: soma pontos SEM streak (base + tempo apenas).
     * - Não usa streakBonus
     * - Não usa goldBonus
     * - Não altera currentStreak
     * - Também incrementa acerto global (mapa), se você quiser manter progresso.
     */
    fun addScoreSecret(remainingTimeInMillis: Long, totalTimeInMillis: Long) {
        totalCorrectAnswers++
        GameDataManager.incrementTotalCorrectGlobal(context, 1)

        val timeBonus = calculateTimeBonus(remainingTimeInMillis, totalTimeInMillis)
        val totalPointsEarned = BASE_POINTS_PER_CORRECT + timeBonus

        applyPointsInternal(totalPointsEarned)

        Log.d("ScoreManager", "⚡ ACERTO SECRETO: +$totalPointsEarned (sem streak)")
    }

    /**
     * Erro SECRETO: deduz pontos, mas NÃO mexe na streak.
     */
    fun onWrongAnswerSecret() {
        applyPointsInternal(-POINTS_DEDUCTION_WRONG)
        Log.d("ScoreManager", "⚡❌ ERRADO SECRETO: -$POINTS_DEDUCTION_WRONG (sem streak)")
    }

    // =====================================================
    // CORE: aplicar pontos na sessão + total global + marco
    // =====================================================

    private fun applyPointsInternal(delta: Int) {
        val current = _overallScoreLive.value ?: 0
        val updatedSessionScore = (current + delta).coerceAtLeast(0)

        _overallScoreLive.value = updatedSessionScore

        // total global (pode ser negativo também, como você já fazia)
        GameDataManager.addScoreToOverallTotal(context, delta)

        handleMilestoneReward(updatedSessionScore)
    }

    private fun updateHighestStreakIfNeeded(streak: Int) {
        val currentHighest = _highestStreakLive.value ?: 0
        if (streak > currentHighest) {
            _highestStreakLive.value = streak
            GameDataManager.updateHighestStreakIfNeeded(context, streak)
            onNewRecord?.invoke(streak)
        }
    }
    fun onWrongAnswerNoStreak() {
        val deduction = -POINTS_DEDUCTION_WRONG
        val updatedSessionScore = (_overallScoreLive.value ?: 0) + deduction
        _overallScoreLive.value = updatedSessionScore.coerceAtLeast(0)

        // ✅ total global também registra (se você quer)
        GameDataManager.addScoreToOverallTotal(context, deduction)

        Log.d("ScoreManager", "❌ ERRADO (NO STREAK): -$POINTS_DEDUCTION_WRONG pts.")
    }

    /**
     * ✅ Soma pontos SEM mexer na streak.
     * Use quando você NÃO quer streak (ex: modo secreto) ou quando a streak já é controlada fora.
     *
     * @param remainingTimeInMillis tempo restante
     * @param totalTimeInMillis tempo total da questão
     * @param streakNow streak “virtual” usado só para cálculo de bônus (no secreto = 0)
     */
    fun addScoreNoStreak(
        remainingTimeInMillis: Long,
        totalTimeInMillis: Long,
        streakNow: Int
    ) {
        val streak = streakNow.coerceAtLeast(0)

        val streakBonus = streak * STREAK_BONUS_INCREMENT
        val timeBonus = calculateTimeBonus(remainingTimeInMillis, totalTimeInMillis)
        val goldBonus = calculateGoldBonus(streak)

        val totalPointsEarned = BASE_POINTS_PER_CORRECT + streakBonus + timeBonus + goldBonus

        val updatedSessionScore = (_overallScoreLive.value ?: 0) + totalPointsEarned
        _overallScoreLive.value = updatedSessionScore.coerceAtLeast(0)

        // ✅ total global (se você quer que o secreto some no total geral / mapa)
        GameDataManager.addScoreToOverallTotal(context, totalPointsEarned.coerceAtLeast(0))

        // ✅ recompensa por marco (moedas)
        handleMilestoneReward(updatedSessionScore)

        Log.d(
            "ScoreManager",
            "✅ addScoreNoStreak: streak=$streak, goldBonus=$goldBonus, total=$totalPointsEarned"
        )
    }


    // =====================================================
    // RESET
    // =====================================================

    /**
     * Reset completo da sessão (novo jogo).
     * - Zera score, streak, contadores e marco.
     * - NÃO mexe no total global (porque é histórico).
     */
    fun reset() {
        totalCorrectAnswers = 0
        _overallScoreLive.value = 0
        _currentStreakLive.value = 0
        lastMilestoneCheck = 0
        Log.d("ScoreManager", "♻️ Reset de pontuação/streak da sessão.")
    }
}
