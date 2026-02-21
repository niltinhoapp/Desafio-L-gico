package com.desafiolgico.utils

import android.content.Context
import com.desafiolgico.R


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
        // ✅ pago NÃO destrava via conquista
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

    /**
     * ✅ Texto de status PT/EN via strings (se existirem),
     * com fallback seguro caso falte algum recurso.
     */
    fun statusText(ctx: Context): String = when {
        isPaid() -> safeString(
            ctx,
            R.string.premium_status_locked_coins,
            fallback = "🔒 Bloqueado • $priceCoins moedas",
            args = arrayOf(priceCoins)
        )

        isAchievementOnly() -> safeString(
            ctx,
            R.string.premium_status_achievement,
            fallback = "🏆 Conquista • ${requirementText(ctx)}",
            args = arrayOf(requirementText(ctx))
        )

        else -> safeString(
            ctx,
            R.string.premium_status_free,
            fallback = "🆓 Grátis"
        )
    }

    private fun safeString(
        ctx: Context,
        resId: Int,
        fallback: String,
        args: Array<Any> = emptyArray()
    ): String {
        return runCatching {
            if (args.isEmpty()) ctx.getString(resId) else ctx.getString(resId, *args)
        }.getOrElse { fallback }
    }
}
