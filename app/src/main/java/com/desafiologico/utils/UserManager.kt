/* package com.desafiolgico.utils

import android.content.Context
import com.desafiolgico.R

object UserManager {
    private const val PREFS_NAME = "dados_jogo"
    private const val KEY_USERNAME = "username"
    private const val KEY_PHOTO_URL = "photoUrl"
    private const val KEY_AVATAR_ID = "avatarId"

    fun salvarDadosUsuario(context: Context, nome: String?, email: String?, photoUrl: String?, avatarId: Int?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().also { editor ->
            editor.putString(KEY_USERNAME, nome ?: email ?: "Jogador")
            if (photoUrl != null) editor.putString(KEY_PHOTO_URL, photoUrl)
            if (avatarId != null) editor.putInt(KEY_AVATAR_ID, avatarId)
        }.apply()
    }

    fun carregarDadosUsuario(context: Context): Triple<String, String?, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nome = prefs.getString(KEY_USERNAME, "Jogador") ?: "Jogador"
        val fotoUrl = prefs.getString(KEY_PHOTO_URL, null)
        val avatarId = prefs.getInt(KEY_AVATAR_ID, R.drawable.avatar1)
        return Triple(nome, fotoUrl, avatarId)
    }


}
*/