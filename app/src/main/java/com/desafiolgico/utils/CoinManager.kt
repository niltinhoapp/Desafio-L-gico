package com.desafiolgico.utils

import android.content.Context
import android.util.Log
import kotlin.math.floor

/**
 * Gerencia a economia de moedas do jogo e o estado de avatares.
 * Centraliza todos os ganhos e deduções de moedas, garantindo o uso de multiplicadores.
 */
object CoinManager {

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
    // 🧩 Estado e Persistência de Multiplicador
    // =====================================================
    private var currentMultiplier = BONUS_MULTIPLIER_DEFAULT

    // Se quiser persistir o multiplicador entre reinícios do app:
    private const val PREFS = "coin_prefs"
    private const val KEY_MULT = "multiplier"

    /**
     * Carrega o multiplicador salvo (se existir). Chame no boot do app (Application/primeira Activity).
     */
    fun loadMultiplier(context: Context) {
        val sp = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        currentMultiplier = sp.getFloat(KEY_MULT, BONUS_MULTIPLIER_DEFAULT.toFloat()).toDouble()
        Log.d("CoinManager", "📦 Multiplicador carregado: x$currentMultiplier")
    }

    /**
     * Salva o multiplicador atual.
     */
    private fun saveMultiplier(context: Context) {
        val sp = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().putFloat(KEY_MULT, currentMultiplier.toFloat()).apply()
    }

    // =====================================================
    // 💰 Moedas (Encapsulamento Completo)
    // =====================================================

    /** Obtém o total atual de moedas do jogador (usa GameDataManager). */
    fun getCoins(context: Context): Int = GameDataManager.getCoins(context)

    /**
     * Adiciona moedas.
     * ✅ Regra: só permite ganhos com motivos autorizados (anti-exploit).
     * Deve ser o ÚNICO ponto de entrada para GANHOS de moedas.
     */
    fun addCoins(context: Context, baseAmount: Int, reason: String = "padrão") {
        if (baseAmount <= 0) return

        if (!isAllowedReason(reason)) {
            Log.w("CoinManager", "⛔ Moedas bloqueadas. reason=$reason, amount=$baseAmount")
            return
        }

        val finalAmount = floor(baseAmount * currentMultiplier).toInt()
        GameDataManager.addCoins(context, finalAmount)

        Log.d(
            "CoinManager",
            "💰 +$finalAmount moedas (motivo: $reason, base: $baseAmount, mult: x$currentMultiplier)"
        )
    }

    private fun isAllowedReason(reason: String): Boolean {
        val r = reason.trim()
        return r.equals(AD_REWARD, ignoreCase = true) ||
            r.startsWith(STREAK_PREFIX, ignoreCase = true) ||
            r.startsWith(MARCO_PREFIX, ignoreCase = true) ||
            r.equals(SECRET, ignoreCase = true)
    }

    /**
     * Deduz moedas (ex: compras, penalidades).
     * Deve ser o ÚNICO ponto de entrada para DEDUÇÃO de moedas.
     */
    fun removeCoins(context: Context, amount: Int, reason: String = "uso") {
        if (amount <= 0) return

        val total = getCoins(context)
        val amountToRemove = amount.coerceAtMost(total)

        GameDataManager.addCoins(context, -amountToRemove)
        val newTotal = getCoins(context)
        Log.d("CoinManager", "💸 $amountToRemove moedas removidas (motivo: $reason). Total agora: $newTotal")
    }

    /** Remove moedas se o jogador tiver saldo suficiente. Retorna true se sucesso. */
    fun spendCoins(context: Context, cost: Int): Boolean {
        val current = getCoins(context)
        return if (current >= cost) {
            removeCoins(context, cost, reason = "Gasto")
            true
        } else false
    }

    // =====================================================
    // ⚡ Multiplicador
    // =====================================================

    /**
     * Define um multiplicador (e persiste).
     * Se você NÃO quiser persistir, é só remover o `saveMultiplier(context)`.
     */
    fun setMultiplier(context: Context, multiplier: Double) {
        currentMultiplier = multiplier.coerceAtLeast(BONUS_MULTIPLIER_DEFAULT)
        saveMultiplier(context)
        Log.d("CoinManager", "⚡ Multiplicador de moedas ajustado para x$currentMultiplier")
    }

    fun resetMultiplier(context: Context) {
        currentMultiplier = BONUS_MULTIPLIER_DEFAULT
        saveMultiplier(context)
        Log.d("CoinManager", "🎯 Multiplicador de moedas resetado para o padrão (x1.0)")
    }

    // =====================================================
    // 🏆 Recompensas
    // =====================================================

    /** Recompensa por streak longo. */
    fun rewardForStreak(context: Context, streak: Int) {
        if (streak >= 10) {
            val reward = REWARD_STREAK_COINS + (streak / 5) * 2
            addCoins(context, reward, reason = "$STREAK_PREFIX $streak")
            Log.d("CoinManager", "🔥 Bônus de streak: +$reward moedas (streak=$streak)")
        }
    }

    /** Recompensa por assistir anúncio de recompensa. */
    fun rewardForAd(context: Context) {
        addCoins(context, REWARD_AD_COINS, reason = AD_REWARD)
        Log.d("CoinManager", "🎥 Recompensa por anúncio aplicada: +$REWARD_AD_COINS moedas")
    }

    /** Verifica se o jogador atingiu um novo marco de pontuação. */
    fun checkMilestoneReward(context: Context, oldScore: Int, newScore: Int) {
        val oldMilestone = oldScore / SCORE_MILESTONE
        val newMilestone = newScore / SCORE_MILESTONE
        if (newMilestone > oldMilestone) {
            val numMilestones = newMilestone - oldMilestone
            val totalReward = REWARD_SCORE_COINS * numMilestones
            addCoins(context, totalReward, reason = "$MARCO_PREFIX ${newMilestone * SCORE_MILESTONE} pts")
            Log.d("CoinManager", "🏆 Marco(s) atingido(s)! +$totalReward moedas.")
        }
    }

    /** Recompensa por completar uma Fase Secreta. */
    fun rewardForSecretLevelCompletion(context: Context) {
        addCoins(context, SECRET_LEVEL_COIN_REWARD, reason = SECRET)
        GameDataManager.addXP(context, SECRET_LEVEL_XP_REWARD)
        Log.d("CoinManager", "⭐ Fase Secreta concluída! +$SECRET_LEVEL_COIN_REWARD moedas e +$SECRET_LEVEL_XP_REWARD XP.")
    }

    // =====================================================
    // 🧍‍♂️ Avatares desbloqueáveis (Persistência via GameDataManager)
    // =====================================================

    fun unlockAvatar(context: Context, avatarId: Int) {
        GameDataManager.unlockAvatar(context, avatarId)
        Log.d("CoinManager", "🎨 Avatar $avatarId desbloqueado.")
    }

    fun isAvatarUnlocked(context: Context, avatarId: Int): Boolean {
        return GameDataManager.isAvatarUnlocked(context, avatarId)
    }

    fun tryBuyAvatar(context: Context, avatarId: Int): Boolean {
        return if (spendCoins(context, AVATAR_COST)) {
            GameDataManager.unlockAvatar(context, avatarId)
            true
        } else false
    }
}
