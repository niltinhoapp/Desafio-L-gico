package com.desafiolgico.weekly

import android.animation.AnimatorInflater
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import com.desafiolgico.R
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
    // Config da semana (weekly_events/current)
    // =========================
    private var weekId: String? = null
    private var attemptLimit: Int = 3
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

    // Auth listener
    private var authListener: FirebaseAuth.AuthStateListener? = null

    // UI State
    private var lastPrimaryText: String = "Carregando..."
    private var lastPrimaryEnabled: Boolean = false
    private var lastPrimaryLoading: Boolean = false

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
                updatePrimaryButton(enabled = false, text = "Fazer login", loading = false)
            } else {
                updatePrimaryButton(enabled = true, text = "Carregando...", loading = true)
                loadAll()
            }
        }
        auth.addAuthStateListener(authListener!!)
    }
    private fun onPrimaryClick() {
        if (clickLocked) return
        clickLocked = true

        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            binding.txtMyStatus.text = "Status: faça login para participar"
            updatePrimaryButton(enabled = false, text = "Fazer login", loading = false)
            clickLocked = false
            return
        }

        val wid = weekId
        if (wid.isNullOrBlank()) {
            Toast.makeText(this, "Semana indisponível. Tente novamente.", Toast.LENGTH_SHORT).show()
            clickLocked = false
            return
        }

        // não inscrito -> inscreve e consome 1 tentativa oficial
        if (!isRegistered) {
            registerTransaction(wid, uid) {
                startWeeklyAttemptAndOpen(wid)
                // clickLocked será liberado nos callbacks do startWeeklyAttemptAndOpen
            }
            return
        }

        // inscrito -> acabou? abre resultado
        val remaining = max(0, attemptLimit - attemptsUsed)
        if (remaining <= 0) {
            openWeeklyResult(wid)
            clickLocked = false
            return
        }

        // inscrito -> inicia tentativa oficial
        startWeeklyAttemptAndOpen(wid)
    }

    override fun onDestroy() {
        authListener?.let { auth.removeAuthStateListener(it) }
        super.onDestroy()
    }

    private fun startWeeklyAttemptAndOpen(wid: String) {
        val uid = auth.currentUser?.uid ?: run {
            clickLocked = false
            return
        }

        setLoading(true)

        startWeeklyAttemptTransaction(
            wid, uid,
            onOk = { attemptNo ->
                setLoading(false)
                clickLocked = false
                openWeeklyTest(wid, attemptNo)
            },
            onNoAttempts = {
                setLoading(false)
                clickLocked = false
                openWeeklyResult(wid) // ✅ melhor do que toast
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


    /**
     * Transaction à prova de falhas:
     * - cria doc se não existir
     * - respeita attemptLimit
     * - incrementa attemptsUsed
     * - grava lastRun.startedAt / attemptNo
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

            // se não existe (user antigo), cria base mínima
            if (!snap.exists()) {
                tx.set(
                    userRef,
                    mapOf(
                        "uid" to uid,
                        "weekId" to wid,
                        "attemptLimit" to attemptLimit.toLong(),
                        "attemptsUsed" to 0L,
                        "bannedThisWeek" to false,
                        "lastRun" to mapOf(
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
            }

            val fresh = if (snap.exists()) snap else tx.get(userRef)

            val lim = (fresh.getLong("attemptLimit") ?: attemptLimit.toLong()).toInt()
            val used = (fresh.getLong("attemptsUsed") ?: 0L).toInt()
            val banned = fresh.getBoolean("bannedThisWeek") == true

            if (banned) return@runTransaction mapOf("status" to "BANNED")
            if (used >= lim) return@runTransaction mapOf("status" to "NO_ATTEMPTS")

            val nextAttempt = used + 1

            tx.set(
                userRef,
                mapOf(
                    "attemptsUsed" to FieldValue.increment(1),
                    "lastRun" to mapOf(
                        "attemptNo" to nextAttempt.toLong(),
                        "startedAt" to FieldValue.serverTimestamp(),
                        "finishedAt" to null,
                        "correct" to 0L,
                        "wrong" to 0L,
                        "timeMs" to 0L,
                        "backgroundCount" to 0L,
                        "backgroundTotalMs" to 0L,
                        "disqualified" to false,
                        "disqualifyReason" to ""
                    ),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )

            mapOf("status" to "OK", "attemptNo" to nextAttempt.toLong())
        }.addOnSuccessListener { result ->
            val status = result["status"] as? String ?: "FAIL"
            val attemptAny = result["attemptNo"]

            val attemptNo = when (attemptAny) {
                is Int -> attemptAny
                is Long -> attemptAny.toInt()
                is Double -> attemptAny.toInt()
                else -> -1
            }

            when (status) {
                "OK" -> if (attemptNo > 0) onOk(attemptNo) else onFail("attemptNo inválido")
                "NO_ATTEMPTS" -> onNoAttempts()
                "BANNED" -> onFail("Conta bloqueada nesta semana.")
                else -> onFail("Falha desconhecida.")
            }
        }.addOnFailureListener { e ->
            onFail(e.message ?: "erro")
        }
    }

    private fun openWeeklyTest(wid: String, attemptNo: Int) {
        startActivity(
            Intent(this, TestActivity::class.java)
                .putExtra("MODE", "WEEKLY")
                .putExtra("WEEK_ID", wid)
                .putExtra("ROUND_ID", "R1")
                .putExtra("ATTEMPT_NO", attemptNo)
                .putExtra("QUESTIONS_PER_RUN", questionsPerRun)
                .putExtra("MAX_TIME_MS", questionsPerRun * 30_000L) // 15*30s = 450s
                .putExtra("MIN_CORRECT", minCorrect)
        )
    }

    private fun loadAll() {
        val uid = auth.currentUser?.uid
        Log.d("WEEKLY", "loadAll uid=$uid")

        if (uid.isNullOrBlank()) {
            binding.txtMyStatus.text = "Status: faça login para participar"
            updatePrimaryButton(enabled = false, text = "Fazer login", loading = false)
            return
        }

        setLoading(true)

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
                    updatePrimaryButton(enabled = false, text = "Indisponível", loading = false)
                    return@addOnSuccessListener
                }

                db.collection("weekly_runs").document(wid).get()
                    .addOnSuccessListener { runDoc ->
                        registeredCount = runDoc.getLong("registeredCount") ?: 0L
                        limit = runDoc.getLong("limit") ?: 150L

                        db.collection("weekly_runs").document(wid)
                            .collection("registrations").document(uid).get()
                            .addOnSuccessListener { regDoc ->
                                isRegistered = regDoc.exists()

                                db.collection("weekly_runs").document(wid)
                                    .collection("users").document(uid).get()
                                    .addOnSuccessListener { udoc ->
                                        attemptsUsed = (udoc.getLong("attemptsUsed") ?: 0L).toInt()
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

        binding.txtBest.text = if (bestScore >= 0 && bestTimeMs >= 0) {
            "Melhor: $bestScore/$questionsPerRun • ${formatMs(bestTimeMs)}"
        } else {
            "Melhor: --"
        }

        val full = registeredCount >= limit

        when {
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
     * Transaction de inscrição:
     * - cria registrations/{uid}
     * - cria users/{uid}
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

            tx.set(
                regRef,
                mapOf(
                    "uid" to uid,
                    "active" to true,
                    "registeredAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )

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
                ),
                SetOptions.merge()
            )

            // opcional (recomendado):
            // tx.update(runRef, "registeredCount", FieldValue.increment(1))

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
                    loadAll()
                    clickLocked = false
                    Toast.makeText(this, "Inscrições encerradas (lotado).", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    clickLocked = false
                    loadAll()
                }
            }
        }.addOnFailureListener { e ->
            Log.e("WEEKLY", "registerTransaction fail: ${e.message}", e)
            setLoading(false)
            clickLocked = false
            loadAll()
            Toast.makeText(this, "Erro ao inscrever: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setLoading(loading: Boolean) {
        lastPrimaryLoading = loading
        updatePrimaryButton(
            enabled = !loading && lastPrimaryEnabled,
            text = lastPrimaryText,
            loading = loading
        )
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
