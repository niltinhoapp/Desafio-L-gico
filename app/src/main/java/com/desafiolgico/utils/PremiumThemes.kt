package com.desafiolgico.utils

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.core.content.ContextCompat
import com.desafiolgico.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

object PremiumThemes {

    data class ThemePack(
        val id: String,
        val bgStart: Int,
        val bgEnd: Int,
        val cardColor: Int,
        val accent: Int
    )

    private val packs = mapOf(
        "theme_default" to ThemePack(
            "theme_default",
            R.color.theme_default_bg_start,
            R.color.theme_default_bg_end,
            R.color.theme_default_card,
            R.color.theme_default_accent
        ),
        "theme_neon" to ThemePack(
            "theme_neon",
            R.color.theme_neon_bg_start,
            R.color.theme_neon_bg_end,
            R.color.theme_neon_card,
            R.color.theme_neon_accent
        ),
        "theme_royal" to ThemePack(
            "theme_royal",
            R.color.theme_royal_bg_start,
            R.color.theme_royal_bg_end,
            R.color.theme_royal_card,
            R.color.theme_royal_accent
        ),
        "theme_shadow" to ThemePack(
            "theme_shadow",
            R.color.theme_shadow_bg_start,
            R.color.theme_shadow_bg_end,
            R.color.theme_shadow_card,
            R.color.theme_shadow_accent
        ),
    )

    fun apply(
        activity: Activity,
        root: View,
        cardViews: List<View> = emptyList(),
        accentButtons: List<MaterialButton> = emptyList(),
    ) {
        val id = GameDataManager.getSelectedTheme(activity)
        val pack = packs[id] ?: packs.getValue("theme_default")

        // ✅ Background gradiente no root
        val start = ContextCompat.getColor(activity, pack.bgStart)
        val end = ContextCompat.getColor(activity, pack.bgEnd)

        root.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(start, end)
        )

        // ✅ Cards (preserva MaterialCardView)
        val cardColor = ContextCompat.getColor(activity, pack.cardColor)
        val radiusPx = activity.dp(18).toFloat()

        cardViews.forEach { v ->
            when (v) {
                is MaterialCardView -> {
                    v.setCardBackgroundColor(cardColor)
                    v.radius = radiusPx
                    // se você usa stroke, pode manter aqui também:
                    // v.strokeWidth = activity.dp(1)
                    // v.strokeColor = ContextCompat.getColor(activity, R.color.some_stroke)
                }
                else -> {
                    v.background = GradientDrawable().apply {
                        cornerRadius = radiusPx
                        setColor(cardColor)
                    }
                }
            }
        }

        // ✅ Accent (opcional) – tint em botões
        val accent = ContextCompat.getColor(activity, pack.accent)
        accentButtons.forEach { btn ->
            btn.backgroundTintList = ColorStateList.valueOf(accent)
        }
    }

    fun getAccentColor(activity: Activity): Int {
        val id = GameDataManager.getSelectedTheme(activity)
        val pack = packs[id] ?: packs.getValue("theme_default")
        return ContextCompat.getColor(activity, pack.accent)
    }
}
