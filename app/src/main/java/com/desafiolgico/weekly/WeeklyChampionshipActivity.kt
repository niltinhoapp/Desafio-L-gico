package com.desafiolgico.weekly

import android.animation.AnimatorInflater
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.desafiolgico.weekly.WeeklyResultActivity
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityWeeklyChampionshipBinding
import com.desafiolgico.main.TestActivity
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
    // Config da semana (weekly_events/current)
    // =========================
    private var weekId: String? = null

    // ✅ TESTE: força 20 tentativas no app (mesmo que Firestore esteja 3)
    // Depois você remove isso e volta a usar o valor do servidor.
    private var attemptLimit: Int = 20

    private var questionsPerRun: Int = 15
    private var minCorrect: Int = 13
    private var endAt: Timestamp? = null

    // =========================
    // weekly_runs/{weekId}
    // =========================
    private var registeredCount: Long = 0
    private var limit: Long = 150

    // =========================
    // user state
    // =========================
    private var isRegistered = false
    private var attemptsUsed = 0
    private var bestScore = -1
    private var bestTimeMs = -1L

    private var authListener: FirebaseAuth.AuthStateListener? = null

    // UI State
    private var lastPrimaryText: String = "Carregando..."
    private var lastPrimaryEnabled: Boolean = false
    private var isLoading: Boolean = false

    // Anti double-click / anti race
    private var clickLocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeeklyChampionshipBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPremiumUi()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnPrimary.setOnClickListener {
            if (clickLocked) return@setOnClickListener
            onPrimaryClick()
        }

        authListener = FirebaseAuth.AuthStateListener { a ->
            val u = a.currentUser
            Log.d("WEEKLY", "AuthState changed. user=${u?.uid}")

            if (u == null) {
                binding.txtMyStatus.text = "Status: faça login para participar"
                updatePrimaryButton(enabled = false, text = "Faça login", loading = false)
                setLoading(false)
            } else {
                updatePrimaryButton(enabled = true, text = "Carregando...", loading = true)
                loadAll()
            }
        }
        auth.addAuthStateListener(authListener!!)
    }

    override fun onDestroy() {
        authListener?.let { auth.removeAuthStateListener(it) }
        super.onDestroy()
    }

    private fun onPrimaryClick() {
        if (clickLocked) return
        clickLocked = true

        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            binding.txtMyStatus.text = "Status: faça login para participar"
            updatePrimaryButton(enabled = false, text = "Faça login", loading = false)
            clickLocked = false
            return
        }

        val wid = weekId
        if (wid.isNullOrBlank()) {
            Toast.makeText(this, "Semana indisponível. Tente novamente.", Toast.LENGTH_SHORT).show()
            clickLocked = false
            return
        }

        // se fechou, vai pro resultado
        if (isWeekClosedNow()) {
            openWeeklyResult(wid)
            clickLocked = false
            return
        }

        // não inscrito -> inscreve -> inicia tentativa
        if (!isRegistered) {
            registerTransaction(wid, uid) {
                startWeeklyAttemptAndOpen(wid)
            }
            return
        }

        val remaining = max(0, attemptLimit - attemptsUsed)
        if (remaining <= 0) {
            openWeeklyResult(wid)
            clickLocked = false
            return
        }

        startWeeklyAttemptAndOpen(wid)
    }

    private fun startWeeklyAttemptAndOpen(wid: String) {
        val uid = auth.currentUser?.uid ?: run {
            clickLocked = false
            return
        }

        if (isWeekClosedNow()) {
            setLoading(false)
            clickLocked = false
            openWeeklyResult(wid)
            return
        }

        setLoading(true)

        startWeeklyAttemptTransaction(
            wid = wid,
            uid = uid,
            onOk = { attemptNo ->
                setLoading(false)
                clickLocked = false
                openWeeklyTest(wid, attemptNo)
            },
            onNoAttempts = {
                setLoading(false)
                clickLocked = false
                openWeeklyResult(wid)
                loadAll()
            },
            onFail = { msg ->
                setLoading(false)
                clickLocked = false
                Toast.makeText(this, "Erro ao iniciar: $msg", Toast.LENGTH_LONG).show()
                loadAll()
            }
        )
    }

    private fun openWeeklyResult(wid: String) {
        startActivity(
            Intent(this, WeeklyResultActivity::class.java)
                .putExtra("WEEK_ID", wid)
        )
    }

    private fun openWeeklyTest(wid: String, attemptNo: Int) {
        startActivity(
            Intent(this, TestActivity::class.java)
                .putExtra(TestActivity.EXTRA_MODE, "WEEKLY")
                .putExtra("WEEK_ID", wid)
                .putExtra("ROUND_ID", "R1")
                .putExtra("ATTEMPT_NO", attemptNo)
                .putExtra("QUESTIONS_PER_RUN", questionsPerRun)
                .putExtra("MAX_TIME_MS", questionsPerRun * 30_000L)
                .putExtra("MIN_CORRECT", minCorrect)
        )
    }

    private fun isWeekClosedNow(): Boolean {
        val ts = endAt ?: return false
        return runCatching { System.currentTimeMillis() >= ts.toDate().time }.getOrDefault(false)
    }

    /**
     * Transaction de tentativa:
     * - exige que users/{uid} exista (inscrição)
     * - respeita attemptLimit do doc do usuário (ou fallback local)
     * - incrementa attemptsUsed
     * - salva lastRun.startedAt / attemptNo
     */

    private fun startWeeklyAttemptTransaction(
        wid: String,
        uid: String,
        onOk: (attemptNo: Int) -> Unit,
        onNoAttempts: () -> Unit,
        onFail: (String) -> Unit
    ) {
        val userRef = db.collection("weekly_runs").document(wid)
            .collection("users").document(uid)

        db.runTransaction { tx ->
            val snap = tx.get(userRef)
            if (!snap.exists()) return@runTransaction mapOf("status" to "NOT_REGISTERED")

            val lim = (snap.getLong("attemptLimit") ?: attemptLimit.toLong()).toInt()
            val used = (snap.getLong("attemptsUsed") ?: 0L).toInt()
            val banned = snap.getBoolean("bannedThisWeek") == true

            if (banned) return@runTransaction mapOf("status" to "BANNED")
            if (used >= lim) return@runTransaction mapOf("status" to "NO_ATTEMPTS")

            val nextAttempt = used + 1

            // ✅ atualiza “campo a campo” (não substitui lastRun inteiro)
            tx.update(userRef, mapOf(
                "attemptsUsed" to FieldValue.increment(1),
                "updatedAt" to FieldValue.serverTimestamp(),

                "lastRun.attemptNo" to nextAttempt.toLong(),
                "lastRun.startedAt" to FieldValue.serverTimestamp(), // pode sobrescrever aqui (ok)
                "lastRun.finishedAt" to null,

                // limpa IDs antigos (o WeeklyController vai gravar os 15 IDs do attempt)
                "lastRun.questionIds" to FieldValue.delete(),

                "lastRun.correct" to 0L,
                "lastRun.wrong" to 0L,
                "lastRun.timeMs" to 0L,
                "lastRun.backgroundCount" to 0L,
                "lastRun.backgroundTotalMs" to 0L,
                "lastRun.disqualified" to false,
                "lastRun.disqualifyReason" to ""
            ))

            mapOf("status" to "OK", "attemptNo" to nextAttempt.toLong())
        }.addOnSuccessListener { result ->
            val status = result["status"] as? String ?: "FAIL"
            val attemptNo = (result["attemptNo"] as? Long)?.toInt() ?: -1

            when (status) {
                "OK" -> if (attemptNo > 0) onOk(attemptNo) else onFail("attemptNo inválido")
                "NO_ATTEMPTS" -> onNoAttempts()
                "BANNED" -> onFail("Conta bloqueada nesta semana.")
                "NOT_REGISTERED" -> onFail("Você ainda não está inscrito.")
                else -> onFail("Falha desconhecida.")
            }
        }.addOnFailureListener { e ->
            onFail(e.message ?: "erro")
        }
    }

    /**
     * ✅ loadAll sem bug:
     * - lê weekly_events/current
     * - lê weekly_runs/{weekId}
     * - lê registrations/{uid} e users/{uid} em paralelo
     * - considera inscrito se QUALQUER UM existir
     *
     * ✅ TESTE 20 tentativas:
     * - mantém attemptLimit = 20 local sempre (não sobrescreve com servidor)
     */
    private fun loadAll() {
        val uid = auth.currentUser?.uid
        Log.d("WEEKLY", "loadAll uid=$uid")

        if (uid.isNullOrBlank()) {
            binding.txtMyStatus.text = "Status: faça login para participar"
            updatePrimaryButton(enabled = false, text = "Faça login", loading = false)
            setLoading(false)
            return
        }

        setLoading(true)

        db.collection("weekly_events").document("current").get()
            .addOnSuccessListener { snap ->
                weekId = snap.getString("weekId")

                // ⚠️ NÃO sobrescreve attemptLimit com o servidor durante o teste
                // val fromServer = (snap.getLong("attemptLimit") ?: 3L).toInt()
                // attemptLimit = fromServer

                questionsPerRun = (snap.getLong("questionsPerRun") ?: 15L).toInt()
                minCorrect = (snap.getLong("minCorrect") ?: 13L).toInt()
                endAt = snap.getTimestamp("endAt")

                val wid = weekId
                if (wid.isNullOrBlank()) {
                    setLoading(false)
                    binding.txtWeekInfo.text = "Semana: indisponível"
                    binding.txtMyStatus.text = "Status: sem campeonato ativo"
                    updatePrimaryButton(enabled = false, text = "Indisponível", loading = false)
                    return@addOnSuccessListener
                }

                db.collection("weekly_runs").document(wid).get()
                    .addOnSuccessListener { runDoc ->
                        registeredCount = runDoc.getLong("registeredCount") ?: 0L
                        limit = runDoc.getLong("limit") ?: 150L

                        val runRef = db.collection("weekly_runs").document(wid)
                        val regTask = runRef.collection("registrations").document(uid).get()
                        val userTask = runRef.collection("users").document(uid).get()

                        Tasks.whenAllSuccess<com.google.firebase.firestore.DocumentSnapshot>(regTask, userTask)
                            .addOnSuccessListener { snaps ->
                                val regSnap = snaps[0]
                                val userSnap = snaps[1]

                                val regExists = regSnap.exists()
                                val userExists = userSnap.exists()
                                isRegistered = regExists || userExists

                                if (userExists) {
                                    attemptsUsed = (userSnap.getLong("attemptsUsed") ?: 0L).toInt()
                                    bestScore = (userSnap.getLong("best.correct") ?: -1L).toInt()
                                    bestTimeMs = (userSnap.getLong("best.timeMs") ?: -1L)
                                    minCorrect = (userSnap.getLong("rules.minCorrect") ?: minCorrect.toLong()).toInt()
                                } else {
                                    attemptsUsed = 0
                                    bestScore = -1
                                    bestTimeMs = -1L
                                }

                                setLoading(false)
                                render()
                            }
                            .addOnFailureListener { e ->
                                Log.e("WEEKLY", "reg/users parallel read fail: ${e.message}", e)
                                setLoading(false)
                                render()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("WEEKLY", "weekly_runs/{weekId} read fail: ${e.message}", e)
                        setLoading(false)
                        binding.txtWeekInfo.text = "Semana: $wid"
                        binding.txtMyStatus.text = "Status: erro ao carregar semana"
                        updatePrimaryButton(enabled = true, text = "Tentar novamente", loading = false)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("WEEKLY", "weekly_events/current read fail: ${e.message}", e)
                setLoading(false)
                binding.txtWeekInfo.text = "Semana: indisponível"
                binding.txtMyStatus.text = "Status: erro ao carregar config"
                updatePrimaryButton(enabled = true, text = "Tentar novamente", loading = false)
            }
    }

    private fun render() {
        val wid = weekId ?: "--"
        binding.txtWeekInfo.text = "Semana: $wid"

        binding.txtCloseAt.text = "Fecha em: ${formatTs(endAt)}"
        binding.txtSlots.text = "Inscritos: $registeredCount/$limit"
        runCatching {
            binding.progressSlots.max = limit.toInt()
            binding.progressSlots.progress = registeredCount.toInt().coerceAtMost(limit.toInt())
        }

        val remaining = max(0, attemptLimit - attemptsUsed)
        binding.txtAttempts.text = "Tentativas restantes: $remaining/$attemptLimit"

        val bestIsSet = bestScore > 0 && bestTimeMs in 1..99_999_998L
        binding.txtBest.text = if (bestIsSet) {
            "Melhor: $bestScore/$questionsPerRun • ${formatMs(bestTimeMs)}"
        } else {
            "Melhor: --"
        }

        val full = registeredCount >= limit
        val closed = isWeekClosedNow()

        when {
            closed -> {
                binding.txtMyStatus.text = "Status: campeonato encerrado"
                updatePrimaryButton(enabled = true, text = "Ver resultado", loading = false)
            }
            full && !isRegistered -> {
                binding.txtMyStatus.text = "Status: inscrições encerradas"
                updatePrimaryButton(enabled = false, text = "Lotado", loading = false)
            }
            !isRegistered -> {
                binding.txtMyStatus.text = "Status: você ainda não está inscrito"
                updatePrimaryButton(enabled = true, text = "Inscrever e fazer prova", loading = false)
            }
            remaining <= 0 -> {
                binding.txtMyStatus.text = "Status: tentativas esgotadas"
                updatePrimaryButton(enabled = true, text = "Ver resultado", loading = false)
            }
            else -> {
                binding.txtMyStatus.text = "Status: inscrito ✅"
                val label =
                    if (attemptsUsed == 0) "Iniciar prova"
                    else "Continuar (tentativa ${attemptsUsed + 1})"
                updatePrimaryButton(enabled = true, text = label, loading = false)
            }
        }
    }

    /**
     * ✅ Inscrição idempotente:
     * - se reg OU user já existir => ALREADY (não tenta mexer em registrations)
     * - cria registrations/{uid} com tx.set (create permitido pelas rules)
     * - cria users/{uid} com tx.set (create permitido pelas rules)
     *
     * ⚠️ NÃO atualiza weekly_runs/{weekId} root (suas rules bloqueiam write).
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
            if (current >= lim) return@runTransaction "FULL"

            val regSnap = tx.get(regRef)
            val userSnap = tx.get(userRef)

            // ✅ já inscrito se existir qualquer um dos 2
            if (regSnap.exists() || userSnap.exists()) {
                // não encosta em registrations (evita rules chatas)
                return@runTransaction "ALREADY"
            }

            // ✅ cria registration
            tx.set(
                regRef,
                mapOf(
                    "uid" to uid,
                    "active" to true,
                    "registeredAt" to FieldValue.serverTimestamp()
                )
            )

            // ✅ cria userDoc com attemptLimit do teste (20)
            tx.set(
                userRef,
                mapOf(
                    "uid" to uid,
                    "weekId" to wid,
                    "attemptLimit" to attemptLimit.toLong(),
                    "attemptsUsed" to 0L,
                    "bannedThisWeek" to false,
                    "rules" to mapOf("minCorrect" to minCorrect.toLong()),
                    "best" to mapOf(
                        "correct" to 0L,
                        "timeMs" to 99999999L,
                        "attemptNo" to 0L,
                        "achievedAt" to null
                    ),
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
                )
            )

            "OK"
        }.addOnSuccessListener { result ->
            setLoading(false)
            when (result) {
                "OK", "ALREADY" -> {
                    isRegistered = true
                    loadAll()
                    onOk()
                }
                "FULL" -> {
                    clickLocked = false
                    loadAll()
                    Toast.makeText(this, "Inscrições encerradas (lotado).", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    clickLocked = false
                    loadAll()
                }
            }
        }.addOnFailureListener { e ->
            setLoading(false)
            clickLocked = false
            loadAll()

            // ✅ se falhar por permissão mas você "já estava inscrito", tenta seguir pras tentativas
            // (útil quando registrations existe mas o app tentou regravar algo)
            if (e.message?.contains("PERMISSION_DENIED", true) == true) {
                startWeeklyAttemptAndOpen(wid)
                return@addOnFailureListener
            }

            Toast.makeText(this, "Erro ao inscrever: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setLoading(loading: Boolean) {
        isLoading = loading
        updatePrimaryButton(enabled = lastPrimaryEnabled, text = lastPrimaryText, loading = loading)
    }

    // =========================
    // PREMIUM UI HELPERS
    // =========================

    private fun setupPremiumUi() = with(binding) {
        runCatching { cardSlots.premiumEnter(R.anim.premium_enter) }
        runCatching { cardStatus.premiumEnter(R.anim.premium_enter_delay) }
        runCatching { ctaWrap.premiumEnter(R.anim.premium_enter_delay) }

        btnPrimary.pressFeedback()
        btnBack.pressFeedback()
    }

    private fun View.premiumEnter(animRes: Int) {
        visibility = View.INVISIBLE
        doOnPreDraw {
            visibility = View.VISIBLE
            startAnimation(AnimationUtils.loadAnimation(context, animRes))
        }
    }

    private fun View.pressFeedback() {
        setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(90).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }
            false
        }
    }

    private fun setPulseEnabled(view: View, enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.stateListAnimator =
                if (enabled) AnimatorInflater.loadStateListAnimator(this, R.animator.btn_pulse)
                else null
        } else {
            view.clearAnimation()
        }
    }

    private fun updatePrimaryButton(enabled: Boolean, text: String, loading: Boolean) = with(binding) {
        lastPrimaryText = text
        lastPrimaryEnabled = enabled

        progress.visibility = if (loading) View.VISIBLE else View.GONE

        btnPrimary.text = text
        btnPrimary.isEnabled = enabled && !loading
        btnPrimary.alpha = if (loading) 0.90f else 1f

        setPulseEnabled(btnPrimary, btnPrimary.isEnabled && !loading)
    }

    // =========================
    // Format helpers
    // =========================

    private fun formatTs(ts: Timestamp?): String {
        if (ts == null) return "--"
        val sdf = SimpleDateFormat("dd/MM 'às' HH:mm", Locale("pt", "BR"))
        return sdf.format(ts.toDate())
    }

    private fun formatMs(ms: Long): String {
        if (ms < 0) return "--"
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format(Locale("pt", "BR"), "%02d:%02d", min, sec)
    }
}
