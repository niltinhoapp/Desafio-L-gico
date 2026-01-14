package com.desafiolgico.utils

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.core.content.ContextCompat
import com.desafiolgico.R

object PremiumThemes {

    data class ThemePack(
        val id: String,
        val bgStart: Int,
        val bgEnd: Int,
        val cardColor: Int,
        val accent: Int
    )

    // ✅ Cores por recurso (você cria em colors.xml)
    private val packs = mapOf(
        "theme_default" to ThemePack("theme_default",
            R.color.theme_default_bg_start,
            R.color.theme_default_bg_end,
            R.color.theme_default_card,
            R.color.theme_default_accent
        ),
        "theme_neon" to ThemePack("theme_neon",
            R.color.theme_neon_bg_start,
            R.color.theme_neon_bg_end,
            R.color.theme_neon_card,
            R.color.theme_neon_accent
        ),
        "theme_royal" to ThemePack("theme_royal",
            R.color.theme_royal_bg_start,
            R.color.theme_royal_bg_end,
            R.color.theme_royal_card,
            R.color.theme_royal_accent
        ),
        "theme_shadow" to ThemePack("theme_shadow",
            R.color.theme_shadow_bg_start,
            R.color.theme_shadow_bg_end,
            R.color.theme_shadow_card,
            R.color.theme_shadow_accent
        ),
    )

    fun apply(activity: Activity, root: View, cardViews: List<View> = emptyList()) {
        val id = GameDataManager.getSelectedTheme(activity)
        val pack = packs[id] ?: packs.getValue("theme_default")

        // Background gradiente no root
        val start = ContextCompat.getColor(activity, pack.bgStart)
        val end = ContextCompat.getColor(activity, pack.bgEnd)
        val grad = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(start, end)
        )
        root.background = grad

        // Card style (se você passar as views)
        val cardColor = ContextCompat.getColor(activity, pack.cardColor)
        cardViews.forEach { v ->
            v.background = GradientDrawable().apply {
                cornerRadius = 28f
                setColor(cardColor)
            }
        }

        // Accent você usa pra tint de botões/ícones se quiser (mais abaixo eu mostro)
    }
}
