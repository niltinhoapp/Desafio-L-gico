package com.desafiolgico.weekly

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.desafiolgico.model.Question
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions

class WeeklyController(
    private val activity: AppCompatActivity,
    private val state: WeeklyState,
    private val onQuestionsLoaded: (List<Question>) -> Unit,
    private val onRankingOpen: (weekId: String) -> Unit,
    private val onHudUpdate: (correct: Int, wrong: Int) -> Unit
) {

    companion object {
        private const val QUESTIONS_PER_RUN = 15
        private const val POOL_FETCH_LIMIT = 300
        private const val DEFAULT_MIN_CORRECT = 13
    }

    fun isWeeklyMode(): Boolean = state.enabled
    fun weekId(): String = state.weekId

    fun beginFromIntent(intent: Intent) {
        state.enabled = true
        state.weekId = intent.getStringExtra("WEEK_ID").orEmpty()
        state.roundId = intent.getStringExtra("ROUND_ID") ?: "R1"
        state.attemptNo = intent.getIntExtra("ATTEMPT_NO", 1)

        state.correct = 0
        state.wrong = 0
        state.startedAtMs = 0L
        state.finishedAtMs = 0L
        state.backgroundCount = 0
        state.backgroundTotalMs = 0L
        state.wentBackgroundAtMs = 0L
        state.questionIds = emptyList()

        onHudUpdate(state.correct, state.wrong)
    }

    /**
     * 🔥 Agora NÃO reserva tentativa aqui.
     * A tentativa já foi consumida e marcada (attemptNo) na WeeklyChampionshipActivity.
     * Aqui só carrega (ou cria) as 15 perguntas fixas dessa tentativa.
     */
    fun startWeeklyMode(onFail: (String) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            onFail("Faça login para participar do ranking.")
            state.reset()
            return
        }

        val weekId = state.weekId
        if (weekId.isBlank()) {
            onFail("Semana inválida.")
            state.reset()
            return
        }

        val attemptNo = state.attemptNo.coerceAtLeast(1)

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("weekly_runs").document(weekId)
            .collection("users").document(uid)

        // 1) tenta reaproveitar ids já salvas para esse attemptNo
        userRef.get().addOnSuccessListener { udoc ->
            val existingAttempt = (udoc.getLong("lastRun.attemptNo") ?: 0L).toInt()
            val existingIds =
                (udoc.get("lastRun.questionIds") as? List<*>)?.filterIsInstance<String>()

            if (existingAttempt == attemptNo &&
                !existingIds.isNullOrEmpty() &&
                existingIds.size == QUESTIONS_PER_RUN
            ) {
                state.questionIds = existingIds
                loadQuestionsByIds(existingIds, onFail)
                return@addOnSuccessListener
            }

            // 2) gerar 15 ids fixas e salvar no lastRun (sem mexer em attemptsUsed)
            db.collection("weekly_question_pool")
                .whereEqualTo("active", true)
                .limit(POOL_FETCH_LIMIT.toLong())
                .get()
                .addOnSuccessListener { snap ->
                    val allIds = snap.documents.map { it.id }
                    if (allIds.size < QUESTIONS_PER_RUN) {
                        onFail("Pool tem só ${allIds.size}. Precisa $QUESTIONS_PER_RUN+.")
                        return@addOnSuccessListener
                    }

                    val seed = (weekId + "|" + uid + "|" + attemptNo + "|DL").hashCode().toLong()
                    val rnd = java.util.Random(seed)
                    val picked = allIds.shuffled(rnd).take(QUESTIONS_PER_RUN)

                    state.questionIds = picked

                    userRef.set(
                        mapOf(
                            "lastRun" to mapOf(
                                "attemptNo" to attemptNo.toLong(),
                                "questionIds" to picked,
                                "startedAt" to FieldValue.serverTimestamp()
                            ),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    ).addOnSuccessListener {
                        loadQuestionsByIds(picked, onFail)
                    }.addOnFailureListener { e ->
                        onFail("Falha ao salvar tentativa: ${e.message}")
                    }
                }
                .addOnFailureListener { e ->
                    onFail("Erro no pool: ${e.message}")
                }
        }.addOnFailureListener { e ->
            onFail(e.message ?: "Falha ao iniciar weekly")
            state.reset()
        }
    }

    fun submitWeeklyResult(
        weekId: String,
        correct: Int,
        wrong: Int,
        timeMs: Long,
        bgCount: Int,
        bgTotalMs: Long,
        disqualified: Boolean,
        disqualifyReason: String,
        onDone: (() -> Unit)? = null,
        onFail: ((String) -> Unit)? = null
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            onFail?.invoke("Faça login para enviar o resultado.")
            return
        }

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("weekly_runs").document(weekId)
            .collection("users").document(uid)

        db.runTransaction { tx ->
            val snap = tx.get(userRef)
            if (!snap.exists()) throw IllegalStateException("Usuário não inscrito nesta semana.")

            val attemptNo = (snap.getLong("lastRun.attemptNo") ?: state.attemptNo.toLong()).toInt()
            val minCorrect = (snap.getLong("rules.minCorrect") ?: DEFAULT_MIN_CORRECT.toLong()).toInt()

            val bestCorrect = (snap.getLong("best.correct") ?: -1L).toInt()
            val bestTime = (snap.getLong("best.timeMs") ?: Long.MAX_VALUE)

            val eligible = !disqualified && correct >= minCorrect

            val lastRunMap = mapOf(
                "attemptNo" to attemptNo.toLong(),
                "finishedAt" to FieldValue.serverTimestamp(),
                "correct" to correct.toLong(),
                "wrong" to wrong.toLong(),
                "timeMs" to timeMs.coerceAtLeast(0L),
                "backgroundCount" to bgCount.toLong(),
                "backgroundTotalMs" to bgTotalMs.coerceAtLeast(0L),
                "disqualified" to disqualified,
                "disqualifyReason" to (if (disqualified) disqualifyReason else "")
            )

            val updates = mutableMapOf<String, Any>(
                "lastRun" to lastRunMap,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (eligible) {
                val better =
                    (correct > bestCorrect) ||
                        (correct == bestCorrect && timeMs in 1 until bestTime)

                if (better) {
                    updates["best"] = mapOf(
                        "correct" to correct.toLong(),
                        "timeMs" to timeMs.coerceAtLeast(0L),
                        "attemptNo" to attemptNo.toLong(),
                        "achievedAt" to FieldValue.serverTimestamp()
                    )
                }
            }

            tx.set(userRef, updates, SetOptions.merge())
            "OK"
        }.addOnSuccessListener {
            onDone?.invoke()
        }.addOnFailureListener { e ->
            onFail?.invoke(e.message ?: "Falha ao enviar resultado")
        }
    }
    fun onStop() {
        if (!state.enabled) return
        state.wentBackgroundAtMs = System.currentTimeMillis()
        state.backgroundCount += 1
    }

    fun onStart() {
        if (!state.enabled) return
        val wentAt = state.wentBackgroundAtMs
        if (wentAt > 0L) {
            state.backgroundTotalMs += (System.currentTimeMillis() - wentAt)
            state.wentBackgroundAtMs = 0L
        }
    }

    private fun loadQuestionsByIds(ids: List<String>, onFail: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        val chunks = ids.chunked(10)
        val tasks = chunks.map { part ->
            db.collection("weekly_question_pool")
                .whereIn(FieldPath.documentId(), part)
                .get()
        }

        Tasks.whenAllSuccess<QuerySnapshot>(tasks)
            .addOnSuccessListener { snaps ->
                val docs = snaps.flatMap { it.documents }
                val mapById = docs.associateBy { it.id }
                val ordered = ids.mapNotNull { mapById[it] }

                val built: List<Question> = ordered.mapNotNull { doc ->
                    val text = doc.getString("text") ?: return@mapNotNull null
                    val options = (doc.get("options") as? List<*>)?.filterIsInstance<String>()
                        ?: return@mapNotNull null
                    val correctIdx = (doc.getLong("correctIndex") ?: -1L).toInt()
                    if (correctIdx !in options.indices) return@mapNotNull null

                    Question(
                        questionText = text,
                        options = options,
                        correctAnswerIndex = correctIdx
                    )
                }

                if (built.size < QUESTIONS_PER_RUN) {
                    onFail("Pool inválido (faltando perguntas).")
                    return@addOnSuccessListener
                }

                state.correct = 0
                state.wrong = 0
                state.startedAtMs = 0L
                state.finishedAtMs = 0L
                state.backgroundCount = 0
                state.backgroundTotalMs = 0L
                state.wentBackgroundAtMs = 0L

                onHudUpdate(state.correct, state.wrong)
                onQuestionsLoaded(built)
            }
            .addOnFailureListener { e ->
                onFail("Erro ao carregar perguntas: ${e.message}")
            }
    }

    fun onFirstQuestionShown() {
        if (!state.enabled) return
        if (state.startedAtMs == 0L) state.startedAtMs = System.currentTimeMillis()
    }

    fun recordAnswer(isCorrect: Boolean, spentMs: Long) {
        if (!state.enabled) return
        if (state.startedAtMs == 0L) state.startedAtMs = System.currentTimeMillis()
        if (isCorrect) state.correct += 1 else state.wrong += 1
        onHudUpdate(state.correct, state.wrong)
    }

    fun finishIfEndedIfNeeded(
        currentIndex: Int,
        total: Int,
        submit: (
            weekId: String,
            correct: Int,
            wrong: Int,
            timeMs: Long,
            bgCount: Int,
            bgTotalMs: Long
        ) -> Unit
    ): Boolean {
        if (!state.enabled) return false
        if (currentIndex < total) return false

        if (state.finishedAtMs == 0L) state.finishedAtMs = System.currentTimeMillis()
        if (state.startedAtMs == 0L) state.startedAtMs = state.finishedAtMs

        val timeMs = (state.finishedAtMs - state.startedAtMs).coerceAtLeast(0L)

        submit(
            state.weekId,
            state.correct,
            state.wrong,
            timeMs,
            state.backgroundCount,
            state.backgroundTotalMs
        )

        Toast.makeText(
            activity,
            "✅ Resultado enviado! Acertos: ${state.correct} • Tempo conta.",
            Toast.LENGTH_LONG
        ).show()

        val week = state.weekId
        state.reset()
        onRankingOpen(week)
        return true
    }
}
