package com.desafiolgico.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.concurrent.atomic.AtomicReference

/**
 * SecurePrefs (PRO / fail-safe):
 * - Usa EncryptedSharedPreferences quando disponível
 * - Fallback automático para SharedPreferences normal (sem crash)
 * - Cache thread-safe (AtomicReference)
 * - Cópias defensivas para StringSet
 * - Helpers completos (String/Int/Long/Boolean/StringSet)
 */
object SecurePrefs {

    private const val TAG = "SecurePrefs"

    private const val SECURE_PREFS_NAME = "DesafioLogicoPrefs_secure"
    private const val FALLBACK_PREFS_NAME = "DesafioLogicoPrefs_fallback"

    private val cached = AtomicReference<SharedPreferences?>(null)

    private fun buildSecure(appCtx: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(appCtx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                appCtx,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "EncryptedSharedPreferences falhou. Usando fallback.", e)
            appCtx.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun prefs(context: Context): SharedPreferences {
        cached.get()?.let { return it }

        val appCtx = context.applicationContext
        val created = buildSecure(appCtx)

        // garante singleton thread-safe
        if (cached.compareAndSet(null, created)) return created
        return cached.get() ?: created
    }

    fun get(context: Context): SharedPreferences = prefs(context)

    // =========================
    // Put / Get
    // =========================

    fun putString(context: Context, key: String, value: String?) {
        prefs(context).edit().putString(key, value).apply()
    }

    fun getString(context: Context, key: String, def: String? = null): String? {
        return prefs(context).getString(key, def)
    }

    fun putInt(context: Context, key: String, value: Int) {
        prefs(context).edit().putInt(key, value).apply()
    }

    fun getInt(context: Context, key: String, def: Int = 0): Int {
        return prefs(context).getInt(key, def)
    }

    fun putLong(context: Context, key: String, value: Long) {
        prefs(context).edit().putLong(key, value).apply()
    }

    fun getLong(context: Context, key: String, def: Long = 0L): Long {
        return prefs(context).getLong(key, def)
    }

    fun putBoolean(context: Context, key: String, value: Boolean) {
        prefs(context).edit().putBoolean(key, value).apply()
    }

    fun getBoolean(context: Context, key: String, def: Boolean = false): Boolean {
        return prefs(context).getBoolean(key, def)
    }

    fun putStringSet(context: Context, key: String, value: Set<String>) {
        // cópia defensiva (SharedPreferences pode manter referência interna)
        prefs(context).edit().putStringSet(key, HashSet(value)).apply()
    }

    fun getStringSet(context: Context, key: String, def: Set<String> = emptySet()): Set<String> {
        // cópia defensiva (para não vazar mutabilidade)
        val set = prefs(context).getStringSet(key, null) ?: def
        return HashSet(set)
    }

    // =========================
    // Remove / Clear
    // =========================

    fun remove(context: Context, key: String) {
        prefs(context).edit().remove(key).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    // =========================
    // Util
    // =========================

    /**
     * Se em algum cenário você quiser forçar recriar o prefs
     * (ex: depois de atualizar libs de crypto / migração).
     */
    fun resetCache() {
        cached.set(null)
    }
}
