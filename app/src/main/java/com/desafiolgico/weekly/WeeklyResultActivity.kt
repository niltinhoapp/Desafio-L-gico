package com.desafiolgico.weekly

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.desafiolgico.databinding.ActivityWeeklyResultBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class WeeklyResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeeklyResultBinding
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var weekId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWeeklyResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        weekId = intent.getStringExtra("WEEK_ID").orEmpty()
        if (weekId.isBlank()) {
            Toast.makeText(this, "Semana inválida.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.txtWeek.text = "Semana: $weekId"
        load()
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        val a = if (loading) 0.65f else 1f
        binding.cardStatus.alpha = a
        binding.cardBest.alpha = a
        binding.cardLast.alpha = a
    }

    private fun load() {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            renderLoggedOut()
            return
        }

        setLoading(true)

        val userRef = db.collection("weekly_runs").document(weekId)
            .collection("users").document(uid)

        userRef.get()
            .addOnSuccessListener { doc ->
                setLoading(false)

                if (!doc.exists()) {
                    renderNotEnrolled()
                    return@addOnSuccessListener
                }

                // -------------------------
                // Tentativas / regras / status
                // -------------------------
                val attemptLimit = (doc.getLong("attemptLimit") ?: 0L).toInt()
                val attemptsUsed = (doc.getLong("attemptsUsed") ?: 0L).toInt()

                binding.txtAttempts.text =
                    if (attemptLimit > 0) "Tentativas: $attemptsUsed/$attemptLimit"
                    else "Tentativas: --"

                val rules = doc.get("rules") as? Map<*, *>
                val minCorrect = (rules?.get("minCorrect") as? Number)?.toInt() ?: 13
                binding.txtMin.text = "Mínimo p/ ranking: $minCorrect"

                val banned = doc.getBoolean("bannedThisWeek") == true
                binding.txtStatus.text = when {
                    banned -> "Status: bloqueado nesta semana"
                    attemptLimit > 0 && attemptsUsed >= attemptLimit -> "Status: tentativas esgotadas"
                    else -> "Status: inscrito ✅"
                }

                // -------------------------
                // BEST
                // -------------------------
                val best = doc.get("best") as? Map<*, *>
                val bestCorrect = (best?.get("correct") as? Number)?.toInt() ?: 0
                val bestTimeMs = (best?.get("timeMs") as? Number)?.toLong() ?: -1L
                val bestAttemptNo = (best?.get("attemptNo") as? Number)?.toInt() ?: 0

                binding.txtBest.text =
                    if (bestCorrect > 0 && bestTimeMs > 0) {
                        "Melhor: $bestCorrect • ${formatMs(bestTimeMs)} • (tentativa $bestAttemptNo)"
                    } else {
                        "Melhor: --"
                    }

                // -------------------------
                // LAST RUN
                // -------------------------
                val last = doc.get("lastRun") as? Map<*, *>
                val lastAttemptNo = (last?.get("attemptNo") as? Number)?.toInt() ?: 0
                val lastCorrect = (last?.get("correct") as? Number)?.toInt() ?: 0
                val lastWrong = (last?.get("wrong") as? Number)?.toInt() ?: 0
                val lastTimeMs = (last?.get("timeMs") as? Number)?.toLong() ?: -1L
                val disq = last?.get("disqualified") as? Boolean ?: false
                val reason = last?.get("disqualifyReason") as? String ?: ""
                val startedAt = last?.get("startedAt") as? Timestamp
                val finishedAt = last?.get("finishedAt") as? Timestamp

                val hasLast = lastAttemptNo > 0 || lastCorrect > 0 || lastWrong > 0 || lastTimeMs > 0

                binding.txtLast.text = if (hasLast) {
                    val t = if (lastTimeMs > 0) formatMs(lastTimeMs) else "--"
                    val s = formatTs(startedAt)
                    val f = formatTs(finishedAt)
                    val dq = if (disq) " • DESCLASSIFICADO(${if (reason.isBlank()) "MOTIVO" else reason})" else ""
                    "Última: T$lastAttemptNo • $lastCorrect acertos • $lastWrong erros • $t$dq\nInício: $s  Fim: $f"
                } else {
                    "Última: --"
                }

                // -------------------------
                // ✅ CACHE (para aparecer em Recordes)
                // -------------------------
                val bestLine =
                    if (bestCorrect > 0 && bestTimeMs > 0) "Melhor: $bestCorrect • ${formatMs(bestTimeMs)}"
                    else "Melhor: --"

                val lastLine =
                    if (hasLast) {
                        val t = if (lastTimeMs > 0) formatMs(lastTimeMs) else "--"
                        val dq = if (disq) " • DQ(${if (reason.isBlank()) "MOTIVO" else reason})" else ""
                        "Última: T$lastAttemptNo • $lastCorrect acertos • $t$dq"
                    } else {
                        "Última: --"
                    }

                val statusLine = binding.txtStatus.text?.toString().orEmpty()
                val summary = "Semana $weekId\n$statusLine\n$bestLine\n$lastLine"

                WeeklyLocalCache.save(this, weekId, summary)
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Erro ao carregar: ${e.message}", Toast.LENGTH_LONG).show()
                binding.txtStatus.text = "Status: erro ao carregar"
            }
    }

    private fun renderLoggedOut() {
        binding.txtStatus.text = "Status: faça login"
        binding.txtAttempts.text = "Tentativas: --"
        binding.txtMin.text = "Mínimo p/ ranking: --"
        binding.txtBest.text = "Melhor: --"
        binding.txtLast.text = "Última: --"
    }

    private fun renderNotEnrolled() {
        binding.txtStatus.text = "Status: você não está inscrito"
        binding.txtAttempts.text = "Tentativas: --"
        binding.txtMin.text = "Mínimo p/ ranking: --"
        binding.txtBest.text = "Melhor: --"
        binding.txtLast.text = "Última: --"
    }

    private fun formatTs(ts: Timestamp?): String {
        if (ts == null) return "--"
        val sdf = SimpleDateFormat("dd/MM 'às' HH:mm", Locale("pt", "BR"))
        return sdf.format(ts.toDate())
    }

    private fun formatMs(ms: Long): String {
        if (ms <= 0) return "--"
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format(Locale("pt", "BR"), "%02d:%02d", min, sec)
    }
}
