package com.desafiologico.utils

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.desafiologico.R

object GameDataManager {
    private const val PREFS_NAME = "game_data_manager"
    private const val DEFAULT_USERNAME = "Jogador"

    private const val KEY_USERNAME = "username"
    private const val KEY_PHOTO_URL = "photoUrl"
    private const val KEY_AVATAR_ID = "avatarId"

    private const val KEY_COINS = "coins"
    private const val KEY_UNLOCKED_LEVELS = "unlocked_levels"
    private const val KEY_NOTIFIED_LEVELS = "notified_levels"
    private const val KEY_OVERALL_TOTAL_SCORE = "overall_total_score"

    private const val KEY_PROGRESS_LEVEL = "progress_level"
    private const val KEY_PROGRESS_SCORE = "progress_score"
    private const val KEY_PROGRESS_ERRORS = "progress_errors"

    private const val KEY_CURRENT_STREAK = "current_streak"
    private const val KEY_TOTAL_ERRORS = "total_errors"

    object Levels {
        const val INICIANTE = "Iniciante"
        const val INTERMEDIARIO = "Intermediário"
        const val AVANCADO = "Avançado"
        const val ESPECIALISTA = "Especialista"
    }

    var currentStreak: Int = 0
        private set

    var totalErrors: Int = 0
        private set

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // region User data
    fun saveUserData(context: Context, username: String, photoUrl: String?, avatarId: Int?) {
        prefs(context).edit().apply {
            putString(KEY_USERNAME, username)
            photoUrl?.let { putString(KEY_PHOTO_URL, it) }
            avatarId?.let { putInt(KEY_AVATAR_ID, it) }
        }.apply()
    }

    fun loadUserData(context: Context): Triple<String, String?, Int> {
        val preferences = prefs(context)
        val username = preferences.getString(KEY_USERNAME, DEFAULT_USERNAME) ?: DEFAULT_USERNAME
        val photoUrl = preferences.getString(KEY_PHOTO_URL, null)
        val avatarId = preferences.getInt(KEY_AVATAR_ID, R.drawable.avatar1)
        return Triple(username, photoUrl, avatarId)
    }
    // endregion

    // region Coins
    fun saveCoins(context: Context, coins: Int) {
        prefs(context).edit().putInt(KEY_COINS, coins).apply()
    }

    fun loadCoins(context: Context): Int = prefs(context).getInt(KEY_COINS, 0)
    // endregion

    // region Scores and progress
    fun addScoreToOverallTotal(context: Context, scoreDelta: Int) {
        val newScore = (getOverallTotalScore(context) + scoreDelta).coerceAtLeast(0)
        prefs(context).edit().putInt(KEY_OVERALL_TOTAL_SCORE, newScore).apply()
    }

    fun getOverallTotalScore(context: Context): Int =
        prefs(context).getInt(KEY_OVERALL_TOTAL_SCORE, 0)

    fun saveProgresso(context: Context, nivel: String, pontuacao: Int, erros: Int) {
        prefs(context).edit().apply {
            putString(KEY_PROGRESS_LEVEL, nivel)
            putInt(KEY_PROGRESS_SCORE, pontuacao)
            putInt(KEY_PROGRESS_ERRORS, erros)
        }.apply()
        totalErrors = erros
    }
    // endregion

    // region Streak & errors
    fun loadCurrentStreak(context: Context): Int {
        currentStreak = prefs(context).getInt(KEY_CURRENT_STREAK, 0)
        return currentStreak
    }

    fun loadTotalErrors(context: Context): Int {
        totalErrors = prefs(context).getInt(KEY_TOTAL_ERRORS, 0)
        return totalErrors
    }

    fun incrementStreak(context: Context): Int {
        currentStreak = loadCurrentStreak(context) + 1
        prefs(context).edit().putInt(KEY_CURRENT_STREAK, currentStreak).apply()
        return currentStreak
    }

    fun resetStreak(context: Context) {
        currentStreak = 0
        prefs(context).edit().putInt(KEY_CURRENT_STREAK, currentStreak).apply()
    }

    fun incrementErrors(context: Context) {
        totalErrors = loadTotalErrors(context) + 1
        prefs(context).edit().putInt(KEY_TOTAL_ERRORS, totalErrors).apply()
    }

    fun resetErrors(context: Context) {
        totalErrors = 0
        prefs(context).edit().putInt(KEY_TOTAL_ERRORS, totalErrors).apply()
    }
    // endregion

    // region Levels
    fun getUnlockedLevels(context: Context): Set<String> {
        val stored = prefs(context).getStringSet(KEY_UNLOCKED_LEVELS, setOf(Levels.INICIANTE))
        return stored?.toSet() ?: setOf(Levels.INICIANTE)
    }

    fun saveUnlockedLevels(context: Context, levels: Set<String>) {
        prefs(context).edit().putStringSet(KEY_UNLOCKED_LEVELS, levels).apply()
    }

    fun unlockLevel(context: Context, level: String) {
        val unlocked = getUnlockedLevels(context).toMutableSet()
        if (unlocked.add(level)) {
            saveUnlockedLevels(context, unlocked)
        }
    }

    fun isLevelUnlocked(context: Context, level: String): Boolean =
        getUnlockedLevels(context).contains(level)

    fun verificarDesbloqueioNivel(context: Context): Set<String> {
        val unlocked = getUnlockedLevels(context).toMutableSet()
        unlocked.add(Levels.INICIANTE)

        // Thresholds simples baseados na pontuação total geral.
        val totalScore = getOverallTotalScore(context)
        if (totalScore >= 50) unlocked.add(Levels.INTERMEDIARIO)
        if (totalScore >= 120) unlocked.add(Levels.AVANCADO)

        saveUnlockedLevels(context, unlocked)
        return unlocked
    }

    fun checkLevelUnlockWithNotification(context: Context) {
        val unlocked = verificarDesbloqueioNivel(context)
        val preferences = prefs(context)
        val alreadyNotified =
            preferences.getStringSet(KEY_NOTIFIED_LEVELS, setOf(Levels.INICIANTE))?.toMutableSet()
                ?: mutableSetOf(Levels.INICIANTE)

        val newlyUnlocked = unlocked.filter { it !in alreadyNotified && it != Levels.INICIANTE }
        if (newlyUnlocked.isNotEmpty()) {
            val message = newlyUnlocked.joinToString(separator = "\n") { level ->
                "Nível \"$level\" desbloqueado!"
            }
            AlertDialog.Builder(context)
                .setTitle("Novo nível desbloqueado")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()

            alreadyNotified.addAll(newlyUnlocked)
            preferences.edit().putStringSet(KEY_NOTIFIED_LEVELS, alreadyNotified).apply()
        }
    }
    // endregion

    fun resetarTodosOsDados(context: Context) {
        prefs(context).edit().clear().apply()
        currentStreak = 0
        totalErrors = 0
    }
}
