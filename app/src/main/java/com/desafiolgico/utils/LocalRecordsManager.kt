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
    // ✅ MELHOR SCORE POR NÍVEL
    // =========================
    private fun keyBestScore(level: String) = "best_score_$level"

    fun getBestScoreForLevel(ctx: Context, level: String): Int =
        recordsPrefs(ctx).getInt(keyBestScore(level), 0)

    fun updateBestScoreForLevel(ctx: Context, level: String, scoreNovo: Int) {
        val p = recordsPrefs(ctx)
        val best = p.getInt(keyBestScore(level), 0)
        if (scoreNovo > best) {
            p.edit().putInt(keyBestScore(level), scoreNovo).apply()
        }
    }

    // =========================
    // ✅ MELHOR TEMPO MÉDIO (menor é melhor)
    // =========================
    private fun keyBestAvgTime(level: String) = "best_avg_time_$level"

    fun getBestAvgTimeForLevel(ctx: Context, level: String): Long =
        recordsPrefs(ctx).getLong(keyBestAvgTime(level), 0L)

    fun updateBestAvgTimeForLevel(ctx: Context, level: String, avgTimeMsNovo: Long) {
        if (avgTimeMsNovo <= 0L) return
        val p = recordsPrefs(ctx)
        val best = p.getLong(keyBestAvgTime(level), 0L)
        if (best == 0L || avgTimeMsNovo < best) {
            p.edit().putLong(keyBestAvgTime(level), avgTimeMsNovo).apply()
        }
    }
}
