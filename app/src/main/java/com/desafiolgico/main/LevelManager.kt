package com.desafiolgico.main

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import com.desafiolgico.R
import com.desafiolgico.utils.GameDataManager
import com.google.android.material.button.MaterialButton

class LevelManager(private val context: Context) {

    companion object {
        const val THRESHOLD_INTERMEDIATE = 6000
        const val THRESHOLD_ADVANCED = 9000
        const val THRESHOLD_EXPERT = 12000

        private const val VIBRATION_MS = 280L
        private const val ALPHA_LOCKED = 0.6f
        private const val ALPHA_UNLOCKED = 1.0f
    }

    /**
     * ✅ Fonte única de unlock.
     * Retorna a lista de levels destravados AGORA (nesta chamada).
     *
     * showToast=true: use no menu (MainActivity)
     * showToast=false: use in-game (TestActivity) pra você mostrar chip/confetti.
     */
    fun checkAndSaveLevelUnlocks(showToast: Boolean = true): List<String> {
        val total = GameDataManager.getOverallTotalScore(context)
        val unlockedNow = mutableListOf<String>()

        fun unlockIfNeeded(level: String, threshold: Int, nameRes: Int, emoji: String) {
            if (total < threshold) return
            if (GameDataManager.isLevelUnlocked(context, level)) return

            GameDataManager.unlockLevel(context, level)
            unlockedNow.add(level)

            if (showToast) {
                val levelName = context.getString(nameRes)
                val msg = context.getString(R.string.level_unlocked_format, levelName)
                Toast.makeText(context, "$emoji $msg", Toast.LENGTH_LONG).show()
            }
        }

        unlockIfNeeded(
            GameDataManager.Levels.INTERMEDIARIO,
            THRESHOLD_INTERMEDIATE,
            R.string.level_intermediate,
            "🎉"
        )
        unlockIfNeeded(
            GameDataManager.Levels.AVANCADO,
            THRESHOLD_ADVANCED,
            R.string.level_advanced,
            "🌟"
        )
        unlockIfNeeded(
            GameDataManager.Levels.EXPERIENTE,
            THRESHOLD_EXPERT,
            R.string.level_expert,
            "🔥"
        )

        return unlockedNow
    }

    fun setupButtons(
        beginnerButton: MaterialButton,
        intermediateButton: MaterialButton,
        advancedButton: MaterialButton,
        expertButton: MaterialButton,
        exitButton: MaterialButton,
        onLevelClick: (MaterialButton, String) -> Unit,
        onLocked: (String) -> Unit,
        onExitConfirm: () -> Unit
    ) {
        setupLevelButton(beginnerButton, GameDataManager.Levels.INICIANTE, onLevelClick, onLocked)
        setupLevelButton(intermediateButton, GameDataManager.Levels.INTERMEDIARIO, onLevelClick, onLocked)
        setupLevelButton(advancedButton, GameDataManager.Levels.AVANCADO, onLevelClick, onLocked)
        setupLevelButton(expertButton, GameDataManager.Levels.EXPERIENTE, onLevelClick, onLocked)

        exitButton.setOnClickListener { onExitConfirm() }
    }

    fun updateButtonStates(
        intermediateButton: MaterialButton,
        advancedButton: MaterialButton,
        expertButton: MaterialButton
    ) {
        updateSingleButtonState(intermediateButton, GameDataManager.Levels.INTERMEDIARIO)
        updateSingleButtonState(advancedButton, GameDataManager.Levels.AVANCADO)
        updateSingleButtonState(expertButton, GameDataManager.Levels.EXPERIENTE)
    }

    private fun updateSingleButtonState(button: MaterialButton, level: String) {
        val unlocked = GameDataManager.isLevelUnlocked(context, level)

        // ✅ Mantém clicável pra mostrar motivo de bloqueio
        button.isEnabled = true

        if (unlocked) {
            button.alpha = ALPHA_UNLOCKED
            button.icon = null
        } else {
            button.alpha = ALPHA_LOCKED
            button.setIconResource(R.drawable.ic_lock)
        }
    }

    private fun setupLevelButton(
        button: MaterialButton,
        level: String,
        onClick: (MaterialButton, String) -> Unit,
        onLocked: (String) -> Unit
    ) {
        button.setOnClickListener {
            val unlocked = GameDataManager.isLevelUnlocked(context, level)
            if (unlocked) {
                onClick(button, level)
                return@setOnClickListener
            }

            val threshold = thresholdFor(level)
            val levelName = context.getString(nameResFor(level))
            val msg = context.getString(R.string.level_locked_format, levelName, threshold)
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

            vibrateOnce()
            onLocked(level)
        }
    }

    private fun thresholdFor(level: String): Int = when (level) {
        GameDataManager.Levels.INICIANTE -> 0
        GameDataManager.Levels.INTERMEDIARIO -> THRESHOLD_INTERMEDIATE
        GameDataManager.Levels.AVANCADO -> THRESHOLD_ADVANCED
        GameDataManager.Levels.EXPERIENTE -> THRESHOLD_EXPERT
        else -> THRESHOLD_INTERMEDIATE
    }

    private fun nameResFor(level: String): Int = when (level) {
        GameDataManager.Levels.INTERMEDIARIO -> R.string.level_intermediate
        GameDataManager.Levels.AVANCADO -> R.string.level_advanced
        GameDataManager.Levels.EXPERIENTE -> R.string.level_expert
        else -> R.string.level_beginner
    }

    private fun vibrateOnce() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val v = vm?.defaultVibrator
                if (v?.hasVibrator() == true) {
                    v.vibrate(VibrationEffect.createOneShot(VIBRATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (v?.hasVibrator() == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(VIBRATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(VIBRATION_MS)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }
}
