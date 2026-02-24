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
import java.util.Random

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
        state.attemptNo = intent.getIntExtra("ATTEMPT_NO", 1).coerceAtLeast(1)

        resetRunLocalState()
        onHudUpdate(state.correct, state.wrong)
    }

    /**
     * NÃO consome tentativa aqui.
     * A tentativa já foi consumida/registrada na WeeklyChampionshipActivity.
     * Aqui só garante as 15 perguntas fixas do attemptNo.
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

        userRef.get()
            .addOnSuccessListener { udoc ->
                if (!udoc.exists()) {
                    onFail("Você ainda não está inscrito nesta semana.")
                    state.reset()
                    return@addOnSuccessListener
                }

                val existingAttempt = (udoc.getLong("lastRun.attemptNo") ?: 0L).toInt()
                val existingIds =
                    (udoc.get("lastRun.questionIds") as? List<*>)?.filterIsInstance<String>()

                // ✅ Se já tem as 15 perguntas para ESTE attempt, só carrega
                if (existingAttempt == attemptNo &&
                    !existingIds.isNullOrEmpty() &&
                    existingIds.size == QUESTIONS_PER_RUN
                ) {
                    state.questionIds = existingIds
                    loadQuestionsByIds(existingIds, onFail)
                    return@addOnSuccessListener
                }

                // ✅ Gera perguntas determinísticas e salva
                db.collection("weekly_question_pool")
                    .whereEqualTo("active", true)
                    .limit(POOL_FETCH_LIMIT.toLong())
                    .get()
                    .addOnSuccessListener { poolSnap ->
                        val allIds = poolSnap.documents.map { it.id }
                        if (allIds.size < QUESTIONS_PER_RUN) {
                            onFail("Pool tem só ${allIds.size}. Precisa $QUESTIONS_PER_RUN+.")
                            return@addOnSuccessListener
                        }

                        val seed = ("$weekId|$uid|$attemptNo|DL").hashCode().toLong()
                        val rnd = Random(seed)
                        val picked = allIds.shuffled(rnd).take(QUESTIONS_PER_RUN)

                        state.questionIds = picked

                        // ✅ startedAt: só seta se ainda não existir (mantém a 1ª vez)
                        val startedAtExists = udoc.getTimestamp("lastRun.startedAt") != null

                        val updates = hashMapOf<String, Any>(
                            "lastRun.attemptNo" to attemptNo.toLong(),
                            "lastRun.questionIds" to picked,
                            "lastRun.finishedAt" to FieldValue.delete(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        userRef.update(updates)
                        if (!startedAtExists || existingAttempt != attemptNo) {
                            updates["lastRun.startedAt"] = FieldValue.serverTimestamp()
                        }

                        userRef.update(updates)
                            .addOnSuccessListener {
                                loadQuestionsByIds(picked, onFail)
                            }
                            .addOnFailureListener { e ->
                                onFail("Falha ao salvar tentativa: ${e.message}")
                            }
                    }
                    .addOnFailureListener { e ->
                        onFail("Erro no pool: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                onFail(e.message ?: "Falha ao iniciar weekly")
                state.reset()
            }
    }

    /**
     * Envia o resultado final do attempt (com trava anti-duplicação).
     * Regra de BEST:
     * - só se elegível (!disqualified && correct >= minCorrect)
     * - maior correct vence
     * - empate: menor timeMs vence
     */
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

            // ✅ trava anti-duplicação
            val alreadyFinished = snap.getTimestamp("lastRun.finishedAt") != null
            if (alreadyFinished) return@runTransaction "ALREADY_SUBMITTED"

            // attemptNo atual (não diminui)
            val attemptNo = (snap.getLong("lastRun.attemptNo") ?: state.attemptNo.toLong()).toInt()

            val minCorrect = (snap.getLong("rules.minCorrect") ?: DEFAULT_MIN_CORRECT.toLong()).toInt()

            val bestCorrect = (snap.getLong("best.correct") ?: -1L).toInt()
            val bestTime = (snap.getLong("best.timeMs") ?: Long.MAX_VALUE)

            val eligible = !disqualified && correct >= minCorrect
            val tMs = timeMs.coerceAtLeast(0L)

            val better =
                eligible && (
                    (correct > bestCorrect) ||
                        (correct == bestCorrect && tMs in 1 until bestTime)
                    )

            // ✅ NÃO sobrescreve lastRun inteiro (senão perde questionIds/startedAt)
            val updates = hashMapOf<String, Any>(
                "lastRun.attemptNo" to attemptNo.toLong(),
                "lastRun.finishedAt" to FieldValue.serverTimestamp(),
                "lastRun.correct" to correct.toLong(),
                "lastRun.wrong" to wrong.toLong(),
                "lastRun.timeMs" to tMs,
                "lastRun.backgroundCount" to bgCount.toLong(),
                "lastRun.backgroundTotalMs" to bgTotalMs.coerceAtLeast(0L),
                "lastRun.disqualified" to disqualified,
                "lastRun.disqualifyReason" to (if (disqualified) disqualifyReason else ""),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (better) {
                updates["best.correct"] = correct.toLong()
                updates["best.timeMs"] = tMs
                updates["best.attemptNo"] = attemptNo.toLong()
                // extra field OK (suas rules não proíbem)
                updates["best.achievedAt"] = FieldValue.serverTimestamp()
            }

            tx.update(userRef, updates)
            "OK"
        }.addOnSuccessListener { result ->
            // ✅ depois que o best foi gravado, atualiza leaderboard SEGURO
            if (result == "OK" || result == "ALREADY_SUBMITTED") {
                updateLeaderboardFromUserBest(weekId) {
                    onDone?.invoke()
                }
            } else {
                onDone?.invoke()
            }
        }.addOnFailureListener { e ->
            onFail?.invoke(e.message ?: "Falha ao enviar resultado")
        }
    }

    private fun updateLeaderboardFromUserBest(weekId: String, onDone: () -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            onDone()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("weekly_runs").document(weekId)
            .collection("users").document(uid)

        val lbRef = db.collection("weekly_runs").document(weekId)
            .collection("leaderboard").document(uid)

        userRef.get()
            .addOnSuccessListener { snap ->
                val bestCorrect = (snap.getLong("best.correct") ?: 0L)
                val bestTimeMs = (snap.getLong("best.timeMs") ?: 0L)

                // se não tem best ainda, não escreve leaderboard
                if (bestCorrect <= 0L || bestTimeMs <= 0L) {
                    onDone()
                    return@addOnSuccessListener
                }

                val data = hashMapOf<String, Any>(
                    "uid" to uid,
                    "weekId" to weekId,
                    "bestCorrect" to bestCorrect,
                    "bestTimeMs" to bestTimeMs
                )

                lbRef.set(data, SetOptions.merge())
                    .addOnSuccessListener { onDone() }
                    .addOnFailureListener { onDone() } // não bloqueia fluxo
            }
            .addOnFailureListener {
                onDone()
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
            state.backgroundTotalMs += (System.currentTimeMillis() - wentAt).coerceAtLeast(0L)
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

                // mantém a ordem exata do "ids"
                val orderedDocs = ids.mapNotNull { mapById[it] }

                val built = orderedDocs.mapNotNull { doc ->
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

                if (built.size != QUESTIONS_PER_RUN) {
                    onFail("Pool inválido (faltando perguntas).")
                    return@addOnSuccessListener
                }

                resetRunLocalState()
                onHudUpdate(state.correct, state.wrong)
                onQuestionsLoaded(built)
            }
            .addOnFailureListener { e ->
                onFail("Erro ao carregar perguntas: ${e.message}")
            }
    }

    private fun resetRunLocalState() {
        state.correct = 0
        state.wrong = 0
        state.startedAtMs = 0L
        state.finishedAtMs = 0L
        state.backgroundCount = 0
        state.backgroundTotalMs = 0L
        state.wentBackgroundAtMs = 0L
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

    /**
     * Opcional: se você usar este método, ele deve ser o ÚNICO responsável por finalizar.
     * Se você já finaliza no QuizEngine, pode remover este método do seu projeto pra evitar duplicidade.
     */
}
