package com.desafiolgico.utils

import android.content.Context
import android.util.Log
import kotlin.math.floor

/**
 * Gerencia a economia de moedas do jogo e o estado de avatares.
 * ✅ Fonte única de verdade: GameDataManager (que usa SecurePrefs internamente).
 *
 * Regras:
 * - Ganhos só por motivos permitidos (anti-exploit).
 * - Multiplicador persistente.
 * - Dedução sempre protegida (não deixa negativo).
 */
object CoinManager {

    private const val TAG = "CoinManager"

    // =====================================================
    // 🔢 Constantes principais
    // =====================================================
    const val REWARD_SCORE_COINS = 50
    const val REWARD_STREAK_COINS = 10
    const val REWARD_AD_COINS = 50

    const val BONUS_MULTIPLIER_DEFAULT = 1.0
    const val BONUS_MULTIPLIER_EVENTO = 1.5

    private const val SCORE_MILESTONE = 500
    const val AVATAR_COST = 150

    // ✅ Motivos permitidos (use SEMPRE estas constantes)
    const val AD_REWARD = "AdReward"
    const val SECRET = "Fase Secreta"
    const val STREAK_PREFIX = "Streak"
    const val MARCO_PREFIX = "Marco"

    // Constantes da Fase Secreta
    const val SECRET_LEVEL_COIN_REWARD = 25
    const val SECRET_LEVEL_XP_REWARD = 50

    // =====================================================
    // 🧩 Estado e Persistência de Multiplicador (SecurePrefs)
    // =====================================================
    private const val PREF_MULT = "coin_multiplier_v1"
    private var currentMultiplier = BONUS_MULTIPLIER_DEFAULT

    private fun keyForUser(): String {
        val uid = GameDataManager.currentUserId.ifBlank { "guest" }
        return "${uid}_$PREF_MULT"
    }

    /**
     * Carrega o multiplicador salvo (se existir). Chame no boot do app (Application/primeira Activity).
     */
    fun loadMultiplier(context: Context) {
        val sp = SecurePrefs.get(context.applicationContext)
        currentMultiplier = sp.getFloat(keyForUser(), BONUS_MULTIPLIER_DEFAULT.toFloat()).toDouble()
        Log.d(TAG, "📦 Multiplicador carregado: x$currentMultiplier")
    }

    private fun saveMultiplier(context: Context) {
        SecurePrefs.get(context.applicationContext)
            .edit()
            .putFloat(keyForUser(), currentMultiplier.toFloat())
            .apply()
    }

    // =====================================================
    // 💰 Moedas (Encapsulamento Completo)
    // =====================================================

    fun getCoins(context: Context): Int = GameDataManager.getCoins(context)

    /**
     * Adiciona moedas.
     * ✅ Regra: só permite ganhos com motivos autorizados (anti-exploit).
     * Deve ser o ÚNICO ponto de entrada para GANHOS de moedas.
     */
    fun addCoins(context: Context, baseAmount: Int, reason: String = "padrão") {
        if (baseAmount <= 0) return

        if (!isAllowedReason(reason)) {
            Log.w(TAG, "⛔ Moedas bloqueadas. reason=$reason, base=$baseAmount")
            return
        }

        val mult = currentMultiplier.coerceAtLeast(BONUS_MULTIPLIER_DEFAULT)
        val finalAmount = floor(baseAmount * mult).toInt().coerceAtLeast(1)

        GameDataManager.addCoins(context, finalAmount)

        Log.d(TAG, "💰 +$finalAmount moedas (motivo=$reason, base=$baseAmount, mult=x$mult)")
    }

    private fun isAllowedReason(reason: String): Boolean {
        val r = reason.trim()
        if (r.isBlank()) return false

        return r.equals(AD_REWARD, ignoreCase = true) ||
            r.equals(SECRET, ignoreCase = true) ||
            r.startsWith(STREAK_PREFIX, ignoreCase = true) ||
            r.startsWith(MARCO_PREFIX, ignoreCase = true)
    }

