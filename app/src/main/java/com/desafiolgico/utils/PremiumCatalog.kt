package com.desafiolgico.utils

object PremiumCatalog {

    val themes = listOf(
        PremiumItem("theme_default", PremiumType.THEME, "Tema Padrão", priceCoins = 0, desc = "Tema original"),
        PremiumItem("theme_neon", PremiumType.THEME, "Tema Neon", priceCoins = 800, desc = "Brilho cyber premium"),
        PremiumItem("theme_royal", PremiumType.THEME, "Tema Royal", priceCoins = 1200, desc = "Luxo roxo + dourado"),
        PremiumItem("theme_shadow", PremiumType.THEME, "Tema Shadow", minHighestStreak = 40, desc = "Só por conquista (streak 40)")
    )

    val frames = listOf(
        PremiumItem("frame_none", PremiumType.FRAME, "Sem moldura"),
        PremiumItem("frame_bronze", PremiumType.FRAME, "Moldura Bronze", priceCoins = 300),
        PremiumItem("frame_silver", PremiumType.FRAME, "Moldura Prata", priceCoins = 600),
        PremiumItem("frame_gold", PremiumType.FRAME, "Moldura Ouro", priceCoins = 900),
        PremiumItem("frame_neon", PremiumType.FRAME, "Moldura Neon", minDailyStreak = 7, desc = "7 dias no Daily"),
        PremiumItem("frame_diamond", PremiumType.FRAME, "Moldura Diamante", minHighestStreak = 60, desc = "Streak 60")
    )

    val titles = listOf(
        PremiumItem("title_none", PremiumType.TITLE, "Sem título"),
        PremiumItem("title_aprendiz", PremiumType.TITLE, "Aprendiz", priceCoins = 150),
        PremiumItem("title_estrategista", PremiumType.TITLE, "Estrategista", priceCoins = 350),
        PremiumItem("title_mestre", PremiumType.TITLE, "Mestre do Desafio", minHighestStreak = 30),
        PremiumItem("title_rei_daily", PremiumType.TITLE, "Rei do Daily", minDailyStreak = 10),
        PremiumItem("title_lenda", PremiumType.TITLE, "Lenda", minHighestStreak = 80)
    )

    val pets = listOf(
        PremiumItem("pet_none", PremiumType.PET, "Sem pet"),
        PremiumItem("pet_owl", PremiumType.PET, "Corujinha Sábia", priceCoins = 700),
        PremiumItem("pet_bot", PremiumType.PET, "Robô Lógico", priceCoins = 900),
        PremiumItem("pet_dragon", PremiumType.PET, "Mini Dragão", minHighestStreak = 50)
    )

    val vfx = listOf(
        PremiumItem("vfx_basic", PremiumType.VFX, "Vitória Básica"),
        PremiumItem("vfx_gold", PremiumType.VFX, "Konfetti Ouro", priceCoins = 400),
        PremiumItem("vfx_neon", PremiumType.VFX, "Neon Burst", priceCoins = 600),
        PremiumItem("vfx_fire", PremiumType.VFX, "Fogo Lendário", minHighestStreak = 70)
    )

    fun all(): List<PremiumItem> = themes + frames + titles + pets + vfx

    fun find(id: String): PremiumItem? = all().firstOrNull { it.id == id }
}
