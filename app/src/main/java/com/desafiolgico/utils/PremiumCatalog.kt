package com.desafiolgico.utils

import android.content.Context

enum class PremiumType { THEME, FRAME, TITLE, PET, VFX }


data class PremiumItem(
    val id: String,
    val type: PremiumType,
    val name: String,
    val priceCoins: Int = 0,          // 0 = grátis OU só conquista
    val minDailyStreak: Int = 0,      // conquista
    val minHighestStreak: Int = 0,    // conquista
    val desc: String = ""
) {

    // ----------- HELPERS (pra UI ficar simples) -----------

    /** Item comprado com moedas */
    fun isPaid(): Boolean = priceCoins > 0

    /** Item grátis de verdade (sem preço e sem requisitos) */
    fun isFree(): Boolean = priceCoins <= 0 && minDailyStreak <= 0 && minHighestStreak <= 0

    /** Item só por conquista (sem preço, mas tem requisito) */
    fun isAchievementOnly(): Boolean = priceCoins <= 0 && (minDailyStreak > 0 || minHighestStreak > 0)

    /** Já atingiu os requisitos de conquista? */
    fun canUnlockByAchievement(ctx: Context): Boolean {
        val daily = GameDataManager.getDailyStreak(ctx)
        val best = GameDataManager.getHighestStreak(ctx)
        return daily >= minDailyStreak && best >= minHighestStreak
    }

    /** Texto do requisito (ex: "Daily 7 • Streak 40") */
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
     * Texto de status padrão (pra você usar no card):
     * - "Grátis"
     * - "Bloqueado • 800 moedas"
     * - "Conquista • Daily 7 (atual 3)"
     */
    fun statusText(ctx: Context): String {
        return when {
            isPaid() -> "🔒 Bloqueado • $priceCoins moedas"
            isAchievementOnly() -> "🏆 Conquista • ${requirementText(ctx)}"
            else -> "🆓 Grátis"
        }
    }
}


object PremiumCatalog {

    // TEMAS
    val themes = listOf(
        PremiumItem("theme_default", PremiumType.THEME, "Tema Padrão", priceCoins = 0, desc = "Tema original"),
        PremiumItem("theme_neon", PremiumType.THEME, "Tema Neon", priceCoins = 800, desc = "Brilho cyber premium"),
        PremiumItem("theme_royal", PremiumType.THEME, "Tema Royal", priceCoins = 1200, desc = "Luxo roxo + dourado"),
        PremiumItem("theme_shadow", PremiumType.THEME, "Tema Shadow", minHighestStreak = 40, desc = "Só por conquista (streak 40)")
    )

    // MOLDURAS
    val frames = listOf(
        PremiumItem("frame_none", PremiumType.FRAME, "Sem moldura"),
        PremiumItem("frame_bronze", PremiumType.FRAME, "Moldura Bronze", priceCoins = 300),
        PremiumItem("frame_silver", PremiumType.FRAME, "Moldura Prata", priceCoins = 600),
        PremiumItem("frame_gold", PremiumType.FRAME, "Moldura Ouro", priceCoins = 900),
        PremiumItem("frame_neon", PremiumType.FRAME, "Moldura Neon", minDailyStreak = 7, desc = "7 dias no Daily"),
        PremiumItem("frame_diamond", PremiumType.FRAME, "Moldura Diamante", minHighestStreak = 60, desc = "Streak 60")
    )

    // TÍTULOS
    val titles = listOf(
        PremiumItem("title_none", PremiumType.TITLE, "Sem título"),
        PremiumItem("title_aprendiz", PremiumType.TITLE, "Aprendiz", priceCoins = 150),
        PremiumItem("title_estrategista", PremiumType.TITLE, "Estrategista", priceCoins = 350),
        PremiumItem("title_mestre", PremiumType.TITLE, "Mestre do Desafio", minHighestStreak = 30),
        PremiumItem("title_rei_daily", PremiumType.TITLE, "Rei do Daily", minDailyStreak = 10),
        PremiumItem("title_lenda", PremiumType.TITLE, "Lenda", minHighestStreak = 80)
    )

    // PETS (lvl 1..3 via upgrade)
    val pets = listOf(
        PremiumItem("pet_none", PremiumType.PET, "Sem pet"),
        PremiumItem("pet_owl", PremiumType.PET, "Corujinha Sábia", priceCoins = 700),
        PremiumItem("pet_bot", PremiumType.PET, "Robô Lógico", priceCoins = 900),
        PremiumItem("pet_dragon", PremiumType.PET, "Mini Dragão", minHighestStreak = 50)
    )

    // VFX (efeito de vitória)
    val vfx = listOf(
        PremiumItem("vfx_basic", PremiumType.VFX, "Vitória Básica"),
        PremiumItem("vfx_gold", PremiumType.VFX, "Konfetti Ouro", priceCoins = 400),
        PremiumItem("vfx_neon", PremiumType.VFX, "Neon Burst", priceCoins = 600),
        PremiumItem("vfx_fire", PremiumType.VFX, "Fogo Lendário", minHighestStreak = 70)
    )

    fun all(): List<PremiumItem> = themes + frames + titles + pets + vfx

    fun find(id: String): PremiumItem? = all().firstOrNull { it.id == id }
}
