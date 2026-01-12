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
        private const val THRESHOLD_INTERMEDIATE = 3500
        private const val THRESHOLD_ADVANCED = 6000
        private const val THRESHOLD_EXPERT = 10000

        private const val VIBRATION_MS = 300L
    }

    /**
     * ✅ Verifica e salva os desbloqueios de níveis com base na pontuação total ACUMULADA.
     * ✅ Retorna uma lista com os níveis que foram desbloqueados AGORA (somente 1x).
     * ⚠️ Não mostra Toast aqui (a Activity decide o "AAA").
     */
    fun checkAndSaveLevelUnlocks(): List<String> {
        val totalScoreAcumulado = GameDataManager.getOverallTotalScore(context)
        val newlyUnlocked = mutableListOf<String>()

        fun unlockIfNeeded(level: String, threshold: Int) {
            if (totalScoreAcumulado >= threshold &&
                !GameDataManager.isLevelUnlocked(context, level)
            ) {
                GameDataManager.unlockLevel(context, level)
                newlyUnlocked.add(level)
            }
        }

        unlockIfNeeded(GameDataManager.Levels.INTERMEDIARIO, THRESHOLD_INTERMEDIATE)
        unlockIfNeeded(GameDataManager.Levels.AVANCADO, THRESHOLD_ADVANCED)
        unlockIfNeeded(GameDataManager.Levels.EXPERIENTE, THRESHOLD_EXPERT)

        return newlyUnlocked
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

    /**
     * ✅ Atualiza os estados visuais dos botões.
     * ✅ Também recalcula desbloqueios e retorna os níveis que liberaram agora.
     */
    fun updateButtonStates(
        intermediateButton: MaterialButton,
        advancedButton: MaterialButton,
        expertButton: MaterialButton
    ): List<String> {
        val unlockedNow = checkAndSaveLevelUnlocks()

        updateSingleButtonState(intermediateButton, GameDataManager.Levels.INTERMEDIARIO)
        updateSingleButtonState(advancedButton, GameDataManager.Levels.AVANCADO)
        updateSingleButtonState(expertButton, GameDataManager.Levels.EXPERIENTE)

        return unlockedNow
    }

    private fun updateSingleButtonState(button: MaterialButton, level: String) {
        if (GameDataManager.isLevelUnlocked(context, level)) {
            button.isEnabled = true
            button.alpha = 1f
            button.icon = null
        } else {
            // ✅ continua clicável para mostrar mensagem
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
            // ✅ se está liberado, entra no nível
            if (GameDataManager.isLevelUnlocked(context, level)) {
                onClick(button, level)
                return@setOnClickListener
            }

            // ✅ se está bloqueado, mostra msg e vibra
            val threshold = thresholdFor(level)
            val levelName = context.getString(levelNameRes(level))

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

    private fun thresholdFor(level: String): Int = when (level) {
        GameDataManager.Levels.INTERMEDIARIO -> THRESHOLD_INTERMEDIATE
        GameDataManager.Levels.AVANCADO -> THRESHOLD_ADVANCED
        GameDataManager.Levels.EXPERIENTE -> THRESHOLD_EXPERT
        else -> THRESHOLD_INTERMEDIATE
    }

    private fun levelNameRes(level: String): Int = when (level) {
        GameDataManager.Levels.INTERMEDIARIO -> R.string.level_intermediate
        GameDataManager.Levels.AVANCADO -> R.string.level_advanced
        GameDataManager.Levels.EXPERIENTE -> R.string.level_expert
        else -> R.string.level_beginner
    }
}
