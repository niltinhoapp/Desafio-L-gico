package com.desafiolgico.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.desafiolgico.R

object PremiumUi {

    // Tag p/ guardar o background original do root e restaurar no theme_default
    private const val TAG_ORIGINAL_BG = 0x51A7B001

    fun applyThemeToRoot(root: View, context: Context) {
        // salva o bg original só na primeira vez
        if (root.getTag(TAG_ORIGINAL_BG) == null) {
            val cs = root.background?.constantState
            root.setTag(TAG_ORIGINAL_BG, cs) // pode ser null
        }

        when (GameDataManager.getSelectedTheme(context)) {
            "theme_neon"   -> root.setBackgroundResource(R.drawable.bg_theme_neon)
            "theme_royal"  -> root.setBackgroundResource(R.drawable.bg_theme_royal)
            "theme_shadow" -> root.setBackgroundResource(R.drawable.bg_theme_shadow)
            else -> {
                // ✅ volta pro background original
                val cs = root.getTag(TAG_ORIGINAL_BG) as? Drawable.ConstantState
                root.background = cs?.newDrawable()?.mutate()
                // se era null originalmente, fica null mesmo
            }
        }
    }

    fun applyFrameToAvatar(avatar: ImageView, context: Context) {
        // ✅ usa sua versão “pro” de molduras
        PremiumFrames.applyFrame(context, avatar)
    }

    fun applyTitleToUsername(tv: TextView, context: Context, baseName: String) {
        val titleId = GameDataManager.getSelectedTitle(context)

        val title: String? = when (titleId) {
            "title_aprendiz"     -> context.getString(R.string.premium_title_aprendiz)
            "title_estrategista" -> context.getString(R.string.premium_title_estrategista)
            "title_mestre"       -> context.getString(R.string.premium_title_mestre)
            "title_rei_daily"    -> context.getString(R.string.premium_title_rei_daily)
            "title_lenda"        -> context.getString(R.string.premium_title_lenda)
            else -> null // title_none / desconhecido
        }

        tv.text = if (!title.isNullOrBlank()) "$baseName\n$title" else baseName
    }
}
