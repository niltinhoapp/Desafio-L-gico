package com.desafiolgico.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.desafiolgico.databinding.ActivityLocalRecordsBinding
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LocalRecordsManager
import com.desafiolgico.utils.ShareUtils
import com.desafiolgico.utils.applyEdgeToEdge
import com.desafiolgico.utils.applySystemBarsPadding
import com.desafiolgico.weekly.WeeklyLocalCache
import com.desafiolgico.weekly.WeeklyResultActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class LocalRecordsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocalRecordsBinding

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyEdgeToEdge(lightSystemBarIcons = false)

        binding = ActivityLocalRecordsBinding.inflate(layoutInflater)
        binding.root.applySystemBarsPadding(applyTop = true, applyBottom = true)
        setContentView(binding.root)

        GameDataManager.init(this)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnShare.setOnClickListener {
            binding.recordsCard.post {
                ShareUtils.shareViewAsImage(this, binding.recordsCard, "Compartilhar placar")
            }
        }

        binding.btnWeeklyDetails.setOnClickListener {
            // abre a tela detalhada se tiver weekId no cache
            val weekId = WeeklyLocalCache.getWeekId(this)
            if (weekId.isNotBlank()) {
                startActivity(Intent(this, WeeklyResultActivity::class.java).putExtra("WEEK_ID", weekId))
            } else {
                // se não tiver cache, tenta abrir a atual via fetch (vai tentar atualizar no refreshWeekly)
                refreshWeeklySummary(forceOpenAfter = true)
            }
        }

        refreshAll()
    }

    override fun onResume() {
        super.onResume()
        refreshAll()
    }

    private fun refreshAll() {
        refreshBestStreak()
        refreshLevelsText()
        refreshWeeklySummary(forceOpenAfter = false)
    }

    private fun refreshBestStreak() {
        val bestStreak = LocalRecordsManager.getBestStreakOfDay(this)
        binding.bestStreakTodayText.text = "🔥 Melhor streak do dia: $bestStreak"
    }

    private fun refreshLevelsText() {
        fun line(level: String, label: String): String {
            val bestScore = LocalRecordsManager.getBestScoreForLevel(this, level)
            val bestTime = LocalRecordsManager.getBestAvgTimeForLevel(this, level)
            return "$label: $bestScore pts • ${formatTime(bestTime)}"
        }

        binding.levelsText.text = listOf(
            line(GameDataManager.Levels.INICIANTE, "Iniciante"),
            line(GameDataManager.Levels.INTERMEDIARIO, "Intermediário"),
            line(GameDataManager.Levels.AVANCADO, "Avançado"),
            line(GameDataManager.Levels.EXPERIENTE, "Experiente"),
        ).joinToString("\n")
    }

    private fun refreshWeeklySummary(forceOpenAfter: Boolean) {
        // 1) mostra cache instantâneo
        val cached = WeeklyLocalCache.getSummary(this)
        binding.weeklySummaryText.text = if (cached.isNotBlank()) cached else "Campeonato: sem dados ainda."

        // se não tem login, fica só no cache
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            binding.btnWeeklyDetails.visibility = View.GONE
            return
        } else {
            binding.btnWeeklyDetails.visibility = View.VISIBLE
        }

        // 2) tenta descobrir a semana atual e puxar do Firestore
        db.collection("weekly_events").document("current").get()
            .addOnSuccessListener { snap ->
                val weekId = snap.getString("weekId").orEmpty()
                if (weekId.isBlank()) return@addOnSuccessListener

                val userRef = db.collection("weekly_runs").document(weekId)
                    .collection("users").document(uid)

                userRef.get()
                    .addOnSuccessListener { doc ->
                        if (!doc.exists()) return@addOnSuccessListener

                        val best = doc.get("best") as? Map<*, *>
                        val bestCorrect = (best?.get("correct") as? Number)?.toInt() ?: 0
                        val bestTimeMs = (best?.get("timeMs") as? Number)?.toLong() ?: 0L

                        val last = doc.get("lastRun") as? Map<*, *>
                        val lastAttemptNo = (last?.get("attemptNo") as? Number)?.toInt() ?: 0
                        val lastCorrect = (last?.get("correct") as? Number)?.toInt() ?: 0
                        val lastTimeMs = (last?.get("timeMs") as? Number)?.toLong() ?: 0L
                        val disq = last?.get("disqualified") as? Boolean ?: false
                        val reason = last?.get("disqualifyReason") as? String ?: ""

                        val status = doc.getBoolean("bannedThisWeek") == true

                        val statusLine = when {
                            status -> "Status: bloqueado nesta semana"
                            else -> "Status: inscrito ✅"
                        }

                        val bestLine = if (bestCorrect > 0 && bestTimeMs > 0) {
                            "Melhor: $bestCorrect • ${formatMs(bestTimeMs)}"
                        } else "Melhor: --"

                        val lastLine = if (lastAttemptNo > 0 || lastCorrect > 0 || lastTimeMs > 0) {
                            val dq = if (disq) " • DQ(${if (reason.isBlank()) "MOTIVO" else reason})" else ""
                            "Última: T$lastAttemptNo • $lastCorrect acertos • ${formatMs(lastTimeMs)}$dq"
                        } else "Última: --"

                        val summary = "Semana $weekId\n$statusLine\n$bestLine\n$lastLine"

                        WeeklyLocalCache.save(this, weekId, summary)
                        binding.weeklySummaryText.text = summary

                        if (forceOpenAfter) {
                            startActivity(Intent(this, WeeklyResultActivity::class.java).putExtra("WEEK_ID", weekId))
                        }
                    }
            }
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0L) return "00:00"
        val totalSec = ms / 1000L
        val m = totalSec / 60L
        val s = totalSec % 60L
        return String.format("%02d:%02d", m, s)
    }

    private fun formatMs(ms: Long): String {
        if (ms <= 0L) return "--"
        val totalSec = ms / 1000L
        val m = totalSec / 60L
        val s = totalSec % 60L
        return String.format(Locale("pt", "BR"), "%02d:%02d", m, s)
    }
}
