package com.desafiolgico.weekly

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.math.min

object WeeklyChampionshipRepo {
    private const val TAG = "WEEKLY"

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    data class Exam(
        val questionIds: List<String>,
        val correctIndexes: List<Int>,
        val createdAt: Timestamp? = null
    )

    /**
     * Cria (1x) a prova do usuário para weekId/roundId:
     * - escolhe N questões do pool
     * - salva questionIds + correctIndexes no exam (pra submit ser local)
     */
    fun ensureExam(
        weekId: String,
        roundId: String,
        questionsPerRun: Int,
        onDone: (ok: Boolean, questionIds: List<String>) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onDone(false, emptyList())

        val baseRef = db.collection("weekly_runs").document(weekId)
            .collection("rounds").document(roundId)

        val examRef = baseRef.collection("exams").document(uid)

        examRef.get()
            .addOnSuccessListener { snap ->
                val existingIds = (snap.get("questionIds") as? List<*>)?.filterIsInstance<String>()
                val existingCorrect = (snap.get("correctIndexes") as? List<*>)?.mapNotNull {
                    when (it) {
                        is Long -> it.toInt()
                        is Int -> it
                        else -> null
                    }
                }

                if (!existingIds.isNullOrEmpty() && !existingCorrect.isNullOrEmpty()
                    && existingIds.size == existingCorrect.size
                ) {
                    onDone(true, existingIds)
                    return@addOnSuccessListener
                }

                // 1) carrega pool
                db.collection("weekly_question_pool")
                    .limit(500)
                    .get()
                    .addOnSuccessListener { pool ->
                        val docs = pool.documents.shuffled()
                        val selectedDocs = docs.take(min(questionsPerRun, docs.size))

                        val selectedIds = selectedDocs.map { it.id }
                        val correctIndexes = selectedDocs.map {
                            (it.getLong("correctIndex") ?: -1L).toInt()
                        }

                        val data = hashMapOf(
                            "questionIds" to selectedIds,
                            "correctIndexes" to correctIndexes,
                            "createdAt" to FieldValue.serverTimestamp()
                        )

                        examRef.set(data, SetOptions.merge())
                            .addOnSuccessListener { onDone(true, selectedIds) }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "ensureExam set fail", e)
                                onDone(false, emptyList())
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "ensureExam pool fail", e)
                        onDone(false, emptyList())
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "ensureExam get fail", e)
                onDone(false, emptyList())
            }
    }

    /**
     * Submit:
     * - lê exam (questionIds + correctIndexes)
     * - calcula score local comparando answers vs correctIndexes
     * - salva tentativa
     * - atualiza participant (best + attemptsUsed)
     */
    fun submitAttempt(
        weekId: String,
        roundId: String,
        answers: List<Int>,      // 0..3 (ou -1 se pulou)
        totalTimeMs: Long,
        attemptNo: Int,          // 1..3
        questionsPerRun: Int,
        onDone: (ok: Boolean, score: Int) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onDone(false, 0)

        val baseRef = db.collection("weekly_runs").document(weekId)
            .collection("rounds").document(roundId)

        val examRef = baseRef.collection("exams").document(uid)
        val attemptId = "${uid}_$attemptNo"
        val attemptRef = baseRef.collection("attempts").document(attemptId)
        val participantRef = baseRef.collection("participants").document(uid)

        examRef.get()
            .addOnSuccessListener { examSnap ->
                val correctIndexes = (examSnap.get("correctIndexes") as? List<*>)?.mapNotNull {
                    when (it) {
                        is Long -> it.toInt()
                        is Int -> it
                        else -> null
                    }
                } ?: emptyList()

                if (correctIndexes.isEmpty()) return@addOnSuccessListener onDone(false, 0)

                var score = 0
                val size = min(correctIndexes.size, answers.size)
                for (i in 0 until size) {
                    if (answers[i] == correctIndexes[i]) score++
                }

                val attemptData = hashMapOf(
                    "uid" to uid,
                    "attemptNo" to attemptNo,
                    "answers" to answers,
                    "totalTimeMs" to totalTimeMs,
                    "score" to score,
                    "questionsPerRun" to questionsPerRun,
                    "finishedAt" to FieldValue.serverTimestamp()
                )

                attemptRef.set(attemptData, SetOptions.merge())
                    .addOnSuccessListener {
                        db.runTransaction { tx ->
                            val p = tx.get(participantRef)

                            val prevBestScore = (p.getLong("bestScore") ?: -1).toInt()
                            val prevBestTime = p.getLong("bestTimeMs") ?: Long.MAX_VALUE
                            val prevAttempts = (p.getLong("attemptsUsed") ?: 0).toInt()

                            val newAttempts = maxOf(prevAttempts, attemptNo)

                            val isBetter =
                                (score > prevBestScore) ||
                                    (score == prevBestScore && totalTimeMs < prevBestTime)

                            val updates = hashMapOf<String, Any>(
                                "attemptsUsed" to newAttempts,
                                "lastScore" to score,
                                "lastTimeMs" to totalTimeMs,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )

                            if (isBetter) {
                                updates["bestScore"] = score
                                updates["bestTimeMs"] = totalTimeMs
                            }

                            tx.set(participantRef, updates, SetOptions.merge())
                        }.addOnSuccessListener {
                            onDone(true, score)
                        }.addOnFailureListener { e ->
                            Log.e(TAG, "participant tx fail", e)
                            onDone(false, score)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "attempt set fail", e)
                        onDone(false, 0)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "exam get fail", e)
                onDone(false, 0)
            }
    }
}
