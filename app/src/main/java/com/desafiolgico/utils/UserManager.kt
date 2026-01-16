package com.desafiolgico.utils

import android.content.Context
import androidx.core.content.edit

/**
 * Perfil por usuário (UID).
 * - NÃO sensível (nome, avatarId, canChangeAvatar) -> SharedPreferences normal
 * - Sensível (email, photoUrl) -> EncryptedSharedPreferences (SecurePrefs)
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

    private fun plainPrefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setCanChangeAvatar(context: Context, value: Boolean) {
        plainPrefs(context).edit { putBoolean(getUserKey(KEY_CAN_CHANGE_AVATAR), value) }
    }

    fun canChangeAvatar(context: Context): Boolean {
        return plainPrefs(context).getBoolean(getUserKey(KEY_CAN_CHANGE_AVATAR), false)
    }

    /**
     * Salva perfil.
     * - email/photoUrl vão criptografados
     * - avatarId: se null ou <=0 => remove avatar (volta a usar foto)
     *
     * Obs: "nome mostrado" continua: nome -> email -> Jogador
     */
    fun salvarDadosUsuario(
        context: Context,
        nome: String?,
        email: String?,
        photoUrl: String?,
        avatarId: Int?
    ) {
        val appCtx = context.applicationContext

        // 1) Nome/Avatar (não sensível) -> prefs normal
        val finalName = nome?.takeIf { it.isNotBlank() }
            ?: email?.takeIf { it.isNotBlank() }
            ?: "Jogador"

        plainPrefs(appCtx).edit {
            putString(getUserKey(KEY_USERNAME), finalName)

            if (avatarId != null && avatarId > 0) {
                putInt(getUserKey(KEY_AVATAR_ID), avatarId)
            } else {
                remove(getUserKey(KEY_AVATAR_ID))
            }
        }

        // 2) Email/Foto (sensível) -> prefs criptografado
        // Guest: recomendado minimizar (não salvar email/foto)
        val isGuest = sanitizeUserId(GameDataManager.currentUserId) == "guest_mode" ||
            sanitizeUserId(GameDataManager.currentUserId) == GUEST_ID

        if (isGuest) {
            SecurePrefs.remove(appCtx, getUserKey(KEY_EMAIL))
            SecurePrefs.remove(appCtx, getUserKey(KEY_PHOTO_URL))
        } else {
            SecurePrefs.putString(appCtx, getUserKey(KEY_EMAIL), email)
            SecurePrefs.putString(appCtx, getUserKey(KEY_PHOTO_URL), photoUrl)
        }
    }

    /**
     * Carrega perfil do usuário atual.
     * - email/photoUrl vêm do SecurePrefs
     * - avatarId default = 0 (não força avatar1)
     */
    fun carregarDadosUsuario(context: Context): UserProfile {
        val appCtx = context.applicationContext

        val name = plainPrefs(appCtx).getString(getUserKey(KEY_USERNAME), "Jogador") ?: "Jogador"
        val avatarId = plainPrefs(appCtx).getInt(getUserKey(KEY_AVATAR_ID), 0)

        val email = SecurePrefs.getString(appCtx, getUserKey(KEY_EMAIL), null)
        val photoUrl = SecurePrefs.getString(appCtx, getUserKey(KEY_PHOTO_URL), null)

        return UserProfile(name = name, email = email, photoUrl = photoUrl, avatarId = avatarId)
    }

    fun getNomeUsuario(context: Context): String? =
        plainPrefs(context).getString(getUserKey(KEY_USERNAME), null)

    fun getEmailUsuario(context: Context): String? =
        SecurePrefs.getString(context.applicationContext, getUserKey(KEY_EMAIL), null)

    fun limparDadosUsuario(context: Context) {
        val appCtx = context.applicationContext

        plainPrefs(appCtx).edit {
            remove(getUserKey(KEY_USERNAME))
            remove(getUserKey(KEY_AVATAR_ID))
            remove(getUserKey(KEY_CAN_CHANGE_AVATAR))
        }

        SecurePrefs.remove(appCtx, getUserKey(KEY_EMAIL))
        SecurePrefs.remove(appCtx, getUserKey(KEY_PHOTO_URL))
    }

    fun resetarTodosOsUsuarios(context: Context) {
        // ⚠️ aqui limpa o normal, mas o SecurePrefs fica com chaves antigas de usuários.
        // Se você quiser "reset total", eu te passo uma estratégia segura pra limpar por prefixo.
        plainPrefs(context).edit { clear() }
    }
}
