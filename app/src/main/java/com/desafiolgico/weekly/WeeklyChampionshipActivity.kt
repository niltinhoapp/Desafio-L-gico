package com.desafiolgico.weekly

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.desafiolgico.databinding.ActivityWeeklyChampionshipBinding
import com.desafiolgico.main.TestActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

class WeeklyChampionshipActivity : AppCompatActivity() {

    // =========================
    // UI / Firebase
    // =========================
    private lateinit var binding: ActivityWeeklyChampionshipBinding
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    // =========================
    // Config da semana (vinda de weekly_events/current)
    // =========================
    private var weekId: String? = null
    private var attemptLimit: Int = 3
    private var questionsPerRun: Int = 15
    private var minCorrect: Int = 13
    private var endAt: Timestamp? = null

    // =========================
    // Estado do “evento” (weekly_runs/{weekId})
    // =========================
    private var registeredCount: Long = 0
    private var limit: Long = 150

    // =========================
    // Estado do usuário (nesta semana)
    // =========================
    private var isRegistered = false
    private var attemptsUsed = 0
    private var bestScore = -1
    private var bestTimeMs = -1L

    // =========================
    // Auth listener (evita ler Firestore antes do token estar pronto)
    // =========================
    private var authListener: FirebaseAuth.AuthStateListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeeklyChampionshipBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Botões
        binding.btnBack.setOnClickListener { finish() }
        binding.btnPrimary.setOnClickListener { onPrimaryClick() }

