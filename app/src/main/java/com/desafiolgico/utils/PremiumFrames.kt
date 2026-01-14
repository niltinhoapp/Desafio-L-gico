package com.desafiolgico.utils

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.desafiolgico.R

object PremiumFrames {

    fun applyFrame(context: Context, avatarView: ImageView) {
        val id = GameDataManager.getSelectedFrame(context)

        val drawable = when (id) {
            "frame_bronze" -> ring(context, R.color.frame_bronze)
            "frame_silver" -> ring(context, R.color.frame_silver)
            "frame_gold" -> ring(context, R.color.frame_gold)
            "frame_neon" -> neonRing(context)
            "frame_diamond" -> ring(context, R.color.frame_diamond)
            else -> null
        }

        avatarView.background = drawable
        avatarView.clipToOutline = true // se seu view tiver outline
        avatarView.setPadding(10, 10, 10, 10) // dá espaço pra moldura aparecer
    }

    private fun ring(context: Context, colorRes: Int): GradientDrawable {
        val c = ContextCompat.getColor(context, colorRes)
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setStroke(10, c)
            setColor(0x00000000)
        }
    }

    private fun neonRing(context: Context): GradientDrawable {
        val c1 = ContextCompat.getColor(context, R.color.frame_neon_1)
        val c2 = ContextCompat.getColor(context, R.color.frame_neon_2)
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(c1, c2)
        ).apply {
            shape = GradientDrawable.OVAL
            setStroke(10, c1)
            setColor(0x00000000)
        }
    }
}
