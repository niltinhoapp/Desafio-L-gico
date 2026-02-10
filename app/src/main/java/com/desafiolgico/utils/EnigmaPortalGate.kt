package com.desafiolgico.utils

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * EnigmaPortalGate (PRO / Premium):
 * - Reset diário seguro
 * - Tentativas por dia (3)
 * - RUN ativa (para continuar mesmo com 0 tentativas)
 * - Anti-exploit: tentativa é reservada ao ENTRAR no portal
 * - Fail-safe: evita "run fantasma" presa para sempre (TTL)
 * - Auto-open controlado (só 1x por dia, e nunca durante RUN ativa)
 *
 * Requer: SecurePrefs
 */
object EnigmaPortalGate {

    private const val TAG = "EnigmaPortalGate"

    private const val MAX_TRIES_PER_DAY = 3

    // chaves diárias
    private const val KEY_DAY = "portal_day"
    private const val KEY_TRIES_USED = "portal_tries_used"
    private const val KEY_ACTIVE_RUN = "portal_active_run"
    private const val KEY_AUTO_OPENED = "portal_auto_opened"

    // relíquias
    private const val KEY_RELICS = "portal_relics"

    // PRO: metadados da RUN (anti run fantasma)
    private const val KEY_RUN_START_TS = "portal_run_start_ts"
    private const val KEY_RUN_LAST_TOUCH_TS = "portal_run_last_touch_ts"

    // TTL da RUN: se ficar travada (crash / kill), liberamos automaticamente depois disso
    // (30 min é um bom equilíbrio; pode ajustar)
    private const val RUN_TTL_MS = 30L * 60L * 1000L

    fun requiredScore(): Int = 100

    private fun todayKey(): String {
        val df = SimpleDateFormat("yyyyMMdd", Locale.US)
        return df.format(Date())
    }