        // ✅ IMPORTANTÍSSIMO:
        // Em alguns cenários o Android abre a tela antes do FirebaseAuth “assentar”
        // e o Firestore faz request sem request.auth => PERMISSION_DENIED.
        // Então a gente só chama loadAll() quando currentUser estiver OK.
        authListener = FirebaseAuth.AuthStateListener { a ->
            val u = a.currentUser
            Log.d("WEEKLY", "AuthState changed. user=${u?.uid}")

            if (u == null) {
                // Sem login: trava botão e mostra instrução
                binding.txtMyStatus.text = "Status: faça login para participar"
                binding.btnPrimary.isEnabled = false
                binding.btnPrimary.text = "Fazer login"
                binding.progress.visibility = View.GONE
            } else {
                // Com login: carrega tudo
                binding.btnPrimary.isEnabled = true
                loadAll()
            }
        }
        auth.addAuthStateListener(authListener!!)
    }

    override fun onDestroy() {
        // Remove listener para evitar leaks
        authListener?.let { auth.removeAuthStateListener(it) }
        super.onDestroy()
    }

    /**
     * Clique principal:
     * - Se não inscrito → inscreve (transaction) e depois abre prova
     * - Se inscrito e tem tentativas → abre prova
     * - Se inscrito e sem tentativas → (por enquanto) mostra mensagem
     */
    private fun onPrimaryClick() {
        val uid = auth.currentUser?.uid
        Log.d("WEEKLY", "onPrimaryClick uid=$uid")

        if (uid.isNullOrBlank()) {
            binding.txtMyStatus.text = "Status: faça login para participar"
            binding.btnPrimary.isEnabled = false
            binding.btnPrimary.text = "Fazer login"
            return
        }

        val wid = weekId ?: run {
            Toast.makeText(this, "Semana indisponível. Tente novamente.", Toast.LENGTH_SHORT).show()
            return
        }

        // Se não inscrito → inscreve e inicia
        if (!isRegistered) {
            registerTransaction(wid, uid) {
                // após inscrever, abre a prova (tentativa 1)
                openWeeklyTest(wid)
            }
            return
        }

        // Se inscrito → checa tentativas
        val remaining = max(0, attemptLimit - attemptsUsed)
        if (remaining <= 0) {
            Toast.makeText(this, "Tentativas esgotadas. Em breve: ranking!", Toast.LENGTH_SHORT).show()
            loadAll()
            return
        }

        // Inscrito e com tentativas → abre prova
        openWeeklyTest(wid)
    }

    /**
     * Abre a TestActivity em modo WEEKLY.
     * ATTEMPT_NO aqui é só informativo (o “oficial” deve ser controlado no Firestore no startWeeklyAttempt).
     */
    private fun openWeeklyTest(wid: String) {
        startActivity(
            Intent(this, TestActivity::class.java)
                .putExtra("MODE", "WEEKLY")
                .putExtra("WEEK_ID", wid)
                .putExtra("ROUND_ID", "R1")
                .putExtra("ATTEMPT_NO", attemptsUsed + 1)
        )
    }

    /**
     * Carrega:
     * 1) weekly_events/current (config)
     * 2) weekly_runs/{weekId} (inscritos/limite)
     * 3) weekly_runs/{weekId}/registrations/{uid} (compat: saber se está inscrito)
     * 4) weekly_runs/{weekId}/users/{uid} (schema novo: attemptsUsed + best.*)
     */
    private fun loadAll() {
        val uid = auth.currentUser?.uid
        Log.d("WEEKLY", "loadAll uid=$uid")

        if (uid.isNullOrBlank()) {
            binding.txtMyStatus.text = "Status: faça login para participar"
            binding.btnPrimary.isEnabled = false
            binding.btnPrimary.text = "Fazer login"
            return
        }

        setLoading(true)

        // 1) weekly_events/current
        db.collection("weekly_events").document("current").get()
            .addOnSuccessListener { snap ->
                weekId = snap.getString("weekId")
                attemptLimit = (snap.getLong("attemptLimit") ?: 3L).toInt()
                questionsPerRun = (snap.getLong("questionsPerRun") ?: 15L).toInt()
                minCorrect = (snap.getLong("minCorrect") ?: 13L).toInt()
                endAt = snap.getTimestamp("endAt")

                val wid = weekId
                if (wid.isNullOrBlank()) {
                    setLoading(false)
                    binding.txtWeekInfo.text = "Semana: indisponível"
                    binding.txtMyStatus.text = "Status: sem campeonato ativo"
                    binding.btnPrimary.isEnabled = false
                    binding.btnPrimary.text = "Indisponível"
                    return@addOnSuccessListener
                }

                // 2) weekly_runs/{weekId}
                db.collection("weekly_runs").document(wid).get()
                    .addOnSuccessListener { runDoc ->
                        registeredCount = runDoc.getLong("registeredCount") ?: 0L
                        limit = runDoc.getLong("limit") ?: 150L

                        // 3) registrations/{uid} (compat)
                        db.collection("weekly_runs").document(wid)
                            .collection("registrations").document(uid).get()
                            .addOnSuccessListener { regDoc ->
                                isRegistered = regDoc.exists()

                                // 4) users/{uid} (novo schema)
                                db.collection("weekly_runs").document(wid)
                                    .collection("users").document(uid).get()
                                    .addOnSuccessListener { udoc ->
                                        // Se o doc não existe ainda, defaults
                                        attemptsUsed = (udoc.getLong("attemptsUsed") ?: 0L).toInt()

                                        // best.correct / best.timeMs (se não tiver, defaults)
                                        bestScore = (udoc.getLong("best.correct") ?: -1L).toInt()
                                        bestTimeMs = (udoc.getLong("best.timeMs") ?: -1L)

                                        setLoading(false)
                                        render()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("WEEKLY", "users/{uid} read fail: ${e.message}", e)
                                        setLoading(false)
                                        render()
                                    }
                            }
                            .addOnFailureListener { e ->
                                Log.e("WEEKLY", "registrations/{uid} read fail: ${e.message}", e)
                                setLoading(false)
                                render()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("WEEKLY", "weekly_runs/{weekId} read fail: ${e.message}", e)
                        setLoading(false)
                        binding.txtWeekInfo.text = "Semana: $wid"
                        binding.txtMyStatus.text = "Status: erro ao carregar semana"
                        binding.btnPrimary.isEnabled = false
                        binding.btnPrimary.text = "Tentar novamente"
                    }
            }
            .addOnFailureListener { e ->
                Log.e("WEEKLY", "weekly_events/current read fail: ${e.message}", e)
                setLoading(false)
                binding.txtWeekInfo.text = "Semana: indisponível"
                binding.txtMyStatus.text = "Status: erro ao carregar config"
                binding.btnPrimary.isEnabled = false
                binding.btnPrimary.text = "Tentar novamente"
            }
    }

    /**
     * Atualiza a UI com os dados carregados.
     */
    private fun render() {
        val wid = weekId ?: "--"
        binding.txtWeekInfo.text = "Semana: $wid"

        binding.txtCloseAt.text = "Fecha em: ${formatTs(endAt)}"
        binding.txtSlots.text = "Inscritos: $registeredCount/$limit"

        val remaining = max(0, attemptLimit - attemptsUsed)
        binding.txtAttempts.text = "Tentativas restantes: $remaining/$attemptLimit"

        binding.txtBest.text = if (bestScore >= 0 && bestTimeMs >= 0) {
            "Melhor: $bestScore/$questionsPerRun • ${formatMs(bestTimeMs)}"
        } else {
            "Melhor: --"
        }

        val full = registeredCount >= limit
        binding.btnPrimary.isEnabled = true

        when {
            full && !isRegistered -> {
                binding.txtMyStatus.text = "Status: inscrições encerradas"
                binding.btnPrimary.isEnabled = false
                binding.btnPrimary.text = "Lotado"
            }
            !isRegistered -> {
                binding.txtMyStatus.text = "Status: você ainda não está inscrito"
                binding.btnPrimary.text = "Inscrever e fazer prova"
            }
            remaining <= 0 -> {
                binding.txtMyStatus.text = "Status: tentativas esgotadas"
                binding.btnPrimary.text = "Ver resultado"
            }
            else -> {
                binding.txtMyStatus.text = "Status: inscrito ✅"
                binding.btnPrimary.text =
                    if (attemptsUsed == 0) "Iniciar prova"
                    else "Continuar (tentativa ${attemptsUsed + 1})"
            }
        }
    }

    /**
     * Transaction de inscrição:
     * - Garante que não estoura limite
     * - Cria registrations/{uid} (compat)
     * - Cria users/{uid} (schema novo) pra não quebrar startWeeklyAttempt
     * - Incrementa registeredCount
     */
    private fun registerTransaction(wid: String, uid: String, onOk: () -> Unit) {
        setLoading(true)

        val runRef = db.collection("weekly_runs").document(wid)
        val regRef = runRef.collection("registrations").document(uid)
        val userRef = runRef.collection("users").document(uid)

        db.runTransaction { tx ->
            val runSnap = tx.get(runRef)
            val current = runSnap.getLong("registeredCount") ?: 0L
            val lim = runSnap.getLong("limit") ?: 150L

            val regSnap = tx.get(regRef)
            if (regSnap.exists()) return@runTransaction "ALREADY"
            if (current >= lim) return@runTransaction "FULL"

            // 1) cria registration
            tx.set(
                regRef,
                mapOf(
                    "uid" to uid,
                    "active" to true,
                    "registeredAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )

            // 2) cria/merge doc do usuário na semana (novo schema)
            tx.set(
                userRef,
                mapOf(
                    "uid" to uid,
                    "weekId" to wid,
                    "attemptLimit" to attemptLimit.toLong(),
                    "attemptsUsed" to 0L,
                    "bannedThisWeek" to false,

                    // regras que você usa no submitWeeklyResult()
                    "rules" to mapOf(
                        "minCorrect" to minCorrect.toLong()
                    ),

                    // melhor resultado
                    "best" to mapOf(
                        "correct" to 0L,
                        "timeMs" to 99999999L,
                        "attemptNo" to 0L,
                        "achievedAt" to null
                    ),

                    // última run (vai ser atualizada ao iniciar/finalizar)
                    "lastRun" to mapOf(
                        "attemptNo" to 0L,
                        "startedAt" to null,
                        "finishedAt" to null,
                        "correct" to 0L,
                        "wrong" to 0L,
                        "timeMs" to 0L,
                        "backgroundCount" to 0L,
                        "backgroundTotalMs" to 0L,
                        "disqualified" to false,
                        "disqualifyReason" to ""
                    ),

                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )

            // 3) incrementa inscritos
            //  tx.update(runRef, "registeredCount", current + 1)

            "OK"
        }.addOnSuccessListener { result ->
            setLoading(false)
            when (result) {
                "OK", "ALREADY" -> {
                    isRegistered = true
                    // Recarrega para refletir attemptsUsed/best etc.
                    loadAll()
                    onOk()
                }
                "FULL" -> {
                    loadAll()
                    Toast.makeText(this, "Inscrições encerradas (lotado).", Toast.LENGTH_SHORT).show()
                }
                else -> loadAll()
            }
        }.addOnFailureListener { e ->
            Log.e("WEEKLY", "registerTransaction fail: ${e.message}", e)
            setLoading(false)
            loadAll()
            Toast.makeText(this, "Erro ao inscrever: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Mostra/oculta loader e bloqueia o botão
     */
    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnPrimary.isEnabled = !loading
    }

    /**
     * Formata Timestamp para UI
     */
    private fun formatTs(ts: Timestamp?): String {
        if (ts == null) return "--"
        val sdf = SimpleDateFormat("dd/MM 'às' HH:mm", Locale("pt", "BR"))
        return sdf.format(ts.toDate())
    }

    /**
     * Formata ms em mm:ss
     */
    private fun formatMs(ms: Long): String {
        if (ms < 0) return "--"
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format(Locale("pt", "BR"), "%02d:%02d", min, sec)
    }
}
