package com.desafiolgico.main.quiz

import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityTestBinding
import kotlin.math.roundToInt

class TimerController(
    private val activity: AppCompatActivity,
    private val binding: ActivityTestBinding,

    private val getLevel: () -> String,

    // state (GameState) via getters/setters
    private val isTimerPaused: () -> Boolean,
    private val getRemainingMs: () -> Long,
    private val getLastCriticalSecond: () -> Int,          // ✅ NOVO
    private val setTimerPaused: (Boolean) -> Unit,
    private val setRemainingMs: (Long) -> Unit,
    private val setTotalMs: (Long) -> Unit,
    private val setLastCriticalSecond: (Int) -> Unit,

    private val onTinyTickUi: () -> Unit,
    private val onTimeUp: () -> Unit
) {

    companion object {
        private const val GREEN_THRESHOLD_PERCENT = 50
        private const val YELLOW_THRESHOLD_PERCENT = 20
        private const val TIMER_INTERVAL_MS = 100L
    }

    private var countDownTimer: CountDownTimer? = null
    private var currentTimerColorRes: Int = R.drawable.progress_green
    private var currentTotalMs: Long = 0L

    fun cancel() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    fun pause() {
        cancel()
        setTimerPaused(true)
    }

    fun resume() {
        if (isTimerPaused() && getRemainingMs() > 0L) start()
    }

    fun start() {
        cancel()

        val level = getLevel()
        val baseTime = when (level.uppercase()) {
            "INTERMEDIARIO", "INTERMEDIÁRIO" -> 20_000L
            "AVANCADO", "AVANÇADO" -> 15_000L
            "EXPERIENTE" -> 15_000L
            else -> 30_000L
        }

        currentTotalMs = baseTime
        setTotalMs(baseTime)

        val duration =
            if (isTimerPaused() && getRemainingMs() > 0L) getRemainingMs()
            else baseTime

        setTimerPaused(false)
        setRemainingMs(duration)
        setLastCriticalSecond(-1)

        // UI inicial
        binding.timerTextView.text = formatTime(duration)
        binding.timerProgressBar.max = 100
        binding.timerProgressBar.progress =
            ((duration.toDouble() / baseTime.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)

        currentTimerColorRes = R.drawable.progress_green
        binding.timerProgressBar.progressDrawable =
            ContextCompat.getDrawable(activity, currentTimerColorRes)

        countDownTimer = object : CountDownTimer(duration, TIMER_INTERVAL_MS) {
            override fun onTick(millisUntilFinished: Long) {
                setRemainingMs(millisUntilFinished)

                val progressPercent =
                    ((millisUntilFinished.toDouble() / baseTime.toDouble()) * 100.0)
                        .roundToInt()
                        .coerceIn(0, 100)

                binding.timerTextView.text = formatTime(millisUntilFinished)
                binding.timerProgressBar.progress = progressPercent

                val newColorRes = when {
                    progressPercent > GREEN_THRESHOLD_PERCENT -> R.drawable.progress_green
                    progressPercent > YELLOW_THRESHOLD_PERCENT -> R.drawable.progress_yellow
                    else -> R.drawable.progress_red
                }

                if (newColorRes != currentTimerColorRes) {
                    currentTimerColorRes = newColorRes
                    binding.timerProgressBar.progressDrawable =
                        ContextCompat.getDrawable(activity, newColorRes)
                }

                // ✅ Tick 1x por segundo (3,2,1), sem spam em 100ms
                val secLeft = (millisUntilFinished / 1000L).toInt()
                if (secLeft in 1..3) {
                    val last = getLastCriticalSecond()
                    if (secLeft != last) {
                        setLastCriticalSecond(secLeft)
                        onTinyTickUi()
                    }
                }
            }

            override fun onFinish() {
                setRemainingMs(0L)
                setTimerPaused(false)
                setLastCriticalSecond(-1)

                binding.timerProgressBar.progress = 0
                binding.timerProgressBar.progressDrawable =
                    ContextCompat.getDrawable(activity, R.drawable.progress_red)
                binding.timerTextView.text = "00:00"

                onTimeUp()
            }
        }.start()
    }

    private fun formatTime(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(0L)
        val m = totalSec / 60L
        val s = totalSec % 60L
        return String.format("%02d:%02d", m, s)
    }

    fun totalMs(): Long = currentTotalMs

    fun spentMs(): Long = (currentTotalMs - getRemainingMs()).coerceAtLeast(0L)
}
