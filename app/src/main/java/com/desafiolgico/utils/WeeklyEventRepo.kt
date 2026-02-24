package com.desafiolgico.utils

import com.desafiolgico.model.WeeklyEventConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object WeeklyEventRepo {
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /**
     * Escuta o documento weekly_events/current em tempo real.
     * Retorna o ListenerRegistration para você remover quando quiser (ex: onStop/onDestroy).
     */
    fun listenCurrent(onChange: (WeeklyEventConfig?) -> Unit): ListenerRegistration {
        return db.collection("weekly_events").document("current")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null || !snap.exists()) {
                    onChange(null)
                    return@addSnapshotListener
                }

                onChange(
                    WeeklyEventConfig(
                        active = snap.getBoolean("active") ?: false,
                        weekId = snap.getString("weekId") ?: "",
                        attemptLimit = (snap.getLong("attemptLimit") ?: 3L).toInt(),
                        minCorrect = (snap.getLong("minCorrect") ?: 13L).toInt()
                    )
                )
            }
    }
}
