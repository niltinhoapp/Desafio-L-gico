package com.desafiolgico.weekly

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.desafiolgico.databinding.ActivityWeeklyResultBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import kotlin.math.max

class WeeklyResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeeklyResultBinding
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var weekId: String = ""

    // defaults (só fallback caso o doc do user esteja incompleto)
    private var attemptLimit: Int = 3
    private var questionsPerRun: Int = 15
    private var minCorrect: Int = 13

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeeklyResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        weekId = intent.getStringExtra("WEEK_ID").orEmpty()
        if (weekId.isBlank()) {
            toast("Semana inválida.")
            finish()
            return
        }

        // já mostra o id
        binding.txtWeek.text = "Semana: $weekId"

        loadStrictFromUserDoc()
    }

    /**
     * ✅ FIEL: lê tudo do doc oficial do usuário na semana:
     * weekly_runs/{weekId}/users/{uid}
     *
     * (Não recalcula desclassificação na UI, apenas exibe o que foi salvo.)
     */
    private fun loadStrictFromUserDoc() {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            toast("Faça login.")
            finish()
            return
        }

        setLoading(true)

        val userRef = db.collection("weekly_runs").document(weekId)
            .collection("users").document(uid)

        userRef.get()
            .addOnSuccessListener { snap ->
                setLoading(false)

                if (!snap.exists()) {
                    renderNotRegistered()
                    return@addOnSuccessListener
                }

                // ✅ regras/limites vindos do doc do user (fiel)
                attemptLimit = (snap.getLong("attemptLimit") ?: attemptLimit.toLong()).toInt()
                minCorrect = (snap.getLong("rules.minCorrect") ?: minCorrect.toLong()).toInt()

                // (se você não salvar questionsPerRun no Firestore, fica no fallback 15)
                questionsPerRun = (snap.getLong("questionsPerRun") ?: questionsPerRun.toLong()).toInt()

                val attemptsUsed = (snap.getLong("attemptsUsed") ?: 0L).toInt()
                val remaining = max(0, attemptLimit - attemptsUsed)

                // BEST (já é “oficial” e só atualiza quando elegível)
                val bestCorrect = (snap.getLong("best.correct") ?: 0L).toInt()
                val bestTimeMs = (snap.getLong("best.timeMs") ?: -1L)
                val bestAttempt = (snap.getLong("best.attemptNo") ?: 0L).toInt()

                // LAST (sempre existe quando tentou)
                val lastAttempt = (snap.getLong("lastRun.attemptNo") ?: 0L).toInt()
                val lastCorrect = (snap.getLong("lastRun.correct") ?: 0L).toInt()
                val lastWrong = (snap.getLong("lastRun.wrong") ?: 0L).toInt()
                val lastTimeMs = (snap.getLong("lastRun.timeMs") ?: -1L)
                val bgCount = (snap.getLong("lastRun.backgroundCount") ?: 0L)
                val bgTotalMs = (snap.getLong("lastRun.backgroundTotalMs") ?: 0L)

                // ✅ NUNCA recalcula aqui: usa o que foi salvo no submit (fiel)
                val disqualified = snap.getBoolean("lastRun.disqualified") == true
                val disqReason = snap.getString("lastRun.disqualifyReason").orEmpty()

                binding.txtAttempts.text = "Tentativas: $attemptsUsed/$attemptLimit"
                binding.txtMin.text = "Mínimo p/ ranking: $minCorrect/$questionsPerRun"

                // STATUS: fiel ao que está salvo
                binding.txtStatus.text = when {
                    attemptsUsed == 0 -> "Status: você ainda não fez prova"
                    disqualified -> {
                        val extra = if (disqReason.isNotBlank()) " ($disqReason)" else ""
                        "Status: DESCLASSIFICADO$extra"
                    }
                    lastCorrect >= minCorrect -> "Status: elegível ✅"
                    else -> "Status: não atingiu o mínimo"
                }

                // BEST: fiel (só existe quando o sistema gravou como melhor)
                binding.txtBest.text = if (bestCorrect <= 0 || bestTimeMs <= 0 || bestTimeMs >= 90_000_000L) {
                    "Melhor: --"
                } else {
                    "Melhor: $bestCorrect/$questionsPerRun • ${formatMs(bestTimeMs)} • tent. $bestAttempt"
                }

                // LAST: fiel ao lastRun (sempre mostra)
                binding.txtLast.text = if (lastAttempt <= 0) {
                    "Última: --"
                } else {
                    val t = if (lastTimeMs > 0) formatMs(lastTimeMs) else "--"
                    val bgInfo =
                        if (bgCount > 0 || bgTotalMs > 0) " • fora do app: ${bgCount}x / ${bgTotalMs}ms" else ""
                    "Última: $lastCorrect/$questionsPerRun • erradas: $lastWrong • tempo: $t • tent. $lastAttempt$bgInfo"
                }

                // opcional: se acabou tudo, deixa claro
                if (remaining <= 0 && attemptsUsed > 0) {
                    // você pode mostrar um hint no status ou em outro TextView se tiver
                    // binding.txtStatus.append(" • tentativas esgotadas")
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                toast("Erro: ${e.message}")
                finish()
            }
    }

    private fun renderNotRegistered() {
        binding.txtWeek.text = "Semana: $weekId"
        binding.txtAttempts.text = "Tentativas: 0/$attemptLimit"
        binding.txtStatus.text = "Status: não inscrito"
        binding.txtMin.text = "Mínimo p/ ranking: $minCorrect/$questionsPerRun"
        binding.txtBest.text = "Melhor: --"
        binding.txtLast.text = "Última: --"
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun formatMs(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format(Locale("pt", "BR"), "%02d:%02d", min, sec)
    }
}
