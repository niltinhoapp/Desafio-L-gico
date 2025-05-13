package com.desafiolgico.utils

/**
 * Perfil do usuário atual.
 * avatarId = 0 significa: "nenhum avatar escolhido" (use foto se existir).
 */
data class UserProfile(
    val name: String,
    val email: String? = null,
    val photoUrl: String? = null,
    val avatarId: Int = 0
)
