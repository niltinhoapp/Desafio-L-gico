package com.desafiolgico.utils

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object LocalRecordsManager {

    // ✅ Records permanentes (não zera todo dia)
    private const val PREF_RECORDS = "local_records"

    // ✅ Dados diários (streak do dia)
    private const val PREF_DAILY = "local_records_daily"
    private const val KEY_DAY = "day_yyyymmdd"
    private const val KEY_BEST_STREAK = "best_streak"

    // yyyyMMdd (estável)
    private val DAY_FMT: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE

    // ---------------- prefs helpers ----------------

    private fun recordsPrefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREF_RECORDS, Context.MODE_PRIVATE)

    private fun dailyPrefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREF_DAILY, Context.MODE_PRIVATE)

    private fun todayKey(): String = LocalDate.now().format(DAY_FMT)

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
        return dailyPrefs(ctx).getInt(KEY_BEST_STREAK, 0).coerceAtLeast(0)
    }

    fun updateBestStreakOfDay(ctx: Context, currentStreak: Int) {
        ensureToday(ctx)
        val s = currentStreak.coerceAtLeast(0)
        val p = dailyPrefs(ctx)
        val best = p.getInt(KEY_BEST_STREAK, 0).coerceAtLeast(0)
        if (s > best) {
            p.edit().putInt(KEY_BEST_STREAK, s).apply()
        }
    }

    fun resetDailyStreak(ctx: Context) {
        dailyPrefs(ctx).edit()
            .putString(KEY_DAY, todayKey())
            .putInt(KEY_BEST_STREAK, 0)
            .apply()
    }

    // =========================
    // ✅ NORMALIZAÇÃO DO NÍVEL
    // =========================

    private fun canonicalLevelKey(level: String): String {
        val t = level.trim().lowercase(Locale.ROOT)
        return when (t) {
            "iniciante" -> GameDataManager.Levels.INICIANTE
            "intermediario", "intermediário" -> GameDataManager.Levels.INTERMEDIARIO
            "avancado", "avançado" -> GameDataManager.Levels.AVANCADO
            "experiente" -> GameDataManager.Levels.EXPERIENTE
            else -> level.trim()
        }
    }

    // =========================
    // ✅ MELHOR SCORE POR NÍVEL
    // =========================

    private fun keyBestScore(level: String) = "best_score_${canonicalLevelKey(level)}"

    fun getBestScoreForLevel(ctx: Context, level: String): Int =
        recordsPrefs(ctx).getInt(keyBestScore(level), 0).coerceAtLeast(0)

    /** salva o record se scoreNovo for maior */
    fun updateBestScoreForLevel(ctx: Context, level: String, scoreNovo: Int) {
        val s = scoreNovo.coerceAtLeast(0)
        val p = recordsPrefs(ctx)
        val key = keyBestScore(level)
        val best = p.getInt(key, 0).coerceAtLeast(0)
        if (s > best) {
            p.edit().putInt(key, s).apply()
        }
    }

    // =========================
    // ✅ MELHOR TEMPO MÉDIO POR NÍVEL (menor é melhor)
    // =========================

    private fun keyBestAvgTime(level: String) = "best_avg_time_${canonicalLevelKey(level)}"

    /** retorna ms (0 = sem record) */
    fun getBestAvgTimeForLevel(ctx: Context, level: String): Long =
        recordsPrefs(ctx).getLong(keyBestAvgTime(level), 0L).coerceAtLeast(0L)

    /** salva o record se tempoNovo for menor (e > 0) */
    fun updateBestAvgTimeForLevel(ctx: Context, level: String, avgTimeMsNovo: Long) {
        val t = avgTimeMsNovo.coerceAtLeast(0L)
        if (t <= 0L) return

        val p = recordsPrefs(ctx)
        val key = keyBestAvgTime(level)
        val best = p.getLong(key, 0L).coerceAtLeast(0L)

        if (best == 0L || t < best) {
            p.edit().putLong(key, t).apply()
        }
    }

    // =========================
    // ✅ RESET OPCIONAL (somente records locais)
    // =========================

    fun resetAllRecords(ctx: Context) {
        recordsPrefs(ctx).edit().clear().apply()
        resetDailyStreak(ctx)
    }

    fun resetLevelRecords(ctx: Context, level: String) {
        val p = recordsPrefs(ctx).edit()
        p.remove(keyBestScore(level))
        p.remove(keyBestAvgTime(level))
        p.apply()
    }
}
