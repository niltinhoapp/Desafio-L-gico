package com.desafiolgico.utils

import android.content.Context
import com.desafiolgico.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EnigmaPortalGate {

    private const val PREF = "enigma_portal_gate"

    private const val MAX_PLAYS_PER_DAY = 3

    private const val KEY_AUTO_OPEN_DONE = "auto_open_done"
    private const val KEY_DAY = "played_day"
    private const val KEY_PLAYS_TODAY = "plays_today"
    private const val KEY_LAST_RESULT = "last_result" // "win" | "lose" | ""
    private const val KEY_RELICS = "relics_count"

    fun requiredScore(): Int = if (BuildConfig.DEBUG) 200 else 7000

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun todayKey(): String =
        SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

    private fun ensureToday(ctx: Context) {
        val p = prefs(ctx)
        val today = todayKey()
        val saved = p.getString(KEY_DAY, null)

        if (saved != today) {
            p.edit()
                .putString(KEY_DAY, today)
                .putInt(KEY_PLAYS_TODAY, 0)
                .putString(KEY_LAST_RESULT, "")
                .apply()
        }
    }

    /**
     * ✅ CHAME isso em telas como Map/Main
     * para garantir que o reset diário já ocorreu.
     */
    fun touchToday(ctx: Context) {
        ensureToday(ctx)
    }

    /** ✅ abre automático só 1x (da vida), quando atingir o score */
    fun shouldAutoOpen(ctx: Context, score: Int): Boolean {
        if (score < requiredScore()) return false
        return !prefs(ctx).getBoolean(KEY_AUTO_OPEN_DONE, false)
    }

    fun markAutoOpened(ctx: Context) {
        prefs(ctx).edit().putBoolean(KEY_AUTO_OPEN_DONE, true).apply()
    }

    /** ✅ tentativas restantes hoje (0..3) */
    fun attemptsLeftToday(ctx: Context): Int {
        ensureToday(ctx)
        val used = prefs(ctx).getInt(KEY_PLAYS_TODAY, 0)
        return (MAX_PLAYS_PER_DAY - used).coerceAtLeast(0)
    }

    fun canPlayToday(ctx: Context): Boolean = attemptsLeftToday(ctx) > 0

    /** ✅ consome 1 tentativa do dia */
    fun markPlayedToday(ctx: Context, win: Boolean) {
        ensureToday(ctx)
        val p = prefs(ctx)
        val used = p.getInt(KEY_PLAYS_TODAY, 0)

        p.edit()
            .putInt(KEY_PLAYS_TODAY, (used + 1).coerceAtMost(MAX_PLAYS_PER_DAY))
            .putString(KEY_LAST_RESULT, if (win) "win" else "lose")
            .apply()
    }

    fun addRelic(ctx: Context) {
        val p = prefs(ctx)
        val cur = p.getInt(KEY_RELICS, 0)
        p.edit().putInt(KEY_RELICS, cur + 1).apply()
    }

    fun getRelicsCount(ctx: Context): Int =
        prefs(ctx).getInt(KEY_RELICS, 0)

}
