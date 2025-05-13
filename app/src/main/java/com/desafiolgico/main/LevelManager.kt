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

class LevelManager(
    private val context: Context
) {

    companion object {
        private const val THRESHOLD_INTERMEDIATE = 4000
        private const val THRESHOLD_ADVANCED = 7500
        private const val THRESHOLD_EXPERT = 11000
        private const val VIBRATION_MS = 300L
    }

    /**
     * Verifica e salva os desbloqueios de níveis com base na pontuação total ACUMULADA.
     */
    fun checkAndSaveLevelUnlocks() {
        val totalScoreAcumulado = GameDataManager.getOverallTotalScore(context)

        // 🔹 Nível Intermediário
        if (
            totalScoreAcumulado >= THRESHOLD_INTERMEDIATE &&
            !GameDataManager.isLevelUnlocked(context, GameDataManager.Levels.INTERMEDIARIO)
        ) {
            GameDataManager.unlockLevel(context, GameDataManager.Levels.INTERMEDIARIO)
            val levelName = context.getString(R.string.level_intermediate)
            val message = context.getString(R.string.level_unlocked_format, levelName)
            Toast.makeText(context, "🎉 $message", Toast.LENGTH_LONG).show()
        }

        // 🔹 Nível Avançado
        if (
            totalScoreAcumulado >= THRESHOLD_ADVANCED &&
            !GameDataManager.isLevelUnlocked(context, GameDataManager.Levels.AVANCADO)
        ) {
            GameDataManager.unlockLevel(context, GameDataManager.Levels.AVANCADO)
            val levelName = context.getString(R.string.level_advanced)
            val message = context.getString(R.string.level_unlocked_format, levelName)
            Toast.makeText(context, "🌟 $message", Toast.LENGTH_LONG).show()
        }

        // 🔹 Nível Experiente
        if (
            totalScoreAcumulado >= THRESHOLD_EXPERT &&
            !GameDataManager.isLevelUnlocked(context, GameDataManager.Levels.EXPERIENTE)
        ) {
            GameDataManager.unlockLevel(context, GameDataManager.Levels.EXPERIENTE)
            val levelName = context.getString(R.string.level_expert)
            val message = context.getString(R.string.level_unlocked_format, levelName)
            Toast.makeText(context, "🔥 $message", Toast.LENGTH_LONG).show()
        }
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
        // Recalcula desbloqueios com base na pontuação total
        checkAndSaveLevelUnlocks()

        updateSingleButtonState(intermediateButton, GameDataManager.Levels.INTERMEDIARIO)
        updateSingleButtonState(advancedButton, GameDataManager.Levels.AVANCADO)
        updateSingleButtonState(expertButton, GameDataManager.Levels.EXPERIENTE)
    }

    private fun updateSingleButtonState(button: MaterialButton, level: String) {
        if (GameDataManager.isLevelUnlocked(context, level)) {
            button.isEnabled = true
            button.alpha = 1f
            button.icon = null
        } else {
            // Mantém clicável para mostrar mensagem de bloqueio
            button.isEnabled = true
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

            val message = context.getString(
                R.string.level_locked_format,
                levelName,
                threshold
            )
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()

            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(
                            VibrationEffect.createOneShot(
                                VIBRATION_MS,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
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