    /**
     * Deduz moedas (ex: compras, penalidades).
     * Deve ser o ÚNICO ponto de entrada para DEDUÇÃO de moedas.
     */
    fun removeCoins(context: Context, amount: Int, reason: String = "uso") {
        if (amount <= 0) return

        val total = getCoins(context)
        val amountToRemove = amount.coerceAtMost(total)

        if (amountToRemove <= 0) return

        GameDataManager.addCoins(context, -amountToRemove)
        Log.d(TAG, "💸 -$amountToRemove moedas (motivo=$reason). Total agora=${getCoins(context)}")
    }

    /** Remove moedas se o jogador tiver saldo suficiente. Retorna true se sucesso. */
    fun spendCoins(context: Context, cost: Int): Boolean {
        if (cost <= 0) return true
        val current = getCoins(context)
        return if (current >= cost) {
            removeCoins(context, cost, reason = "Gasto")
            true
        } else false
    }

    // =====================================================
    // ⚡ Multiplicador
    // =====================================================

    fun setMultiplier(context: Context, multiplier: Double) {
        currentMultiplier = multiplier.coerceAtLeast(BONUS_MULTIPLIER_DEFAULT)
        saveMultiplier(context)
        Log.d(TAG, "⚡ Multiplicador ajustado para x$currentMultiplier")
    }

    fun resetMultiplier(context: Context) {
        currentMultiplier = BONUS_MULTIPLIER_DEFAULT
        saveMultiplier(context)
        Log.d(TAG, "🎯 Multiplicador resetado para x$currentMultiplier")
    }

    fun getMultiplier(): Double = currentMultiplier

    // =====================================================
    // 🏆 Recompensas
    // =====================================================

    /** Recompensa por streak longo (NORMAL). */
    fun rewardForStreak(context: Context, streak: Int) {
        if (streak < 10) return
        val reward = REWARD_STREAK_COINS + (streak / 5) * 2
        addCoins(context, reward, reason = "$STREAK_PREFIX $streak")
        Log.d(TAG, "🔥 Streak bônus: +$reward (streak=$streak)")
    }

    /** Recompensa por assistir anúncio de recompensa. */
    fun rewardForAd(context: Context) {
        addCoins(context, REWARD_AD_COINS, reason = AD_REWARD)
        Log.d(TAG, "🎥 Ad reward aplicado: +$REWARD_AD_COINS")
    }

    /** Verifica se o jogador atingiu novo marco de pontuação. */
    fun checkMilestoneReward(context: Context, oldScore: Int, newScore: Int) {
        if (newScore <= oldScore) return
        val oldMilestone = oldScore / SCORE_MILESTONE
        val newMilestone = newScore / SCORE_MILESTONE
        if (newMilestone <= oldMilestone) return

        val numMilestones = newMilestone - oldMilestone
        val totalReward = REWARD_SCORE_COINS * numMilestones

        addCoins(context, totalReward, reason = "$MARCO_PREFIX ${newMilestone * SCORE_MILESTONE} pts")

        Log.d(TAG, "🏆 Marco(s) atingido(s)! +$totalReward moedas (x$numMilestones)")
    }

    /** Recompensa por completar uma Fase Secreta. */
    fun rewardForSecretLevelCompletion(context: Context) {
        addCoins(context, SECRET_LEVEL_COIN_REWARD, reason = SECRET)
        GameDataManager.addXP(context, SECRET_LEVEL_XP_REWARD)
        Log.d(TAG, "⭐ Secreto concluído: +$SECRET_LEVEL_COIN_REWARD moedas, +$SECRET_LEVEL_XP_REWARD XP")
    }

    // =====================================================
    // 🧍‍♂️ Avatares (Fonte única via GameDataManager)
    // =====================================================

    fun unlockAvatar(context: Context, avatarId: Int) {
        GameDataManager.unlockAvatar(context, avatarId)
        Log.d(TAG, "🎨 Avatar desbloqueado: $avatarId")
    }

    fun isAvatarUnlocked(context: Context, avatarId: Int): Boolean =
        GameDataManager.isAvatarUnlocked(context, avatarId)

    fun tryBuyAvatar(context: Context, avatarId: Int): Boolean {
        return if (spendCoins(context, AVATAR_COST)) {
            unlockAvatar(context, avatarId)
            true
        } else false
    }
}
