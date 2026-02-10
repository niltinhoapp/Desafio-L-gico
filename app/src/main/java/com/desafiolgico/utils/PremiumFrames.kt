package com.desafiolgico.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.desafiolgico.R

object PremiumFrames {

    // Ajuste fino aqui:
    private const val STROKE_DP = 6
    private const val PADDING_EXTRA_DP = 2

    fun applyFrame(context: Context, avatarView: ImageView) {
        val id = GameDataManager.getSelectedFrame(context)

        val strokePx = context.dp(STROKE_DP)
        val padPx = strokePx + context.dp(PADDING_EXTRA_DP)

        val drawable: Drawable? = when (id) {
            "frame_bronze" -> ringSolid(context, R.color.frame_bronze, strokePx)
            "frame_silver" -> ringSolid(context, R.color.frame_silver, strokePx)
            "frame_gold" -> ringSolid(context, R.color.frame_gold, strokePx)
            "frame_diamond" -> ringSolid(context, R.color.frame_diamond, strokePx)
            "frame_neon" -> ringNeon(context, strokePx)
            else -> null
        }

        avatarView.background = drawable

        // ✅ Se tiver moldura: dá espaço pra aparecer. Se não: zera.
        if (drawable != null) {
            avatarView.setPadding(padPx, padPx, padPx, padPx)
        } else {
            avatarView.setPadding(0, 0, 0, 0)
        }

        // ✅ Recorte pelo background oval (ajuda a ficar redondinho)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            avatarView.outlineProvider = ViewOutlineProvider.BACKGROUND
            avatarView.clipToOutline = drawable != null
        }
    }

    private fun ringSolid(context: Context, colorRes: Int, strokePx: Int): GradientDrawable {
        val c = ContextCompat.getColor(context, colorRes)
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setStroke(strokePx, c)
            setColor(0x00000000) // transparente
        }
    }

    /**
     * ✅ Neon com gradiente de verdade usando LayerDrawable:
     * - Camada 0: oval gradiente preenchido
     * - Camada 1: oval transparente insetado (abre o "buraco" e vira anel)
     */
    private fun ringNeon(context: Context, strokePx: Int): Drawable {
        val c1 = ContextCompat.getColor(context, R.color.frame_neon_1)
        val c2 = ContextCompat.getColor(context, R.color.frame_neon_2)

        val outer = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(c1, c2)
        ).apply {
            shape = GradientDrawable.OVAL
        }

        val inner = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0x00000000) // transparente
        }

        return LayerDrawable(arrayOf(outer, inner)).apply {
            // inset da camada de dentro = espessura do anel
            setLayerInset(1, strokePx, strokePx, strokePx, strokePx)
        }
    }
}
