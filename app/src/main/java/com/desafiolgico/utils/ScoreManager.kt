package com.desafiolgico.utils

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.math.max
import kotlin.random.Random

/**
 * Classe responsável pela lógica de pontuação, streak, bônus e recompensas.
 *
 * REGRAS (2026):
 * - NORMAL: streak existe e influencia bônus.
 * - SECRETO: NÃO EXISTE STREAK (não incrementa, não reseta, não entra no cálculo).
 * - Pontuação da sessão sempre soma e também atualiza o total global (mapa/geral).
 * - Marco de pontuação (500 pts) dá moedas (1x por marco, por sessão).
 */
class ScoreManager(private val context: Context) {

    companion object {
        private const val TAG = "ScoreManager"

        // Bases
        private const val BASE_POINTS_PER_CORRECT = 20
        private const val STREAK_BONUS_INCREMENT = 5
        private const val TIME_BONUS_MAX = 10
        private const val POINTS_DEDUCTION_WRONG = 5

        // Marco / moedas (sessão)
        private const val COINS_PER_MILESTONE = 50
        private const val SCORE_MILESTONE_POINTS = 500

        // Ouro
        private const val GOLD_BONUS_CHANCE_PERCENT = 20

        // Tempo bônus
        private const val GREEN_THRESHOLD_PERCENT = 70 // 70% restante => +10
        private const val YELLOW_THRESHOLD_PERCENT = 40 // 40% restante => +5
    }

    private var totalCorrectAnswers = 0

    // controla marcos sem duplicar (nunca diminui)
    private var lastMilestoneIndex = 0

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

    /**
     * ✅ Ajusta score da sessão (ex: restore/retorno do secreto).
     * Também recalibra o controle de marcos para NÃO disparar moedas ao restaurar.
     */
    fun setOverallScore(score: Int) {
        val v = score.coerceAtLeast(0)
        _overallScoreLive.value = v
        lastMilestoneIndex = v / SCORE_MILESTONE_POINTS
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
        val percentRemaining = ((remainingTimeInMillis * 100) / totalTimeInMillis).toInt()
        return when {
            percentRemaining > GREEN_THRESHOLD_PERCENT -> TIME_BONUS_MAX
            percentRemaining > YELLOW_THRESHOLD_PERCENT -> TIME_BONUS_MAX / 2
            else -> 0
        }
    }

    private fun calculateGoldBonus(streakForCalc: Int): Int {
        if (streakForCalc < 7) return 0
        val shouldGive = Random.nextInt(100) < GOLD_BONUS_CHANCE_PERCENT
        if (!shouldGive) return 0
        val bonus = Random.nextInt(25, 51)
        Log.d(TAG, "GoldBonus aplicado: streak=$streakForCalc, bonus=$bonus")
        return bonus
    }

    // =====================================================
    // MARCOS (500 pts) => moedas (1x por marco, por sessão)
    // =====================================================

    private fun handleMilestoneReward(updatedSessionScore: Int) {
        val milestoneIndex = updatedSessionScore / SCORE_MILESTONE_POINTS
        if (milestoneIndex <= lastMilestoneIndex) return

        val numMilestones = milestoneIndex - lastMilestoneIndex
        val rewardCoins = numMilestones * COINS_PER_MILESTONE
        val milestonePoints = milestoneIndex * SCORE_MILESTONE_POINTS

        CoinManager.addCoins(context, rewardCoins, reason = "Marco $milestonePoints pts")
        lastMilestoneIndex = milestoneIndex

        Log.d(TAG, "🏆 Marco atingido: $milestonePoints pts => +$rewardCoins moedas")
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

        _currentStreakLive.value = newStreak
        updateHighestStreakIfNeeded(newStreak)

        if (goldBonus > 0) onBonusVisual?.invoke(goldBonus)

        Log.d(TAG, "✅ ACERTO NORMAL: streak=$newStreak, +$totalPointsEarned")
    }

    /**
     * Revisão NORMAL: +streak, +0 pts.
     */
    fun onCorrectAnswer() {
        val old = _currentStreakLive.value ?: 0
        val newStreak = old + 1
        _currentStreakLive.value = newStreak
        updateHighestStreakIfNeeded(newStreak)
        Log.d(TAG, "✅ REVIEW NORMAL: streak=$newStreak (+0)")
    }

    /**
     * Erro NORMAL: deduz pontos e reseta streak.
     */
    fun onWrongAnswer() {
        applyPointsInternal(-POINTS_DEDUCTION_WRONG)
        _currentStreakLive.value = 0
        Log.d(TAG, "❌ ERRADO NORMAL: -$POINTS_DEDUCTION_WRONG (streak=0)")
    }

    // =====================================================
    // SECRETO (sem streak)
    // =====================================================

    /**
     * Acerto SECRETO: soma pontos SEM streak (base + tempo).
     * - Não usa streakBonus
     * - Não usa goldBonus
     * - Não altera currentStreak
     * - Também incrementa acerto global (mapa)
     */
    fun addScoreSecret(remainingTimeInMillis: Long, totalTimeInMillis: Long) {
        totalCorrectAnswers++
        GameDataManager.incrementTotalCorrectGlobal(context, 1)

        val timeBonus = calculateTimeBonus(remainingTimeInMillis, totalTimeInMillis)
        val totalPointsEarned = BASE_POINTS_PER_CORRECT + timeBonus

        applyPointsInternal(totalPointsEarned)

        Log.d(TAG, "⚡ ACERTO SECRETO: +$totalPointsEarned (sem streak)")
    }

    /**
     * Erro SECRETO: deduz pontos, mas NÃO mexe na streak.
     */
    fun onWrongAnswerSecret() {
        applyPointsInternal(-POINTS_DEDUCTION_WRONG)
        Log.d(TAG, "⚡❌ ERRADO SECRETO: -$POINTS_DEDUCTION_WRONG (sem streak)")
    }

    // =====================================================
    // API GENÉRICA (sem mexer na streak)
    // =====================================================

    fun onWrongAnswerNoStreak() {
        applyPointsInternal(-POINTS_DEDUCTION_WRONG)
        Log.d(TAG, "❌ ERRADO (NO STREAK): -$POINTS_DEDUCTION_WRONG")
    }

    /**
     * Soma pontos SEM mexer na streak.
     * @param streakNow streak “virtual” usado só para cálculo (no secreto = 0).
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
        applyPointsInternal(totalPointsEarned)

        if (goldBonus > 0) onBonusVisual?.invoke(goldBonus)

        Log.d(TAG, "✅ addScoreNoStreak: streak=$streak, +$totalPointsEarned")
    }

    // =====================================================
    // CORE: aplicar pontos na sessão + total global + marco
    // =====================================================

    private fun applyPointsInternal(delta: Int) {
        val current = _overallScoreLive.value ?: 0
        val updatedSessionScore = (current + delta).coerceAtLeast(0)

        _overallScoreLive.value = updatedSessionScore

        // total global (histórico)
        if (delta != 0) {
            GameDataManager.addScoreToOverallTotal(context, delta)
        }

        // marcos baseados no score da sessão (nunca duplica)
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

    // =====================================================
    // RESET
    // =====================================================

    /**
     * Reset completo da sessão (novo jogo).
     * - Zera score, streak, contadores e marcos de sessão.
     * - NÃO mexe no total global (histórico).
     */
    fun reset() {
        totalCorrectAnswers = 0
        _overallScoreLive.value = 0
        _currentStreakLive.value = 0
        lastMilestoneIndex = 0
        Log.d(TAG, "♻️ Reset sessão (score/streak).")
    }
}
