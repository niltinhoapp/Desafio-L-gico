package com.desafiologico.main

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.desafiologico.utils.GameDataManager

class ScoreManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "score_manager"
        private const val KEY_SESSION_SCORE = "session_score"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var totalScore: Int = prefs.getInt(KEY_SESSION_SCORE, 0)
    private val _currentPoints = MutableLiveData(totalScore)
    val currentPoints: LiveData<Int> = _currentPoints

    private val _currentStreakLive = MutableLiveData(GameDataManager.loadCurrentStreak(context))
    val currentStreakLive: LiveData<Int> = _currentStreakLive

    fun getTotalScore(): Int = totalScore

    fun onCorrectAnswer() {
        val streak = GameDataManager.incrementStreak(context)
        val pointsEarned = 10 + streak // bônus simples baseado na sequência
        totalScore += pointsEarned
        saveScore()
        _currentPoints.value = totalScore
        _currentStreakLive.value = streak
    }

    fun onWrongAnswer() {
        GameDataManager.incrementErrors(context)
        GameDataManager.resetStreak(context)
        totalScore = (totalScore - 2).coerceAtLeast(0)
        saveScore()
        _currentPoints.value = totalScore
        _currentStreakLive.value = GameDataManager.currentStreak
    }

    fun reset() {
        totalScore = 0
        GameDataManager.resetStreak(context)
        GameDataManager.resetErrors(context)
        saveScore()
        _currentPoints.value = totalScore
        _currentStreakLive.value = GameDataManager.currentStreak
    }

    private fun saveScore() {
        prefs.edit().putInt(KEY_SESSION_SCORE, totalScore).apply()
    }
}
