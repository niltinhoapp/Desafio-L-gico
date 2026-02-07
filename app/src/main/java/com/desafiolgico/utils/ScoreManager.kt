package com.desafiolgico.utils

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.random.Random
import kotlin.math.roundToInt
import com.desafiolgico.utils.CoinManager



/**
 * Classe responsável pela lógica de pontuação, streak, bônus e recompensas.
 * Gerencia a pontuação e o streak apenas da sessão atual de jogo.
 * NOTA DE REVISÃO: Os cálculos de bônus de Streak e Ouro foram CENTRALIZADOS aqui.
 */
class ScoreManager(private val context: Context) {

    companion object {
        // As constantes agora representam as bases de cálculo:
        private const val BASE_POINTS_PER_CORRECT = 20 // Base original (Base: 20 pts por acerto)
        private const val STREAK_BONUS_INCREMENT = 5   // +5 pontos por nível de Streak (como na TestActivity)
        private const val TIME_BONUS_MAX = 10          // Máximo de bônus de tempo
        private const val POINTS_DEDUCTION_WRONG = 5
        private const val COINS_PER_MILESTONE = 50 // Recompensa por marco de pontuação

        // Constantes de Limite de Tempo para Bônus (50% e 20% do tempo total)
        private const val GREEN_THRESHOLD_PERCENT = 70 // 70% do tempo restante -> Bônus 10
        private const val YELLOW_THRESHOLD_PERCENT = 40 // 40% do tempo restante -> Bônus 5
    }

    private var overallScore = 0
    private var totalCorrectAnswers = 0
    private var lastMilestoneCheck = 0 // Para controle do marco de 500 pontos

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
    fun setOverallScore(score: Int) { _overallScoreLive.value = score }
    fun resetStreak() { _currentStreakLive.value = 0 }

    // NOTA: Removida a função getStreakMultiplier não utilizada.

    /**
     * Tenta aplicar o "Bônus de Ouro" aleatório.
     */


    /**
     * Calcula o bônus de tempo com base no tempo total da questão.
     * @param remainingTimeInMillis Tempo restante em milissegundos.
     * @param totalTimeInMillis Tempo total da questão.
     */
    private fun calculateTimeBonus(remainingTimeInMillis: Long, totalTimeInMillis: Long): Int {
        if (totalTimeInMillis <= 0) return 0
        val percentRemaining = (remainingTimeInMillis * 100 / totalTimeInMillis).toInt()

        return when {
            percentRemaining > GREEN_THRESHOLD_PERCENT -> TIME_BONUS_MAX // Ex: 10 pts
            percentRemaining > YELLOW_THRESHOLD_PERCENT -> TIME_BONUS_MAX / 2 // Ex: 5 pts
            else -> 0
        }
    }

