package com.desafiolgico.utils

import android.content.Context
import androidx.core.content.edit

/**
 * Perfil por usuário (UID).
 * - NÃO sensível (nome, avatarId, canChangeAvatar) -> SharedPreferences normal
 * - Sensível (email, photoUrl) -> SecurePrefs (EncryptedSharedPreferences com fallback)
 */
object UserManager {

    private const val PREFS_NAME = "UserProfilePrefs"

    private const val KEY_USERNAME = "username"
    private const val KEY_EMAIL = "email"
    private const val KEY_PHOTO_URL = "photoUrl"
    private const val KEY_AVATAR_ID = "avatarId"
    private const val KEY_CAN_CHANGE_AVATAR = "canChangeAvatar"

    private const val GUEST_ID = "guest"
    private const val DEFAULT_NAME = "Jogador"

    // =========================
    // Helpers (UID / keys)
    // =========================

    private fun sanitizeUserId(raw: String?): String {
        val id = raw?.trim().takeUnless { it.isNullOrBlank() } ?: GUEST_ID
        // mantém curto e seguro para chaves
        return id.replace("\\W+".toRegex(), "_").take(80)
    }

    private fun currentUserIdSafe(): String = sanitizeUserId(GameDataManager.currentUserId)

    private fun isGuestUser(userIdSafe: String = currentUserIdSafe()): Boolean {
        val u = userIdSafe.lowercase()
        return u == GUEST_ID || u == "guest_mode" || u.startsWith("guest")
    }

    private fun userKey(key: String): String = "${currentUserIdSafe()}_$key"

    private fun plainPrefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // =========================
    // Public API
    // =========================

    fun setCanChangeAvatar(context: Context, value: Boolean) {
        plainPrefs(context).edit { putBoolean(userKey(KEY_CAN_CHANGE_AVATAR), value) }
    }

    fun canChangeAvatar(context: Context): Boolean {
        return plainPrefs(context).getBoolean(userKey(KEY_CAN_CHANGE_AVATAR), false)
    }

    /**
     * Salva perfil (compatível com seu código atual).
     * - email/photoUrl vão criptografados (SecurePrefs)
     * - avatarId: null ou <=0 => remove (volta a usar foto)
     * - nome mostrado: nome -> email -> "Jogador"
     */
    fun salvarDadosUsuario(
        context: Context,
        nome: String?,
        email: String?,
        photoUrl: String?,
        avatarId: Int?
    ) {
        val appCtx = context.applicationContext
        val safeUid = currentUserIdSafe()

        val emailClean = email?.trim().takeUnless { it.isNullOrBlank() }
        val photoClean = photoUrl?.trim().takeUnless { it.isNullOrBlank() }
        val nameClean = nome?.trim().takeUnless { it.isNullOrBlank() }

        val finalName = nameClean ?: emailClean ?: DEFAULT_NAME

        // 1) Não-sensível -> prefs normal
        plainPrefs(appCtx).edit {
            putString("${safeUid}_$KEY_USERNAME", finalName)

            val aId = avatarId ?: 0
            if (aId > 0) putInt("${safeUid}_$KEY_AVATAR_ID", aId)
            else remove("${safeUid}_$KEY_AVATAR_ID")
        }

        // 2) Sensível -> SecurePrefs (guest: minimiza armazenamento)
        if (isGuestUser(safeUid)) {
            SecurePrefs.remove(appCtx, "${safeUid}_$KEY_EMAIL")
            SecurePrefs.remove(appCtx, "${safeUid}_$KEY_PHOTO_URL")
        } else {
            SecurePrefs.putString(appCtx, "${safeUid}_$KEY_EMAIL", emailClean)
            SecurePrefs.putString(appCtx, "${safeUid}_$KEY_PHOTO_URL", photoClean)
        }
    }

    /**
     * Carrega perfil do usuário atual.
     * - email/photoUrl vêm do SecurePrefs
     * - avatarId default = 0 (não força avatar1)
     */
    fun carregarDadosUsuario(context: Context): UserProfile {
        val appCtx = context.applicationContext
        val safeUid = currentUserIdSafe()

        val name = plainPrefs(appCtx).getString("${safeUid}_$KEY_USERNAME", DEFAULT_NAME) ?: DEFAULT_NAME
        val avatarId = plainPrefs(appCtx).getInt("${safeUid}_$KEY_AVATAR_ID", 0)

        val email = SecurePrefs.getString(appCtx, "${safeUid}_$KEY_EMAIL", null)
        val photoUrl = SecurePrefs.getString(appCtx, "${safeUid}_$KEY_PHOTO_URL", null)

        return UserProfile(
            name = name,
            email = email,
            photoUrl = photoUrl,
            avatarId = avatarId
        )
    }

    fun getNomeUsuario(context: Context): String? {
        val safeUid = currentUserIdSafe()
        return plainPrefs(context).getString("${safeUid}_$KEY_USERNAME", null)
    }

    fun getEmailUsuario(context: Context): String? {
        val appCtx = context.applicationContext
        val safeUid = currentUserIdSafe()
        return SecurePrefs.getString(appCtx, "${safeUid}_$KEY_EMAIL", null)
    }

    fun getPhotoUrl(context: Context): String? {
        val appCtx = context.applicationContext
        val safeUid = currentUserIdSafe()
        return SecurePrefs.getString(appCtx, "${safeUid}_$KEY_PHOTO_URL", null)
    }

    fun getAvatarId(context: Context): Int {
        val safeUid = currentUserIdSafe()
        return plainPrefs(context).getInt("${safeUid}_$KEY_AVATAR_ID", 0)
    }

    fun setAvatarId(context: Context, avatarId: Int?) {
        val appCtx = context.applicationContext
        val safeUid = currentUserIdSafe()
        plainPrefs(appCtx).edit {
            val a = avatarId ?: 0
            if (a > 0) putInt("${safeUid}_$KEY_AVATAR_ID", a)
            else remove("${safeUid}_$KEY_AVATAR_ID")
        }
    }

    fun limparDadosUsuario(context: Context) {
        val appCtx = context.applicationContext
        val safeUid = currentUserIdSafe()

        plainPrefs(appCtx).edit {
            remove("${safeUid}_$KEY_USERNAME")
            remove("${safeUid}_$KEY_AVATAR_ID")
            remove("${safeUid}_$KEY_CAN_CHANGE_AVATAR")
        }

        SecurePrefs.remove(appCtx, "${safeUid}_$KEY_EMAIL")
        SecurePrefs.remove(appCtx, "${safeUid}_$KEY_PHOTO_URL")
    }

    /**
     * ⚠️ Limpa SOMENTE o prefs normal (não-sensível) de TODOS.
     * SecurePrefs mantém dados criptografados antigos (por design).
     */
    fun resetarTodosOsUsuarios(context: Context) {
        plainPrefs(context.applicationContext).edit { clear() }
    }
}
