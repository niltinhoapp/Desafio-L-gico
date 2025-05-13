package com.desafiolgico.utils

import android.content.Context
import androidx.core.content.edit

/**
 * Gerencia o perfil (nome, email, foto e avatar) por usuário (UID).
 * avatarId = 0 => nenhum avatar escolhido (foto do Google tem prioridade).
 */
object UserManager {

    private const val PREFS_NAME = "UserProfilePrefs"

    private const val KEY_USERNAME = "username"
    private const val KEY_EMAIL = "email"
    private const val KEY_PHOTO_URL = "photoUrl"
    private const val KEY_AVATAR_ID = "avatarId"
    private const val KEY_CAN_CHANGE_AVATAR = "canChangeAvatar"

    private const val GUEST_ID = "guest"

    private fun sanitizeUserId(raw: String?): String {
        val id = raw?.trim().takeUnless { it.isNullOrBlank() } ?: GUEST_ID
        return id.replace("\\W+".toRegex(), "_")
    }

    /** Chave única por usuário */
    private fun getUserKey(key: String): String {
        val userId = sanitizeUserId(GameDataManager.currentUserId)
        return "${userId}_$key"
    }

    fun setCanChangeAvatar(context: Context, value: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(getUserKey(KEY_CAN_CHANGE_AVATAR), value) }
    }

    fun canChangeAvatar(context: Context): Boolean {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(getUserKey(KEY_CAN_CHANGE_AVATAR), false)
    }

    /**
     * Salva perfil.
     * - photoUrl pode ser null
     * - avatarId: se null ou <=0 => remove avatar (volta a usar foto)
     */
    fun salvarDadosUsuario(
        context: Context,
        nome: String?,
        email: String?,
        photoUrl: String?,
        avatarId: Int?
    ) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        prefs.edit {
            // Nome mostrado
            val finalName = nome?.takeIf { it.isNotBlank() } ?: email?.takeIf { it.isNotBlank() } ?: "Jogador"
            putString(getUserKey(KEY_USERNAME), finalName)

            // Email (pode ser null)
            putString(getUserKey(KEY_EMAIL), email)

            // Foto (pode ser null)
            putString(getUserKey(KEY_PHOTO_URL), photoUrl)

            // Avatar opcional
            if (avatarId != null && avatarId > 0) {
                putInt(getUserKey(KEY_AVATAR_ID), avatarId)
            } else {
                remove(getUserKey(KEY_AVATAR_ID))
            }
        }
    }

    /**
     * Carrega perfil do usuário atual.
     * IMPORTANTE: avatarId default = 0 (não força avatar1).
     */
    fun carregarDadosUsuario(context: Context): UserProfile {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val name = prefs.getString(getUserKey(KEY_USERNAME), "Jogador") ?: "Jogador"
        val email = prefs.getString(getUserKey(KEY_EMAIL), null)
        val photoUrl = prefs.getString(getUserKey(KEY_PHOTO_URL), null)

        // default 0 => "sem avatar escolhido"
        val avatarId = prefs.getInt(getUserKey(KEY_AVATAR_ID), 0)

        return UserProfile(name = name, email = email, photoUrl = photoUrl, avatarId = avatarId)
    }

    fun getNomeUsuario(context: Context): String? =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(getUserKey(KEY_USERNAME), null)

    fun getEmailUsuario(context: Context): String? =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(getUserKey(KEY_EMAIL), null)

    fun limparDadosUsuario(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            remove(getUserKey(KEY_USERNAME))
            remove(getUserKey(KEY_EMAIL))
            remove(getUserKey(KEY_PHOTO_URL))
            remove(getUserKey(KEY_AVATAR_ID))
            remove(getUserKey(KEY_CAN_CHANGE_AVATAR))
        }
    }

    fun resetarTodosOsUsuarios(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { clear() }
    }
}
