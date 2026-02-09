package com.desafiolgico.utils

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EnigmaPortalGate {

    private const val MAX_TRIES_PER_DAY = 3

    // chaves diárias
    private const val KEY_DAY = "portal_day"
    private const val KEY_TRIES_USED = "portal_tries_used"
    private const val KEY_ACTIVE_RUN = "portal_active_run"
    private const val KEY_AUTO_OPENED = "portal_auto_opened"

    // relíquias (você já usa)
    private const val KEY_RELICS = "portal_relics"

    fun requiredScore(): Int = 12000 // ajuste como você quiser

    private fun todayKey(): String {
        val df = SimpleDateFormat("yyyyMMdd", Locale.US)
        return df.format(Date())
    }

    /** ✅ Reset diário (chame em MapActivity e no Portal). */
    fun touchToday(ctx: Context) {
        val today = todayKey()
        val saved = SecurePrefs.getString(ctx, KEY_DAY, null)

        if (saved != today) {
            SecurePrefs.putString(ctx, KEY_DAY, today)
            SecurePrefs.putInt(ctx, KEY_TRIES_USED, 0)
            SecurePrefs.putBoolean(ctx, KEY_ACTIVE_RUN, false)
            SecurePrefs.putBoolean(ctx, KEY_AUTO_OPENED, false)
        }
    }

    fun attemptsLeftToday(ctx: Context): Int {
        touchToday(ctx)
        val used = SecurePrefs.getInt(ctx, KEY_TRIES_USED, 0).coerceAtLeast(0)
        return (MAX_TRIES_PER_DAY - used).coerceAtLeast(0)
    }

    fun canPlayToday(ctx: Context): Boolean {
        // ✅ Se existe run ativa, pode continuar mesmo com 0 tentativas restantes
        return hasActiveRun(ctx) || attemptsLeftToday(ctx) > 0
    }

    fun hasActiveRun(ctx: Context): Boolean {
        touchToday(ctx)
        return SecurePrefs.getBoolean(ctx, KEY_ACTIVE_RUN, false)
    }

    /**
     * ✅ MELHOR OPÇÃO:
     * Reserva 1 tentativa no INÍCIO do Portal.
     * - Se já tem run ativa, não consome mais.
     * - Impede exploit de sair/entrar pra resetar sem custo.
     */
    fun reserveAttemptIfNeeded(ctx: Context): Boolean {
        touchToday(ctx)

        if (hasActiveRun(ctx)) return true

        val left = attemptsLeftToday(ctx)
        if (left <= 0) return false

        val used = SecurePrefs.getInt(ctx, KEY_TRIES_USED, 0)
        SecurePrefs.putInt(ctx, KEY_TRIES_USED, used + 1)
        SecurePrefs.putBoolean(ctx, KEY_ACTIVE_RUN, true)
        return true
    }

    /** ✅ Chame quando o portal terminar (win/fail). */
    fun finishRun(ctx: Context, win: Boolean) {
        touchToday(ctx)
        SecurePrefs.putBoolean(ctx, KEY_ACTIVE_RUN, false)
        if (win) addRelic(ctx)
    }

    // ---------- Auto-open (se você usa no mapa) ----------
    fun shouldAutoOpen(ctx: Context, score: Int): Boolean {
        if (score < requiredScore()) return false
        if (!canPlayToday(ctx)) return false
        return !SecurePrefs.getBoolean(ctx, KEY_AUTO_OPENED, false)
    }

    fun markAutoOpened(ctx: Context) {
        SecurePrefs.putBoolean(ctx, KEY_AUTO_OPENED, true)
    }

    // ---------- Relíquias ----------
    fun addRelic(ctx: Context) {
        val cur = SecurePrefs.getInt(ctx, KEY_RELICS, 0)
        SecurePrefs.putInt(ctx, KEY_RELICS, cur + 1)
    }

    fun getRelicsCount(ctx: Context): Int = SecurePrefs.getInt(ctx, KEY_RELICS, 0)

    /**
     * (Compat) Se você ainda chama markPlayedToday(win),
     * pode manter chamando — agora só finaliza a run.
     */
    fun markPlayedToday(ctx: Context, win: Boolean) {
        finishRun(ctx, win)
    }
}
