package com.desafiolgico.main.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityTestBinding
import com.google.android.material.card.MaterialCardView
import kotlin.math.roundToInt

class FxController(
    private val activity: AppCompatActivity,
    private val binding: ActivityTestBinding,
    private val rootLayout: ViewGroup,
    private val dp: (Int) -> Int
) {

    private var fxOverlay: View? = null

    private var defaultQuestionStrokeColor: Int? = null
    private var defaultQuestionStrokeWidth: Int? = null

    private var glowAnimator: ValueAnimator? = null

    /** chame 1x no onCreate (depois do setContentView) */
    fun init() {
        ensureFxOverlay()
        cacheQuestionCardDefaults()
    }

    fun ensureFxOverlay() {
        if (fxOverlay != null) return

        fxOverlay = View(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            alpha = 0f
            visibility = View.GONE

            isClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        rootLayout.addView(fxOverlay)
        fxOverlay?.bringToFront()
    }

    fun cacheQuestionCardDefaults() {
        val card = binding.questionCard
        if (defaultQuestionStrokeColor == null) defaultQuestionStrokeColor = card.strokeColor
        if (defaultQuestionStrokeWidth == null) defaultQuestionStrokeWidth = card.strokeWidth
    }

    fun flashFx(success: Boolean) {
        val v = fxOverlay ?: return
        v.bringToFront()

        val baseColor = if (success) {
            // verde do seu theme (ou fallback)
            runCatching { ContextCompat.getColor(activity, R.color.correctAnswerColor) }
                .getOrElse { 0xFF4CAF50.toInt() }
        } else {
            runCatching { ContextCompat.getColor(activity, R.color.wrongAnswerColor) }
                .getOrElse { 0xFFF44336.toInt() }
        }

        // aplica alpha suave por argb (0x22 ~ 13%)
        val overlayColor = Color.argb(
            0x22,
            Color.red(baseColor),
            Color.green(baseColor),
            Color.blue(baseColor)
        )

        v.setBackgroundColor(overlayColor)
        v.animate().cancel()
        v.visibility = View.VISIBLE
        v.alpha = 0f

        v.animate()
            .alpha(1f)
            .setDuration(70)
            .withEndAction {
                v.animate()
                    .alpha(0f)
                    .setDuration(160)
                    .withEndAction { v.visibility = View.GONE }
                    .start()
            }
            .start()
    }

    fun animateQuestionIn() {
        binding.questionCard.animate().cancel()
        binding.questionCard.alpha = 0f
        binding.questionCard.translationY = 10f

        binding.questionCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220)
            .setInterpolator(OvershootInterpolator(0.7f))
            .start()
    }

    fun animateQuestionSwap(newText: String) {
        binding.questionCard.animate().cancel()

        binding.questionCard.animate()
            .alpha(0f)
            .translationY(-8f)
            .setDuration(130)
            .withEndAction {
                binding.questionTextView.text = newText
                binding.questionCard.translationY = 10f

                binding.questionCard.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(200)
                    .setInterpolator(OvershootInterpolator(0.6f))
                    .start()
            }
            .start()
    }

    fun staggerOptions(optionButtons: List<View>) {
        optionButtons.forEachIndexed { i, b ->
            if (b.visibility != View.VISIBLE) return@forEachIndexed
            b.animate().cancel()
            b.alpha = 0f
            b.translationY = 10f

            b.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(i * 55L)
                .setDuration(170)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    fun pop(v: View) {
        v.animate().cancel()
        v.scaleX = 1f
        v.scaleY = 1f

        v.animate()
            .scaleX(0.97f).scaleY(0.97f)
            .setDuration(70)
            .withEndAction {
                v.animate().scaleX(1f).scaleY(1f).setDuration(110).start()
            }
            .start()
    }

    fun shake(v: View) {
        v.animate().cancel()
        val d = 8f
        v.translationX = 0f

        v.animate().translationX(d).setDuration(35).withEndAction {
            v.animate().translationX(-d).setDuration(35).withEndAction {
                v.animate().translationX(d * 0.6f).setDuration(35).withEndAction {
                    v.animate().translationX(0f).setDuration(55).start()
                }.start()
            }.start()
        }.start()
    }

    fun glowQuestionCard(success: Boolean) {
        val card: MaterialCardView = binding.questionCard

        val ok = runCatching { ContextCompat.getColor(activity, R.color.correctAnswerColor) }
            .getOrElse { 0xFF4CAF50.toInt() }
        val no = runCatching { ContextCompat.getColor(activity, R.color.wrongAnswerColor) }
            .getOrElse { 0xFFF44336.toInt() }

        val target = if (success) ok else no

        val baseColor = defaultQuestionStrokeColor ?: card.strokeColor
        val baseWidth = defaultQuestionStrokeWidth ?: card.strokeWidth

        // evita empilhar anim
        glowAnimator?.cancel()
        card.strokeWidth = dp(2)

        glowAnimator = ValueAnimator.ofInt(40, 200, 40).apply {
            duration = 320
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { va ->
                val a = va.animatedValue as Int
                card.strokeColor = Color.argb(
                    a,
                    Color.red(target),
                    Color.green(target),
                    Color.blue(target)
                )
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    card.strokeColor = baseColor
                    card.strokeWidth = baseWidth
                }

                override fun onAnimationCancel(animation: Animator) {
                    card.strokeColor = baseColor
                    card.strokeWidth = baseWidth
                }
            })
        }

        glowAnimator?.start()
    }

    fun tinyTickUi(onVibrate: (Long) -> Unit) {
        binding.timerProgressBar.animate().cancel()
        binding.timerProgressBar.scaleX = 1f
        binding.timerProgressBar.animate()
            .scaleX(1.02f)
            .setDuration(80)
            .withEndAction {
                binding.timerProgressBar.animate().scaleX(1f).setDuration(110).start()
            }
            .start()

        binding.timerTextView.animate().cancel()
        binding.timerTextView.alpha = 1f
        binding.timerTextView.animate()
            .alpha(0.6f)
            .setDuration(80)
            .withEndAction {
                binding.timerTextView.animate().alpha(1f).setDuration(120).start()
            }
            .start()

        onVibrate(35)
    }

    fun release(removeOverlay: Boolean = false) {
        glowAnimator?.cancel()
        glowAnimator = null

        fxOverlay?.animate()?.cancel()
        binding.questionCard.animate().cancel()
        binding.timerProgressBar.animate().cancel()
        binding.timerTextView.animate().cancel()

        if (removeOverlay) {
            fxOverlay?.let { rootLayout.removeView(it) }
            fxOverlay = null
        }
    }

    /** util (se quiser ignorar o dp injetado) */
    fun dpLocal(value: Int): Int =
        (value * activity.resources.displayMetrics.density).roundToInt()
}
