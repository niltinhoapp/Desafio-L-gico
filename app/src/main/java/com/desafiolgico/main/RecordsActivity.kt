package com.desafiolgico.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityRecordsBinding
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LocalRecordsManager
import com.desafiolgico.utils.LanguageHelper

class RecordsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecordsBinding

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordsBinding.inflate(layoutInflater)
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
        // streak do dia
        val bestStreakDay = LocalRecordsManager.getBestStreakOfDay(this)
        binding.txtBestStreakDay.text =
            if (bestStreakDay > 0) "🔥 Melhor streak do dia: $bestStreakDay"
            else "🔥 Melhor streak do dia: —"

        // níveis
        renderLevel("INICIANTE", GameDataManager.Levels.INICIANTE, R.id.level1)
        renderLevel("INTERMEDIÁRIO", GameDataManager.Levels.INTERMEDIARIO, R.id.level2)
        renderLevel("AVANÇADO", GameDataManager.Levels.AVANCADO, R.id.level3)
        renderLevel("EXPERIENTE", GameDataManager.Levels.EXPERIENTE, R.id.level4)



    }

    private fun renderLevel(title: String, levelKey: String, containerId: Int) {
        val root = findViewById<android.view.View>(containerId)

        val tvTitle = root.findViewById<com.google.android.material.textview.MaterialTextView>(R.id.txtLevelTitle)
        val tvScore = root.findViewById<com.google.android.material.textview.MaterialTextView>(R.id.txtLevelScore)
        val tvTime  = root.findViewById<com.google.android.material.textview.MaterialTextView>(R.id.txtLevelTime)

        val bestScore = LocalRecordsManager.getBestScoreForLevel(this, levelKey)
        val bestAvgMs = LocalRecordsManager.getBestAvgTimeForLevel(this, levelKey)

        val scoreText = if (bestScore > 0) "$bestScore" else "—"
        val timeText = if (bestAvgMs > 0L) formatMs(bestAvgMs) else "—"

        tvTitle.text = title
        tvScore.text = "🏆 Melhor score: $scoreText"
        tvTime.text = "⏱ Melhor tempo médio: $timeText"
    }

    private fun formatMs(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format("%02d:%02d", m, s)
    }
}
