package com.desafiolgico.utils

import android.content.Context

enum class PremiumType { THEME, FRAME, TITLE, PET, VFX }

data class PremiumItem(
    val id: String,
    val type: PremiumType,
    val name: String,
    val priceCoins: Int = 0,
    val minDailyStreak: Int = 0,
    val minHighestStreak: Int = 0,
    val desc: String = ""
) {
    fun isPaid(): Boolean = priceCoins > 0
    fun isFree(): Boolean = priceCoins <= 0 && minDailyStreak <= 0 && minHighestStreak <= 0
    fun isAchievementOnly(): Boolean = priceCoins <= 0 && (minDailyStreak > 0 || minHighestStreak > 0)

    fun canUnlockByAchievement(ctx: Context): Boolean {
        // ✅ FIX CRÍTICO: item pago não pode destravar por conquista "de graça"
        if (isPaid()) return false

        val daily = GameDataManager.getDailyStreak(ctx)
        val best = GameDataManager.getHighestStreak(ctx)
        return daily >= minDailyStreak && best >= minHighestStreak
    }

    fun requirementText(ctx: Context? = null): String {
        val parts = mutableListOf<String>()
        if (minDailyStreak > 0) {
            val cur = ctx?.let { GameDataManager.getDailyStreak(it) }
            parts += if (cur != null) "Daily $minDailyStreak (atual $cur)" else "Daily $minDailyStreak"
        }
        if (minHighestStreak > 0) {
            val best = ctx?.let { GameDataManager.getHighestStreak(it) }
            parts += if (best != null) "Streak $minHighestStreak (melhor $best)" else "Streak $minHighestStreak"
        }
        return if (parts.isEmpty()) "Sem requisito" else parts.joinToString(" • ")
    }

    fun statusText(ctx: Context): String = when {
        isPaid() -> "🔒 Bloqueado • $priceCoins moedas"
        isAchievementOnly() -> "🏆 Conquista • ${requirementText(ctx)}"
        else -> "🆓 Grátis"
    }
}
