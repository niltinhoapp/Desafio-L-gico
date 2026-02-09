package com.desafiolgico.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.desafiolgico.databinding.ActivityLocalRecordsBinding
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LocalRecordsManager
import com.desafiolgico.utils.ShareUtils

class LocalRecordsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocalRecordsBinding


        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            binding = ActivityLocalRecordsBinding.inflate(layoutInflater)
            setContentView(binding.root)

            GameDataManager.init(this)

            binding.btnBack.setOnClickListener { finish() }

            binding.btnShare.setOnClickListener {
                binding.recordsCard.post {
                    ShareUtils.shareViewAsImage(this, binding.recordsCard, "Compartilhar placar")
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

    private fun formatTime(ms: Long): String {
        if (ms <= 0L) return "00:00"
        val totalSec = ms / 1000L
        val m = totalSec / 60L
        val s = totalSec % 60L
        return String.format("%02d:%02d", m, s)
    }
}
