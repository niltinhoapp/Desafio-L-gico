package com.desafiolgico.main

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object WeeklyEvent {
    var active: Boolean = false
        private set

    var currentWeekId: String? = null
        private set

    var minCorrect: Int = 13
        private set

    var questionsPerRun: Int = 15
        private set

    var maxBackgroundSeconds: Int = 3
        private set

    var attemptLimit: Int = 3
        private set

    private fun applyDefaults() {
        active = false
        currentWeekId = null
        minCorrect = 13
        questionsPerRun = 15
        maxBackgroundSeconds = 3
        attemptLimit = 3
    }

    private fun applyFromDoc(
        activeValue: Boolean?,
        weekIdValue: String?,
        minCorrectValue: Long?,
        questionsPerRunValue: Long?,
        maxBackgroundSecondsValue: Long?,
        attemptLimitValue: Long?
    ) {
        active = activeValue ?: false
        currentWeekId = weekIdValue

        minCorrect = (minCorrectValue ?: 13L).toInt()
        questionsPerRun = (questionsPerRunValue ?: 15L).toInt()
        maxBackgroundSeconds = (maxBackgroundSecondsValue ?: 3L).toInt()
        attemptLimit = (attemptLimitValue ?: 3L).toInt()
    }

    /**
     * Busca 1x (não fica escutando).
     * Bom pra chamar no onCreate e seguir o fluxo.
     */
    fun refresh(onDone: (() -> Unit)? = null) {
        val db = FirebaseFirestore.getInstance()
        db.collection("weekly_events")
            .document("current")
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    applyDefaults()
                    onDone?.invoke()
                    return@addOnSuccessListener
                }

                applyFromDoc(
                    activeValue = doc.getBoolean("active"),
                    weekIdValue = doc.getString("weekId"),
                    minCorrectValue = doc.getLong("minCorrect"),
                    questionsPerRunValue = doc.getLong("questionsPerRun"),
                    maxBackgroundSecondsValue = doc.getLong("maxBackgroundSeconds"),
                    attemptLimitValue = doc.getLong("attemptLimit")
                )

                onDone?.invoke()
            }
            .addOnFailureListener {
                // se falhar, não quebra o jogo
                applyDefaults()
                onDone?.invoke()
            }
    }

    // =========================
    // REALTIME LISTENER (opcional)
    // =========================

    private var reg: ListenerRegistration? = null

    /**
     * Escuta em tempo real o documento weekly_events/current.
     * Chame startListening() no onStart/onResume e stopListening() no onStop/onDestroy.
     */
    fun startListening(onChanged: (() -> Unit)? = null) {
        if (reg != null) return // já está ouvindo

        val db = FirebaseFirestore.getInstance()
        reg = db.collection("weekly_events")
            .document("current")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null || !snap.exists()) {
                    applyDefaults()
                    onChanged?.invoke()
                    return@addSnapshotListener
                }

                applyFromDoc(
                    activeValue = snap.getBoolean("active"),
                    weekIdValue = snap.getString("weekId"),
                    minCorrectValue = snap.getLong("minCorrect"),
                    questionsPerRunValue = snap.getLong("questionsPerRun"),
                    maxBackgroundSecondsValue = snap.getLong("maxBackgroundSeconds"),
                    attemptLimitValue = snap.getLong("attemptLimit")
                )

                onChanged?.invoke()
            }
    }

    fun stopListening() {
        reg?.remove()
        reg = null
    }
}
