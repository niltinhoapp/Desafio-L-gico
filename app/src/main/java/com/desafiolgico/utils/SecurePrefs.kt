package com.desafiolgico.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePrefs {

    private const val SECURE_PREFS_NAME = "DesafioLogicoPrefs_secure"

    private var cached: SharedPreferences? = null

    private fun securePrefs(context: Context): SharedPreferences {
        cached?.let { return it }

        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        cached = prefs
        return prefs
    }

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

    fun putBoolean(context: Context, key: String, value: Boolean) {
        securePrefs(context).edit().putBoolean(key, value).apply()
    }

    fun getBoolean(context: Context, key: String, def: Boolean = false): Boolean {
        return securePrefs(context).getBoolean(key, def)
    }

    fun putStringSet(context: Context, key: String, value: Set<String>) {
        securePrefs(context).edit().putStringSet(key, value).apply()
    }

    fun getStringSet(context: Context, key: String, def: Set<String> = emptySet()): Set<String> {
        return securePrefs(context).getStringSet(key, def) ?: def
    }

    fun remove(context: Context, key: String) {
        securePrefs(context).edit().remove(key).apply()
    }
    fun get(context: Context): SharedPreferences = securePrefs(context)

}
