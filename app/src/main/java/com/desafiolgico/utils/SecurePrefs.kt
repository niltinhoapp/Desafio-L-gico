package com.desafiolgico.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePrefs {

    private const val SECURE_PREFS_NAME = "DesafioLogicoPrefs_secure"
    private const val FALLBACK_PREFS_NAME = "DesafioLogicoPrefs_fallback"

    @Volatile
    private var cached: SharedPreferences? = null

    private fun securePrefs(context: Context): SharedPreferences {
        cached?.let { return it }

        val appCtx = context.applicationContext

        val prefs = try {
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
        } catch (_: Exception) {
            // ✅ fallback (não derruba o app se crypto falhar)
            appCtx.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
        }

        cached = prefs
        return prefs
    }

    fun get(context: Context): SharedPreferences = securePrefs(context)

    fun putString(context: Context, key: String, value: String?) {
        securePrefs(context).edit().putString(key, value).apply()
    }

    fun getString(context: Context, key: String, def: String? = null): String? {
        return securePrefs(context).getString(key, def)
    }

    fun putInt(context: Context, key: String, value: Int) {
        securePrefs(context).edit().putInt(key, value).apply()
    }

    fun getInt(context: Context, key: String, def: Int = 0): Int {
        return securePrefs(context).getInt(key, def)
    }

    fun putLong(context: Context, key: String, value: Long) {
        securePrefs(context).edit().putLong(key, value).apply()
    }

    fun getLong(context: Context, key: String, def: Long = 0L): Long {
        return securePrefs(context).getLong(key, def)
    }

    fun putBoolean(context: Context, key: String, value: Boolean) {
        securePrefs(context).edit().putBoolean(key, value).apply()
    }

    fun getBoolean(context: Context, key: String, def: Boolean = false): Boolean {
        return securePrefs(context).getBoolean(key, def)
    }

    fun putStringSet(context: Context, key: String, value: Set<String>) {
        // ✅ copia defensiva
        securePrefs(context).edit().putStringSet(key, HashSet(value)).apply()
    }

    fun getStringSet(context: Context, key: String, def: Set<String> = emptySet()): Set<String> {
        // ✅ copia defensiva
        val set = securePrefs(context).getStringSet(key, null) ?: def
        return HashSet(set)
    }

    fun remove(context: Context, key: String) {
        securePrefs(context).edit().remove(key).apply()
    }

    fun clear(context: Context) {
        securePrefs(context).edit().clear().apply()
    }
}