    /**
     * ✅ Reset diário (chame em MapActivity e no Portal).
     * Premium: se mudou o dia, zera tentativas + flags e limpa run antiga.
     */
    fun touchToday(ctx: Context) {
        val today = todayKey()
        val saved = SecurePrefs.getString(ctx, KEY_DAY, null)

        if (saved != today) {
            SecurePrefs.putString(ctx, KEY_DAY, today)
            SecurePrefs.putInt(ctx, KEY_TRIES_USED, 0)
            SecurePrefs.putBoolean(ctx, KEY_ACTIVE_RUN, false)
            SecurePrefs.putBoolean(ctx, KEY_AUTO_OPENED, false)
            // limpa metadata de run antiga
            SecurePrefs.remove(ctx, KEY_RUN_START_TS)
            SecurePrefs.remove(ctx, KEY_RUN_LAST_TOUCH_TS)
        }

        // PRO: garante que não existe run fantasma presa
        ensureRunNotStale(ctx)
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

    /**
     * PRO: hasActiveRun com auto-correção (stale run -> desativa).
     */
    fun hasActiveRun(ctx: Context): Boolean {
        touchToday(ctx)
        ensureRunNotStale(ctx)
        return SecurePrefs.getBoolean(ctx, KEY_ACTIVE_RUN, false)
    }

    /**
     * PRO: chame isso quando o Portal abrir e também em onResume() do Portal
     * para manter a RUN “viva” e evitar TTL indevido.
     */
    fun touchRun(ctx: Context) {
        touchToday(ctx)
        if (SecurePrefs.getBoolean(ctx, KEY_ACTIVE_RUN, false)) {
            SecurePrefs.putLong(ctx, KEY_RUN_LAST_TOUCH_TS, System.currentTimeMillis())
        }
    }

    /**
     * ✅ MELHOR OPÇÃO:
     * Reserva 1 tentativa no INÍCIO do Portal.
     * - Se já tem run ativa, não consome mais.
     * - Impede exploit de sair/entrar pra resetar sem custo.
     * - Registra start/lastTouch para TTL.
     */
    fun reserveAttemptIfNeeded(ctx: Context): Boolean {
        touchToday(ctx)

        if (hasActiveRun(ctx)) {
            touchRun(ctx)
            return true
        }

        val left = attemptsLeftToday(ctx)
        if (left <= 0) return false

        val used = SecurePrefs.getInt(ctx, KEY_TRIES_USED, 0).coerceAtLeast(0)
        SecurePrefs.putInt(ctx, KEY_TRIES_USED, used + 1)

        SecurePrefs.putBoolean(ctx, KEY_ACTIVE_RUN, true)

        val now = System.currentTimeMillis()
        SecurePrefs.putLong(ctx, KEY_RUN_START_TS, now)
        SecurePrefs.putLong(ctx, KEY_RUN_LAST_TOUCH_TS, now)

        return true
    }

    /**
     * ✅ Chame quando o portal terminar (win/fail).
     * Premium: sempre finaliza e limpa metadata.
     */
    fun finishRun(ctx: Context, win: Boolean) {
        touchToday(ctx)

        SecurePrefs.putBoolean(ctx, KEY_ACTIVE_RUN, false)
        SecurePrefs.remove(ctx, KEY_RUN_START_TS)
        SecurePrefs.remove(ctx, KEY_RUN_LAST_TOUCH_TS)

        if (win) addRelic(ctx)
    }

    // ---------- Auto-open (se você usa no mapa) ----------
    fun shouldAutoOpen(ctx: Context, score: Int): Boolean {
        touchToday(ctx)

        // PRO: se tem run ativa, NÃO auto abre (evita teleport)
        if (hasActiveRun(ctx)) return false

        if (score < requiredScore()) return false
        if (!canPlayToday(ctx)) return false
        return !SecurePrefs.getBoolean(ctx, KEY_AUTO_OPENED, false)
    }

    fun markAutoOpened(ctx: Context) {
        SecurePrefs.putBoolean(ctx, KEY_AUTO_OPENED, true)
    }

    // ---------- Relíquias ----------
    fun addRelic(ctx: Context) {
        val cur = SecurePrefs.getInt(ctx, KEY_RELICS, 0).coerceAtLeast(0)
        SecurePrefs.putInt(ctx, KEY_RELICS, cur + 1)
    }

    fun getRelicsCount(ctx: Context): Int = SecurePrefs.getInt(ctx, KEY_RELICS, 0).coerceAtLeast(0)

    /**
     * (Compat) Se você ainda chama markPlayedToday(win),
     * pode manter chamando — agora só finaliza a run.
     */
    fun markPlayedToday(ctx: Context, win: Boolean) {
        finishRun(ctx, win)
    }

    // =============================================================================================
    // Internals (PRO)
    // =============================================================================================

    /**
     * Se a RUN ficou presa (crash, kill, bug), destrava automaticamente após TTL.
     * Isso evita: badge “em andamento” infinito, portal não abrindo, etc.
     */
    private fun ensureRunNotStale(ctx: Context) {
        val active = SecurePrefs.getBoolean(ctx, KEY_ACTIVE_RUN, false)
        if (!active) return

        val now = System.currentTimeMillis()
        val lastTouch = SecurePrefs.getLong(ctx, KEY_RUN_LAST_TOUCH_TS, 0L)
        val start = SecurePrefs.getLong(ctx, KEY_RUN_START_TS, 0L)

        // Se não tem metadata (versão antiga), cria agora para não quebrar
        if (start <= 0L) SecurePrefs.putLong(ctx, KEY_RUN_START_TS, now)
        if (lastTouch <= 0L) SecurePrefs.putLong(ctx, KEY_RUN_LAST_TOUCH_TS, now)

        val last = if (lastTouch > 0L) lastTouch else now
        val delta = now - last

        if (delta > RUN_TTL_MS) {
            Log.w(TAG, "RUN stale detected. Clearing (delta=${delta}ms)")
            SecurePrefs.putBoolean(ctx, KEY_ACTIVE_RUN, false)
            SecurePrefs.remove(ctx, KEY_RUN_START_TS)
            SecurePrefs.remove(ctx, KEY_RUN_LAST_TOUCH_TS)
        }
    }
}
