package com.desafiolgico.utils

object AuthGate {
    const val ADMIN_UID = "Y8qZrDKUfZNOC1hsuVzYeXORn6w2"

    fun isAdmin(): Boolean {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        return uid == ADMIN_UID
    }
}
