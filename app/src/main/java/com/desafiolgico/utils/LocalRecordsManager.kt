package com.desafiolgico.utils

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocalRecordsManager {

    // ✅ Records permanentes (não zera todo dia)
    private const val PREF_RECORDS = "local_records"

    // ✅ Dados diários (streak do dia)
    private const val PREF_DAILY = "local_records_daily"
    private const val KEY_DAY = "day_yyyymmdd"
    private const val KEY_BEST_STREAK = "best_streak"

    // ----------- helpers ----------
    private fun recordsPrefs(ctx: Context) =
        ctx.getSharedPreferences(PREF_RECORDS, Context.MODE_PRIVATE)

    private fun dailyPrefs(ctx: Context) =
        ctx.getSharedPreferences(PREF_DAILY, Context.MODE_PRIVATE)

    private fun todayKey(): String =
        SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

    private fun ensureToday(ctx: Context) {
        val p = dailyPrefs(ctx)
        val today = todayKey()
        val saved = p.getString(KEY_DAY, null)
        if (saved != today) {
            p.edit()
                .putString(KEY_DAY, today)
                .putInt(KEY_BEST_STREAK, 0)
                .apply()
        }
    }

    // =========================
    // ✅ STREAK DO DIA
    // =========================
    fun getBestStreakOfDay(ctx: Context): Int {
        ensureToday(ctx)
        return dailyPrefs(ctx).getInt(KEY_BEST_STREAK, 0)
    }

    fun updateBestStreakOfDay(ctx: Context, currentStreak: Int) {
        ensureToday(ctx)
        val p = dailyPrefs(ctx)
        val best = p.getInt(KEY_BEST_STREAK, 0)
        if (currentStreak > best) {
            p.edit().putInt(KEY_BEST_STREAK, currentStreak).apply()
        }
    }

    // =========================
    // ✅ NORMALIZAÇÃO DO NÍVEL (evita duplicação por acento/variação)
    // =========================
    private fun canonicalLevelKey(level: String): String {
        val t = level.trim().lowercase(Locale.getDefault())
        return when (t) {
            "iniciante" -> GameDataManager.Levels.INICIANTE
            "intermediario", "intermediário" -> GameDataManager.Levels.INTERMEDIARIO
            "avancado", "avançado" -> GameDataManager.Levels.AVANCADO
            "experiente" -> GameDataManager.Levels.EXPERIENTE
            else -> level // fallback (se você tiver níveis especiais)
        }
    }

    // =========================
    // ✅ MELHOR SCORE POR NÍVEL (permanente)
    // =========================
    private fun keyBestScore(level: String) = "best_score_${canonicalLevelKey(level)}"

    fun getBestScoreForLevel(ctx: Context, level: String): Int =
        recordsPrefs(ctx).getInt(keyBestScore(level), 0)

    /** salva o record se scoreNovo for maior */
    fun updateBestScoreForLevel(ctx: Context, level: String, scoreNovo: Int) {
        val p = recordsPrefs(ctx)
        val key = keyBestScore(level)
        val best = p.getInt(key, 0)
        if (scoreNovo > best) {
            p.edit().putInt(key, scoreNovo).apply()
        }
    }

    // =========================
    // ✅ MELHOR TEMPO MÉDIO POR NÍVEL (menor é melhor)
    // =========================
    private fun keyBestAvgTime(level: String) = "best_avg_time_${canonicalLevelKey(level)}"

    fun getBestAvgTimeForLevel(ctx: Context, level: String): Long =
        recordsPrefs(ctx).getLong(keyBestAvgTime(level), 0L)

    /** salva o record se tempoNovo for menor (e > 0) */
    fun updateBestAvgTimeForLevel(ctx: Context, level: String, avgTimeMsNovo: Long) {
        if (avgTimeMsNovo <= 0L) return
        val p = recordsPrefs(ctx)
        val key = keyBestAvgTime(level)
        val best = p.getLong(key, 0L)
        if (best == 0L || avgTimeMsNovo < best) {
            p.edit().putLong(key, avgTimeMsNovo).apply()
        }
    }
}
