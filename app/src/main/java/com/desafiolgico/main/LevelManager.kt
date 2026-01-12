package com.desafiolgico.main

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.core.content.getSystemService
import com.desafiolgico.R
import com.desafiolgico.utils.GameDataManager
import com.google.android.material.button.MaterialButton

class LevelManager(private val context: Context) {

    companion object {
        const val THRESHOLD_INTERMEDIATE = 3500
        const val THRESHOLD_ADVANCED = 6000
        const val THRESHOLD_EXPERT = 10000
        private const val VIBRATION_MS = 300L
    }

    /**
     * ✅ Lógica oficial de unlock.
     * Retorna a lista de levels que foram destravados AGORA.
     * showToast=true: use no menu (MainActivity)
     * showToast=false: use in-game (TestActivity) pra você mostrar chip/confetti.
     */
    fun checkAndSaveLevelUnlocks(showToast: Boolean = true): List<String> {
        val total = GameDataManager.getOverallTotalScore(context)
        val unlockedNow = mutableListOf<String>()

        fun unlockIfNeeded(level: String, threshold: Int, nameRes: Int, emoji: String) {
            if (total >= threshold && !GameDataManager.isLevelUnlocked(context, level)) {
                GameDataManager.unlockLevel(context, level)
                unlockedNow.add(level)

                if (showToast) {
                    val levelName = context.getString(nameRes)
                    val message = context.getString(R.string.level_unlocked_format, levelName)
                    Toast.makeText(context, "$emoji $message", Toast.LENGTH_LONG).show()
                }
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
        // ✅ garante persistência coerente
        checkAndSaveLevelUnlocks(showToast = true)

        updateSingleButtonState(intermediateButton, GameDataManager.Levels.INTERMEDIARIO)
        updateSingleButtonState(advancedButton, GameDataManager.Levels.AVANCADO)
        updateSingleButtonState(expertButton, GameDataManager.Levels.EXPERIENTE)
    }

    private fun updateSingleButtonState(button: MaterialButton, level: String) {
        val unlocked = GameDataManager.isLevelUnlocked(context, level)
        button.isEnabled = true

        if (unlocked) {
            button.alpha = 1f
            button.icon = null
        } else {
            button.alpha = 0.6f
            button.setIconResource(R.drawable.ic_lock)
        }
    }

    private fun setupLevelButton(
        button: MaterialButton,
        level: String,
        onClick: (MaterialButton, String) -> Unit,
        onLocked: (String) -> Unit
    ) {
        val vibrator = context.getSystemService<Vibrator>()

        button.setOnClickListener {
            if (GameDataManager.isLevelUnlocked(context, level)) {
                onClick(button, level)
                return@setOnClickListener
            }

            val threshold = when (level) {
                GameDataManager.Levels.INTERMEDIARIO -> THRESHOLD_INTERMEDIATE
                GameDataManager.Levels.AVANCADO -> THRESHOLD_ADVANCED
                GameDataManager.Levels.EXPERIENTE -> THRESHOLD_EXPERT
                else -> THRESHOLD_INTERMEDIATE
            }

            val levelNameResId = when (level) {
                GameDataManager.Levels.INTERMEDIARIO -> R.string.level_intermediate
                GameDataManager.Levels.AVANCADO -> R.string.level_advanced
                GameDataManager.Levels.EXPERIENTE -> R.string.level_expert
                else -> R.string.level_beginner
            }

            val levelName = context.getString(levelNameResId)
            val message = context.getString(R.string.level_locked_format, levelName, threshold)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()

            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(VIBRATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(VIBRATION_MS)
                    }
                }
            }

            onLocked(level)
        }
    }
}