    /**
     * Chamado quando o jogador acerta uma questão.
     * Recebe o tempo restante para calcular o bônus de tempo.
     */
    fun addScore(remainingTimeInMillis: Long, totalTimeInMillis: Long) {
        val oldStreak = _currentStreakLive.value ?: 0
        val newStreak = oldStreak + 1
        totalCorrectAnswers++
        GameDataManager.incrementTotalCorrectGlobal(context, 1) // ✅ progresso do mapa (persistente)


        val streakBonus = newStreak * STREAK_BONUS_INCREMENT
        val timeBonus = calculateTimeBonus(remainingTimeInMillis, totalTimeInMillis)

        // 👇 AQUI: usar sua função nova
        val goldBonus = calculateGoldBonus(newStreak)

        val totalPointsEarned = BASE_POINTS_PER_CORRECT + streakBonus + timeBonus + goldBonus

        val updatedSessionScore = (_overallScoreLive.value ?: 0) + totalPointsEarned
        _overallScoreLive.value = updatedSessionScore.coerceAtLeast(0)
        overallScore = updatedSessionScore

        val currentHighest = _highestStreakLive.value ?: 0
        if (newStreak > currentHighest) {
            _highestStreakLive.value = newStreak
            GameDataManager.updateHighestStreakIfNeeded(context, newStreak)
            onNewRecord?.invoke(newStreak)
        }

        _currentStreakLive.value = newStreak
        GameDataManager.addScoreToOverallTotal(context, totalPointsEarned.coerceAtLeast(0))

        // Recompensa por marco de pontuação
        if (updatedSessionScore / 500 > lastMilestoneCheck / 500) {
            val numMilestones = updatedSessionScore / 500 - lastMilestoneCheck / 500
            val rewardCoins = numMilestones * COINS_PER_MILESTONE
            CoinManager.addCoins(context, rewardCoins, reason = "Marco ${updatedSessionScore / 500 * 500} pts")
           lastMilestoneCheck = updatedSessionScore
        }

        Log.d(
            "ScoreManager",
            "✅ ACERTO: streak=$newStreak, goldBonus=$goldBonus, total=$totalPointsEarned"
        )
    }
    /**
     * ✅ Soma pontos SEM mexer na streak.
     * Use quando a streak já foi incrementada fora (ex: revisão / anti-farm).
     *
     * @param remainingTimeInMillis tempo restante
     * @param totalTimeInMillis tempo total da questão
     * @param streakNow streak atual (já incrementada)
     */
    fun addScoreNoStreak(remainingTimeInMillis: Long, totalTimeInMillis: Long, streakNow: Int) {
        val streak = streakNow.coerceAtLeast(0)

        val streakBonus = streak * STREAK_BONUS_INCREMENT
        val timeBonus = calculateTimeBonus(remainingTimeInMillis, totalTimeInMillis)
        val goldBonus = calculateGoldBonus(streak)

        val totalPointsEarned = BASE_POINTS_PER_CORRECT + streakBonus + timeBonus + goldBonus

        val updatedSessionScore = (_overallScoreLive.value ?: 0) + totalPointsEarned
        _overallScoreLive.value = updatedSessionScore.coerceAtLeast(0)
        overallScore = updatedSessionScore

        // ✅ Atualiza recorde de streak, mas SEM alterar streak atual
        val currentHighest = _highestStreakLive.value ?: 0
        if (streak > currentHighest) {
            _highestStreakLive.value = streak
            GameDataManager.updateHighestStreakIfNeeded(context, streak)
            onNewRecord?.invoke(streak)
        }

        // ✅ total global (para mapa / total geral) — só quando pontua de verdade
        GameDataManager.addScoreToOverallTotal(context, totalPointsEarned.coerceAtLeast(0))

        // ✅ recompensa por marco
        if (updatedSessionScore / 500 > lastMilestoneCheck / 500) {
            val numMilestones = updatedSessionScore / 500 - lastMilestoneCheck / 500
            val rewardCoins = numMilestones * COINS_PER_MILESTONE
            GameDataManager.addCoins(context, rewardCoins)
            lastMilestoneCheck = updatedSessionScore
        }

        Log.d(
            "ScoreManager",
            "✅ addScoreNoStreak: streak=$streak, goldBonus=$goldBonus, total=$totalPointsEarned"
        )
    }



    fun onCorrectAnswer() {
        val old = _currentStreakLive.value ?: 0
        val newStreak = old + 1
        _currentStreakLive.value = newStreak

        val currentHighest = _highestStreakLive.value ?: 0
        if (newStreak > currentHighest) {
            _highestStreakLive.value = newStreak
            GameDataManager.updateHighestStreakIfNeeded(context, newStreak)
            onNewRecord?.invoke(newStreak)
        }

        Log.d("ScoreManager", "✅ REVIEW HIT: streak=$newStreak (+0 pts)")
    }


    private fun calculateGoldBonus(currentStreak: Int): Int {
        if (currentStreak < 7) {
            Log.d("ScoreManager", "GoldBonus: streak $currentStreak < 7 → 0")
            return 0
        }

        val shouldGiveBonus = (0..0).random() == 0
        if (!shouldGiveBonus) {
            Log.d("ScoreManager", "GoldBonus: streak $currentStreak, NÃO caiu na chance → 0")
            return 0
        }

        val bonus = (25..50).random()
        Log.d("ScoreManager", "GoldBonus: streak $currentStreak, BONUS APLICADO = $bonus")
        return bonus
    }
    fun setCurrentStreak(streak: Int) {
        _currentStreakLive.value = streak
    }





    /**
     * Quando o jogador erra.
     */
    fun onWrongAnswer() {
        val deduction = -POINTS_DEDUCTION_WRONG
        val updatedSessionScore = (_overallScoreLive.value ?: 0) + deduction
        _overallScoreLive.value = updatedSessionScore.coerceAtLeast(0)
        _currentStreakLive.value = 0

        // NOTA: A dedução de pontos também é registrada no total global.
        GameDataManager.addScoreToOverallTotal(context, deduction)
        Log.d("ScoreManager", "❌ ERRADO: -$POINTS_DEDUCTION_WRONG pts. Streak resetado.")
    }

    /**
     * Reset completo da pontuação da sessão (ex: novo jogo).
     */
    fun reset() {
        overallScore = 0
        totalCorrectAnswers = 0
        _overallScoreLive.value = 0
        _currentStreakLive.value = 0
        lastMilestoneCheck = 0 // Reseta o controle do marco
        Log.d("ScoreManager", "♻️ Reset de pontuação e streak da sessão.")
    }
}
