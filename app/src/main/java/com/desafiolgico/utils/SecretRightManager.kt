package com.desafiolgico.utils

import android.content.Context

/**
 * "Direito" do jogador:
 * - Pode ser ganho ao desbloquear fase secreta (mas NÃO inicia automaticamente)
 * - Pode ser usado depois para reviver (volta para maxErrors-1)
 */
object SecretRightManager {

    private const val KEY_RIGHT_AVAILABLE = "secret_right_available"
    private const val KEY_RIGHT_LEVEL_ID = "secret_right_level_id" // opcional (qual secreta)
    private const val KEY_RIGHT_EARNED_AT = "secret_right_earned_at" // debug/controle

    private fun userKey(raw: String): String {
        val uid = (GameDataManager.currentUserId ?: "guest").replace("\\W+".toRegex(), "_")
        return "${uid}_$raw"
    }

    fun hasRight(ctx: Context): Boolean =
        SecurePrefs.getBoolean(ctx, userKey(KEY_RIGHT_AVAILABLE), false)

    fun getPendingSecretLevelId(ctx: Context): String? =
        SecurePrefs.getString(ctx, userKey(KEY_RIGHT_LEVEL_ID), null)

    fun grantRight(ctx: Context, secretLevelId: String?) {
        // Se já tem, não acumula (1 direito no máximo)
        if (hasRight(ctx)) return

        SecurePrefs.putBoolean(ctx, userKey(KEY_RIGHT_AVAILABLE), true)
        SecurePrefs.putString(ctx, userKey(KEY_RIGHT_LEVEL_ID), secretLevelId ?: "")
        SecurePrefs.putLong(ctx, userKey(KEY_RIGHT_EARNED_AT), System.currentTimeMillis())
    }

    fun clearRight(ctx: Context) {
        SecurePrefs.putBoolean(ctx, userKey(KEY_RIGHT_AVAILABLE), false)
        SecurePrefs.remove(ctx, userKey(KEY_RIGHT_LEVEL_ID))
        SecurePrefs.remove(ctx, userKey(KEY_RIGHT_EARNED_AT))
    }

    /** Usa o direito para REVIVER: consome e retorna o novo contador de erros (= maxErrors-1). */
    fun consumeForRevive(ctx: Context, maxErrors: Int): Int {
        clearRight(ctx)
        return (maxErrors - 1).coerceAtLeast(0)
    }

    /** Troca o direito por zerar erros imediatamente (consome direito). */
    fun consumeToClearErrors(ctx: Context) {
        clearRight(ctx)
    }
}
