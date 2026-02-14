package com.desafiolgico.main

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityRecordsBinding
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LocalRecordsManager
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.applyEdgeToEdge
import com.desafiolgico.utils.applySystemBarsPadding

class RecordsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecordsBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Edge-to-edge (tela escura -> ícones claros)
        applyEdgeToEdge(lightSystemBarIcons = false)

        binding = ActivityRecordsBinding.inflate(layoutInflater)

        // ✅ Insets: empurra conteúdo pra fora de status/nav bar
        // Se seu XML tiver contentContainer, aplique nele; senão, no root.
        // binding.contentContainer.applySystemBarsPadding(true, true)
        binding.root.applySystemBarsPadding(applyTop = true, applyBottom = true)

        setContentView(binding.root)

        GameDataManager.init(this)

        binding.btnBack.setOnClickListener { finish() }

        renderRecords()
    }

    override fun onResume() {
        super.onResume()
        renderRecords()
    }

    private fun renderRecords() {
        val bestStreakDay = LocalRecordsManager.getBestStreakOfDay(this)
        binding.txtBestStreakDay.text =
            if (bestStreakDay > 0) "🔥 Melhor streak do dia: $bestStreakDay"
            else "🔥 Melhor streak do dia: —"

        renderLevel(getString(R.string.level_beginner).uppercase(), GameDataManager.Levels.INICIANTE, R.id.level1)
        renderLevel(getString(R.string.level_intermediate).uppercase(), GameDataManager.Levels.INTERMEDIARIO, R.id.level2)
        renderLevel(getString(R.string.level_advanced).uppercase(), GameDataManager.Levels.AVANCADO, R.id.level3)
        renderLevel(getString(R.string.level_expert).uppercase(), GameDataManager.Levels.EXPERIENTE, R.id.level4)
    }

    private fun renderLevel(title: String, levelKey: String, containerId: Int) {
        val root = findViewById<android.view.View>(containerId)

        val tvTitle =
            root.findViewById<com.google.android.material.textview.MaterialTextView>(R.id.txtLevelTitle)
        val tvScore =
            root.findViewById<com.google.android.material.textview.MaterialTextView>(R.id.txtLevelScore)
        val tvTime =
            root.findViewById<com.google.android.material.textview.MaterialTextView>(R.id.txtLevelTime)

        val bestScore = LocalRecordsManager.getBestScoreForLevel(this, levelKey)
        val bestAvgMs = LocalRecordsManager.getBestAvgTimeForLevel(this, levelKey)

        val scoreText = if (bestScore > 0) "$bestScore" else "—"
        val timeText = if (bestAvgMs > 0L) formatMs(bestAvgMs) else "—"

        tvTitle.text = title
        tvScore.text = "🏆 ${getString(R.string.best_score_label)}: $scoreText"
        tvTime.text = "⏱ ${getString(R.string.best_avg_time_label)}: $timeText"
    }

    private fun formatMs(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(0L)
        val m = totalSec / 60L
        val s = totalSec % 60L
        return String.format("%02d:%02d", m, s)
    }
}
