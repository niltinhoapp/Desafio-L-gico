package com.desafiologico.main

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import com.desafiologico.R
import com.desafiologico.utils.GameDataManager
import com.google.android.material.button.MaterialButton

class LevelManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "game_data"
        private const val KEY_TOTAL_POINTS = "totalPoints"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Retorna os pontos totais salvos
    fun getTotalPoints(): Int = prefs.getInt(KEY_TOTAL_POINTS, 0)

    // Adiciona pontos e salva no SharedPreferences
    fun addPoints(points: Int) {
        val updated = getTotalPoints() + points
        prefs.edit().putInt(KEY_TOTAL_POINTS, updated).apply()
        Log.d("LevelManager", "Pontos atualizados: $updated")
    }

    // Verifica se o nível está desbloqueado via GameDataManager
    private fun isLevelUnlocked(level: String): Boolean {
        return GameDataManager.isLevelUnlocked(context, level)
    }

    // Configura os botões dos níveis e botão de sair
    fun setupButtons(
        beginnerButton: MaterialButton,
        intermediateButton: MaterialButton,
        advancedButton: MaterialButton,
        exitButton: MaterialButton,
        onLevelClick: (MaterialButton, String) -> Unit,
        onLocked: (String) -> Unit, // Agora aceita o nome do nível
        onExitConfirm: () -> Unit
    ) {
        setupLevelButton(beginnerButton, "Iniciante", onLevelClick, onLocked)
        setupLevelButton(intermediateButton, "Intermediário", onLevelClick, onLocked)
        setupLevelButton(advancedButton, "Avançado", onLevelClick, onLocked)

        exitButton.setOnClickListener { onExitConfirm() }
    }

    // Atualiza o estado visual e funcional dos botões de nível
    fun updateButtonStates(
        intermediateButton: MaterialButton,
        advancedButton: MaterialButton
    ) {
        updateSingleButtonState(intermediateButton, "Intermediário")
        updateSingleButtonState(advancedButton, "Avançado")
    }

    // Atualiza estado de um botão com base no desbloqueio
    private fun updateSingleButtonState(button: MaterialButton, level: String) {
        if (isLevelUnlocked(level)) {
            button.isEnabled = true
            button.alpha = 1.0f
            button.icon = null
        } else {
            button.isEnabled = true // Mantém habilitado para capturar clique e exibir aviso
            button.alpha = 0.6f
            button.setIconResource(R.drawable.ic_lock)
        }
    }

    // Configura o comportamento dos botões de nível
    private fun setupLevelButton(
        button: MaterialButton,
        level: String,
        onClick: (MaterialButton, String) -> Unit,
        onLocked: (String) -> Unit
    ) {
        val context = button.context
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        button.setOnClickListener {
            if (isLevelUnlocked(level)) {
                onClick(button, level)
            } else {
                // Efeito visual e tátil ao tocar em nível bloqueado
                Toast.makeText(context, "Nível \"$level\" bloqueado!", Toast.LENGTH_SHORT).show()
                vibrator?.let {
                    if (it.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            it.vibrate(VibrationEffect.createOneShot(600, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            it.vibrate(600)
                        }
                    }
                }
                onLocked(level)
            }
        }
    }

    fun addPointsAndCheckLevelUp(points: Int): Boolean {
        val nivelAntes = getTotalPoints()
        addPoints(points)

        val nivelDepois = getTotalPoints()
        Log.d("LevelManager", "Pontos antes: $nivelAntes, depois: $nivelDepois")
        return nivelDepois != nivelAntes
    }
}
