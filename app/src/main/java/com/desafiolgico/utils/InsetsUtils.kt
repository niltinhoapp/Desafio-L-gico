package com.desafiolgico.utils

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Aplica padding automático de system bars (status + navigation).
 * Use no root da tela (o container principal).
 */
fun View.applySystemBarsPadding(
    applyTop: Boolean = true,
    applyBottom: Boolean = true,
    applyLeft: Boolean = true,
    applyRight: Boolean = true
) {
    // Guarda o padding original (do XML)
    val startPaddingLeft = paddingLeft
    val startPaddingTop = paddingTop
    val startPaddingRight = paddingRight
    val startPaddingBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        v.updatePadding(
            left = startPaddingLeft + if (applyLeft) bars.left else 0,
            top = startPaddingTop + if (applyTop) bars.top else 0,
            right = startPaddingRight + if (applyRight) bars.right else 0,
            bottom = startPaddingBottom + if (applyBottom) bars.bottom else 0
        )
        insets
    }

    // força aplicar agora
    requestApplyInsetsWhenAttached()
}

private fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        requestApplyInsets()
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                v.requestApplyInsets()
            }
            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}
