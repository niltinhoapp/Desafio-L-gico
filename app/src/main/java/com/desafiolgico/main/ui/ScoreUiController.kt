package com.desafiolgico.main.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityTestBinding
import com.desafiolgico.utils.ScoreManager
import kotlin.math.abs

class ScoreUiController(private val activity: AppCompatActivity) {

    private lateinit var binding: ActivityTestBinding

    private var scoreAnimator: ValueAnimator? = null
    private var streakAnimator: ValueAnimator? = null

    var lastScoreUi: Int = 0
        private set

    var lastStreakUi: Int = 0
        private set

    // ✅ construtor compatível
    constructor(activity: AppCompatActivity, binding: ActivityTestBinding) : this(activity) {
        bind(binding)
    }

    /** Obrigatório antes de usar (se não veio pelo construtor com binding) */
    fun bind(binding: ActivityTestBinding) {
        this.binding = binding
    }

    /** Inicializa o HUD no XML */
    fun init(score: Int, streak: Int) {
        ensureBound()
        lastScoreUi = score
        lastStreakUi = streak
        renderScore(score)
        renderStreak(streak)
    }

    /**
     * Observa o ScoreManager (se você ainda usa LiveData nele).
     * - Se estiver em modo secreto, esconde streak.
     */
    fun startObserving(
        owner: LifecycleOwner,
        scoreManager: ScoreManager,
        isSecretNow: () -> Boolean = { false },
        onScoreChanged: ((Int) -> Unit)? = null,
        onStreakChanged: ((Int) -> Unit)? = null
    ) {
        ensureBound()

        // estado inicial
        val startScore = scoreManager.overallScoreLive.value ?: scoreManager.getOverallScore()
        val startStreak = scoreManager.currentStreakLive.value ?: 0
        init(startScore, startStreak)

        scoreManager.overallScoreLive.observe(owner) { total ->
            val old = lastScoreUi
            lastScoreUi = total
            animateScoreChange(old, total)
            onScoreChanged?.invoke(total)
        }

        scoreManager.currentStreakLive.observe(owner) { streak ->
            val secret = isSecretNow()
            binding.streakTextView.visibility = if (secret) View.GONE else View.VISIBLE

            val old = lastStreakUi
            lastStreakUi = streak

            if (!secret) {
                animateStreakChange(old, streak)
                onStreakChanged?.invoke(streak)
            }
        }
    }

    fun animateScoreChange(oldScore: Int, newScore: Int) {
        ensureBound()
        val tv = binding.scoreTextView

        if (oldScore == newScore) {
            renderScore(newScore)
            return
        }

        val delta = newScore - oldScore
        val diff = abs(delta)
        val duration = (240 + diff * 8).coerceIn(320, 820).toLong()

        scoreAnimator?.cancel()
        tv.animate().cancel()
        tv.translationX = 0f
        tv.scaleX = 1f
        tv.scaleY = 1f

        scoreAnimator = ValueAnimator.ofInt(oldScore, newScore).apply {
            this.duration = duration
            addUpdateListener { anim ->
                val v = anim.animatedValue as Int
                tv.text = activity.getString(R.string.pontos_format, v)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    if (delta > 0) {
                        tv.animate().scaleX(1.08f).scaleY(1.08f).setDuration(120).start()
                    } else {
                        tv.animate()
                            .translationX(8f).setDuration(40)
                            .withEndAction {
                                tv.animate()
                                    .translationX(-8f).setDuration(70)
                                    .withEndAction {
                                        tv.animate().translationX(0f).setDuration(60).start()
                                    }
                                    .start()
                            }
                            .start()
                    }
                }

                override fun onAnimationEnd(animation: Animator) {
                    tv.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
                }

                override fun onAnimationCancel(animation: Animator) {
                    tv.translationX = 0f
                    tv.scaleX = 1f
                    tv.scaleY = 1f
                }
            })
        }
        scoreAnimator?.start()
    }

    fun animateStreakChange(oldStreak: Int, newStreak: Int) {
        ensureBound()
        val tv = binding.streakTextView

        if (oldStreak == newStreak) {
            renderStreak(newStreak)
            return
        }

        val delta = newStreak - oldStreak

        streakAnimator?.cancel()
        tv.animate().cancel()
        tv.translationX = 0f
        tv.rotation = 0f
        tv.scaleX = 1f
        tv.scaleY = 1f

        // já atualiza o texto
        renderStreak(newStreak)

        if (delta > 0) {
            tv.animate()
                .scaleX(1.10f).scaleY(1.10f).setDuration(140)
                .withEndAction {
                    tv.animate().rotation(6f).setDuration(80)
                        .withEndAction {
                            tv.animate().rotation(-6f).setDuration(120)
                                .withEndAction {
                                    tv.animate()
                                        .rotation(0f)
                                        .scaleX(1f).scaleY(1f)
                                        .setDuration(160)
                                        .start()
                                }
                                .start()
                        }
                        .start()
                }
                .start()
        } else {
            tv.animate()
                .translationX(8f).setDuration(40)
                .withEndAction {
                    tv.animate()
                        .translationX(-8f).setDuration(70)
                        .withEndAction {
                            tv.animate().translationX(0f).setDuration(60).start()
                        }
                        .start()
                }
                .start()
        }
    }

    fun release() {
        scoreAnimator?.cancel()
        streakAnimator?.cancel()
        scoreAnimator = null
        streakAnimator = null
    }

    // ---------------- helpers ----------------

    private fun renderScore(score: Int) {
        binding.scoreTextView.text = activity.getString(R.string.pontos_format, score)
    }

    private fun renderStreak(streak: Int) {
        binding.streakTextView.text = activity.getString(R.string.streak_format, streak)
    }

    private fun ensureBound() {
        check(::binding.isInitialized) {
            "ScoreUiController: chame bind(binding) antes de usar."
        }
    }
}
