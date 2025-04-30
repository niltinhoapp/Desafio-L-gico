package com.desafiolgico.main


import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class LevelUnlockManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)

    // Define os requisitos de pontos para desbloquear cada nível
    private val levelRequirements = mapOf(
        "Iniciante" to 0,        // Sempre desbloqueado
        "Intermediário" to 500,  // Exemplo: precisa de 500 pontos
        "Avançado" to 1200,      // Exemplo: precisa de 1200 pontos
        "Especialista" to 2500   // Exemplo: precisa de 2500 pontos
    )

    /**
     * Verifica se o jogador desbloqueou o nível baseado na pontuação total.
     */
    fun isLevelUnlocked(levelName: String): Boolean {
        val totalPoints = getTotalPoints()
        val requiredPoints = levelRequirements[levelName] ?: return false
        return totalPoints >= requiredPoints
    }

    /**
     * Salva o último nível desbloqueado.
     */
    fun unlockLevel(levelName: String) {
        sharedPreferences.edit().putString("unlockedLevel", levelName).apply()
    }

    /**
     * Retorna o último nível desbloqueado salvo.
     */
    fun getUnlockedLevel(): String {
        return sharedPreferences.getString("unlockedLevel", "Iniciante") ?: "Iniciante"
    }

    /**
     * Atualiza os pontos totais do jogador (acrescenta aos pontos atuais).
     */
    fun addPoints(pointsEarned: Int) {
        val currentPoints = getTotalPoints()
        val newTotal = currentPoints + pointsEarned
        sharedPreferences.edit().putInt("totalPoints", newTotal).apply()

        val desbloqueados = levelRequirements
            .filter { newTotal >= it.value }
            .map { it.key }

        val novoMaiorNivel = desbloqueados.maxByOrNull { levelRequirements[it] ?: 0 } ?: "Iniciante"
        unlockLevel(novoMaiorNivel)

        Log.d("LevelUnlockManager", "Novo total: $newTotal. Maior nível desbloqueado: $novoMaiorNivel")
    }



    /**
     * Obtém os pontos totais atuais do jogador.
     */
    fun getTotalPoints(): Int {
        return sharedPreferences.getInt("totalPoints", 0)
    }

    /**
     * Retorna uma lista de níveis que o jogador já desbloqueou.
     */
    fun getUnlockedLevelsList(): List<String> {
        val totalPoints = getTotalPoints()
        return levelRequirements.filter { totalPoints >= it.value }.map { it.key }
    }

}
