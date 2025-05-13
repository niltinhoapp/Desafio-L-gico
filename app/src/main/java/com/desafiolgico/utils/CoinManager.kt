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
    private const val AVATAR_KEY = "unlocked_avatars" // Chave de avatar movida
    const val AVATAR_COST = 150

    // Constantes da Fase Secreta
    const val SECRET_LEVEL_COIN_REWARD = 25
    const val SECRET_LEVEL_XP_REWARD = 50

    // =====================================================
    // 🧩 Estado e Persistência de Multiplicador
    // =====================================================
    private var currentMultiplier = BONUS_MULTIPLIER_DEFAULT

    // Helper para gerar a chave por usuário (usando GameDataManager)
    // NOTA: Esta função não é mais necessária se usarmos apenas o GameDataManager.
    // MANTIDA SOMENTE PARA REFERÊNCIA E EVITAR ERRO DE COMPILAÇÃO, MAS NÃO É USADA ABAIXO.
    // private fun getUserKey(context: Context, key: String): String = GameDataManager.getUserKey(key)

    // =====================================================
    // 💰 Moedas (Encapsulamento Completo)
    // =====================================================

    /** Obtém o total atual de moedas do jogador (usa GameDataManager). */
    fun getCoins(context: Context): Int = GameDataManager.getCoins(context)

    /** * Adiciona moedas. Aplica o multiplicador ativo antes de persistir.
     * Deve ser o ÚNICO ponto de entrada para GANHOS de moedas.
     */
    fun addCoins(context: Context, baseAmount: Int, reason: String = "padrão") {
        if (baseAmount <= 0) return

        // ✅ Regra: moedas só do anúncio
        if (reason != "AdReward") {
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

    /** * Deduz moedas (ex: compras, penalidades).
     * Deve ser o ÚNICO ponto de entrada para DEDUÇÃO de moedas.
     */
    fun removeCoins(context: Context, amount: Int, reason: String = "uso") {
        if (amount <= 0) return

        val total = getCoins(context)
        val amountToRemove = amount.coerceAtMost(total) // Garante que não remove mais do que tem.

        // Usa GameDataManager para persistir o valor negativo
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

    /** Define um multiplicador temporário para ganhos de moedas. */
    fun setMultiplier(multiplier: Double) {
        currentMultiplier = multiplier.coerceAtLeast(BONUS_MULTIPLIER_DEFAULT)
        Log.d("CoinManager", "⚡ Multiplicador de moedas ajustado para x$currentMultiplier")
    }

    fun resetMultiplier() {
        currentMultiplier = BONUS_MULTIPLIER_DEFAULT
        Log.d("CoinManager", "🎯 Multiplicador de moedas resetado para o padrão (x1.0)")
    }

    // =====================================================
    // 🏆 Recompensas
    // =====================================================

    /** Recompensa por streak longo. */
    fun rewardForStreak(context: Context, streak: Int) {
        if (streak >= 10) {
            val reward = REWARD_STREAK_COINS + (streak / 5) * 2
            addCoins(context, reward, reason = "Streak $streak") // Usa CoinManager.addCoins
            Log.d("CoinManager", "🔥 Bônus de streak: +$reward moedas (streak=$streak)")
        }
    }

    /** Recompensa por assistir anúncio de recompensa. */
    fun rewardForAd(context: Context) {
        addCoins(context, REWARD_AD_COINS, reason = "AdReward") // Usa CoinManager.addCoins
        Log.d("CoinManager", "🎥 Recompensa por anúncio aplicada: +$REWARD_AD_COINS moedas")
    }

    /** Verifica se o jogador atingiu um novo marco de pontuação. */
    fun checkMilestoneReward(context: Context, oldScore: Int, newScore: Int) {
        val oldMilestone = oldScore / SCORE_MILESTONE
        val newMilestone = newScore / SCORE_MILESTONE
        if (newMilestone > oldMilestone) {
            // Recompensa múltiplos marcos se o salto for grande
            val numMilestones = newMilestone - oldMilestone
            val totalReward = REWARD_SCORE_COINS * numMilestones
            addCoins(context, totalReward, reason = "Marco ${newMilestone * SCORE_MILESTONE} pts") // Usa CoinManager.addCoins
            Log.d("CoinManager", "🏆 Marco(s) atingido(s)! +$totalReward moedas.")
        }
    }

    /** * Recompensa combinada por completar uma Fase Secreta.
     * NOTA: A recompensa de moedas passa por CoinManager.addCoins para aplicar o multiplicador.
     */
    fun rewardForSecretLevelCompletion(context: Context) {
        addCoins(context, SECRET_LEVEL_COIN_REWARD, reason = "Fase Secreta") // Usa CoinManager.addCoins

        // O XP não tem multiplicador, então GameDataManager é usado diretamente.
        GameDataManager.addXP(context, SECRET_LEVEL_XP_REWARD)
        Log.d("CoinManager", "⭐ Fase Secreta concluída! Ganhou +$SECRET_LEVEL_XP_REWARD XP.")
    }

    // =====================================================
    // 🧍‍♂️ Avatares desbloqueáveis (Persistência via GameDataManager)
    // =====================================================

    // NOTA: Movido o controle de avatar para usar GameDataManager

    /** Desbloqueia o avatar indicado para o usuário atual */
    fun unlockAvatar(context: Context, avatarId: Int) {
        // Usa o GameDataManager para persistir o desbloqueio do avatar
        GameDataManager.unlockAvatar(context, avatarId)
        Log.d("CoinManager", "🎨 Avatar $avatarId desbloqueado.")
    }

    /** Verifica se o avatar foi desbloqueado pelo usuário atual */
    fun isAvatarUnlocked(context: Context, avatarId: Int): Boolean {
        return GameDataManager.isAvatarUnlocked(context, avatarId)
    }

    /** Tenta comprar o avatar (retorna true se sucesso) */
    fun tryBuyAvatar(context: Context, avatarId: Int): Boolean {
        return if (spendCoins(context, AVATAR_COST)) { // Usa CoinManager.spendCoins
            // NOTA: O desbloqueio de avatar agora deve ser gerenciado no GameDataManager
            // Assumindo que você adicionará as funções de Avatar ao GameDataManager
            // Por enquanto, vou deixá-lo usando uma função simulada no GDM
            GameDataManager.unlockAvatar(context, avatarId)
            true
        } else false
    }
}
