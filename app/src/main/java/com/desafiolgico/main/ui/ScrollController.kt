package com.desafiolgico.main.ui

import android.graphics.Rect
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.desafiolgico.databinding.ActivityTestBinding
import kotlin.math.roundToInt

class ScrollController(
    activity1: NestedScrollView,
    private val activity: AppCompatActivity,
    private val binding: ActivityTestBinding
) {
    private var lastBottomPad = -1

    fun setupScrollBottomInsetFix() {
        binding.gameElementsScrollView.post { applyScrollBottomPadding(false) }
        binding.adContainer.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            applyScrollBottomPadding(false)
        }
    }

    fun applyScrollBottomPadding(includeBannerAsComfort: Boolean = false) {
        val scroll = binding.gameElementsScrollView
        val extra = dp(16)
        val bannerComfort = if (includeBannerAsComfort) binding.adContainer.height else 0
        val bottom = (extra + bannerComfort).coerceAtLeast(dp(12))
        if (bottom == lastBottomPad) return
        lastBottomPad = bottom

        scroll.setPadding(
            scroll.paddingLeft,
            scroll.paddingTop,
            scroll.paddingRight,
            bottom
        )
        scroll.clipToPadding = false
    }

    fun ensureLastOptionVisible(lastVisible: View, extraBottomDp: Int = 16) {
        val scroll = binding.gameElementsScrollView
        scroll.post {
            val rect = Rect()
            scroll.offsetDescendantRectToMyCoords(lastVisible, rect)

            val viewportBottom = scroll.scrollY + scroll.height - scroll.paddingBottom
            val childBottom = rect.bottom

            val isCut = childBottom > (viewportBottom - dp(6))
            if (!isCut) return@post

            val viewportHeight = scroll.height - scroll.paddingTop - scroll.paddingBottom
            val targetY = (childBottom - viewportHeight + dp(extraBottomDp)).coerceAtLeast(0)
            scroll.smoothScrollTo(0, targetY)
        }
    }

    private fun dp(value: Int): Int =
        (value * activity.resources.displayMetrics.density).roundToInt()
}
