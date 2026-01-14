package com.desafiolgico.utils

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.desafiolgico.R

object PremiumUi {

    fun applyThemeToRoot(root: View, context: Context) {
        when (GameDataManager.getSelectedTheme(context)) {
            "theme_neon"   -> root.setBackgroundResource(R.drawable.bg_theme_neon)
            "theme_royal"  -> root.setBackgroundResource(R.drawable.bg_theme_royal)
            "theme_shadow" -> root.setBackgroundResource(R.drawable.bg_theme_shadow)
            else -> { /* theme_default: mantém o layout original */ }
        }
    }

    fun applyFrameToAvatar(avatar: ImageView, context: Context) {
        val frameId = GameDataManager.getSelectedFrame(context)
        val res = when (frameId) {
            "frame_bronze"  -> R.drawable.frame_bronze
            "frame_silver"  -> R.drawable.frame_silver
            "frame_gold"    -> R.drawable.frame_gold
            "frame_neon"    -> R.drawable.frame_neon
            "frame_diamond" -> R.drawable.frame_diamond
            else -> 0 // frame_none
        }

        if (res == 0) {
            avatar.background = null
            avatar.setPadding(0, 0, 0, 0)
        } else {
            avatar.setBackgroundResource(res)
            val p = dp(context, 6)
            avatar.setPadding(p, p, p, p)
        }
    }

    fun applyTitleToUsername(tv: TextView, context: Context, baseName: String) {
        val titleId = GameDataManager.getSelectedTitle(context)
        val title = when (titleId) {
            "title_aprendiz" -> "Aprendiz"
            "title_mestre"   -> "Mestre Lógico"
            "title_lenda"    -> "Lenda do Desafio"
            else -> null
        }
        tv.text = if (title != null) "$baseName\n$title" else baseName
    }

    private fun dp(context: Context, v: Int): Int {
        val d = context.resources.displayMetrics.density
        return (v * d).toInt()
    }
}
