package com.desafiolgico.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityDailyChallengeBinding
import com.desafiolgico.model.Question
import com.desafiolgico.model.QuestionManager
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.applyEdgeToEdge
import com.google.android.material.button.MaterialButton
import kotlin.math.roundToInt

class DailyChallengeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyChallengeBinding
    private lateinit var questionManager: QuestionManager

    private val optionButtons = mutableListOf<MaterialButton>()
    private var dailyQuestions: List<Question> = emptyList()

    private var index = 0
    private var correctCount = 0
    private var visualScore = 0

    private var timer: CountDownTimer? = null
    private val perQuestionMillis = 15_000L
    private var remainingMillis = perQuestionMillis

    private var autoBackRunnable: Runnable? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()
        binding = ActivityDailyChallengeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        GameDataManager.init(this)

        // ✅ Se já fez hoje: avisa e volta pra Main
        if (GameDataManager.isDailyDone(this)) {
            Toast.makeText(
                this,
                getString(R.string.daily_come_back_tomorrow),
                Toast.LENGTH_SHORT
            ).show()
            goBackToMain()
            return
        }

        questionManager = QuestionManager(LanguageHelper.getLanguage(this))

        optionButtons.addAll(listOf(binding.opt1, binding.opt2, binding.opt3, binding.opt4))
        optionButtons.forEachIndexed { i, btn ->
            btn.setOnClickListener { onAnswer(i) }
        }

        // ✅ Voltar pro menu (sem concluir)
        binding.btnBackMenu.setOnClickListener {
            // não marca como feito — apenas sai
            goBackToMain()
        }

        dailyQuestions = buildDailyQuestions()
        if (dailyQuestions.size < 3) {
            Toast.makeText(
                this,
                getString(R.string.daily_not_enough_questions),
                Toast.LENGTH_SHORT
            ).show()
            goBackToMain()
            return
        }

        showQuestion()
    }

    private fun buildDailyQuestions(): List<Question> {
        val unlocked = GameDataManager.getUnlockedLevels(this).toMutableSet()
        unlocked.add(GameDataManager.Levels.INICIANTE)

        return GameDataManager.getDailyQuestions(
            context = this,
            questionManager = questionManager,
            unlockedLevels = unlocked
        )
    }

    private fun showQuestion() {
        resetUIForQuestion()

        val q = dailyQuestions[index]
        binding.progressText.text = "${index + 1}/3"
        binding.questionText.text = q.questionText

        q.options.forEachIndexed { i, text ->
            if (i < optionButtons.size) {
                optionButtons[i].visibility = View.VISIBLE
                optionButtons[i].text = text
            }
        }
        for (i in q.options.size until optionButtons.size) {
            optionButtons[i].visibility = View.GONE
        }

        startTimer()
    }

    private fun startTimer() {
        timer?.cancel()
        remainingMillis = perQuestionMillis

        binding.timerBar.max = 100
        binding.timerBar.progress = 100

        timer = object : CountDownTimer(perQuestionMillis, 100L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = millisUntilFinished
                val pct = ((millisUntilFinished.toDouble() / perQuestionMillis) * 100.0)
                    .roundToInt()
                    .coerceIn(0, 100)
                binding.timerBar.progress = pct
            }

            override fun onFinish() {
                remainingMillis = 0L
                binding.timerBar.progress = 0
                onTimeUp()
            }
        }.start()
    }

    private fun onTimeUp() {
        setOptionsEnabled(false)

        // Errou por tempo (não pinta nenhum específico)
        paintSelected(-1, isCorrect = false)

        binding.rootDaily.postDelayed({ next() }, 650L)
    }

    private fun onAnswer(selectedIndex: Int) {
        timer?.cancel()
        setOptionsEnabled(false)

        val q = dailyQuestions[index]
        val isCorrect = selectedIndex == q.correctAnswerIndex

        paintSelected(selectedIndex, isCorrect)

        if (isCorrect) {
            correctCount++

            // Score visual: base 10 + bônus por tempo (0..10)
            val timeBonus = ((remainingMillis.toDouble() / perQuestionMillis) * 10.0).roundToInt()
            visualScore += (10 + timeBonus)
        }

        binding.rootDaily.postDelayed({ next() }, if (isCorrect) 500L else 800L)
    }

    private fun next() {
        index++
        if (index >= 3) finishDaily() else showQuestion()
    }

    private fun finishDaily() {
        timer?.cancel()
        setOptionsEnabled(false)

        val xpEarned = when (correctCount) {
            3 -> 30
            2 -> 20
            1 -> 10
            else -> 5
        }

        GameDataManager.addXP(this, xpEarned)
        GameDataManager.saveDailyResult(this, correctCount, visualScore, xpEarned)
        GameDataManager.markDailyDone(this) // ✅ marca concluído do dia + streak diário

        val badgeRes = when (correctCount) {
            3 -> R.drawable.gold_medal
            2 -> R.drawable.silver_medal
            else -> R.drawable.ic_trophy
        }

        // ✅ Agora mostra UI de resultado e volta pra Main
        showResultUI(badgeRes, xpEarned)

        setResult(RESULT_OK)
    }

    private fun showResultUI(badgeRes: Int, xpEarned: Int) {
        // Esconde quiz
        optionButtons.forEach { it.visibility = View.GONE }
        binding.questionText.visibility = View.GONE
        binding.timerBar.visibility = View.GONE
        binding.progressText.visibility = View.GONE
        binding.subtitleDaily.visibility = View.GONE

        // Mostra resultado
        binding.resultBox.visibility = View.VISIBLE
        binding.badgeImage.visibility = View.VISIBLE
        binding.badgeImage.setImageResource(badgeRes)

        binding.resultTitle.text = getString(R.string.daily_result_title)
        binding.resultText.text = getString(
            R.string.daily_result_text_format,
            correctCount,
            visualScore
        )
        binding.xpText.text = getString(
            R.string.daily_xp_text_format,
            xpEarned,
            GameDataManager.getDailyStreak(this)
        )

        // ✅ Texto final: volte amanhã
        binding.comeBackTomorrowText?.let { tv ->
            tv.visibility = View.VISIBLE
            tv.text = getString(R.string.daily_come_back_tomorrow)
        }

        // Botão voltar
        binding.btnBackMenu.visibility = View.VISIBLE
        binding.btnBackMenu.text = getString(R.string.daily_back_to_menu)
        binding.btnBackMenu.setOnClickListener { goBackToMain() }

        // Volta automático depois de 2,5s
        autoBackRunnable?.let { binding.rootDaily.removeCallbacks(it) }
        val r = Runnable { if (!isFinishing) goBackToMain() }
        autoBackRunnable = r
        binding.rootDaily.postDelayed(r, 2500L)
    }

    private fun resetUIForQuestion() {
        binding.timerBar.visibility = View.VISIBLE
        binding.questionText.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.subtitleDaily.visibility = View.VISIBLE
        binding.resultBox.visibility = View.GONE

        // se existir no seu layout
        binding.comeBackTomorrowText?.visibility = View.GONE

        val defaultTint = ContextCompat.getColorStateList(this, R.color.button_default)
        val defaultText = ContextCompat.getColor(this, android.R.color.white)

        optionButtons.forEach {
            it.visibility = View.VISIBLE
            it.backgroundTintList = defaultTint
            it.setTextColor(defaultText)
        }
        setOptionsEnabled(true)
    }

    private fun setOptionsEnabled(enabled: Boolean) {
        optionButtons.forEach { it.isEnabled = enabled }
    }

    private fun paintSelected(selected: Int, isCorrect: Boolean) {
        val white = ContextCompat.getColor(this, android.R.color.white)
        val defaultText = ContextCompat.getColor(this, R.color.background_color)
        val defaultTint = ContextCompat.getColorStateList(this, R.color.button_default)
        val correctTint = ContextCompat.getColorStateList(this, R.color.correctAnswerColor)
        val wrongTint = ContextCompat.getColorStateList(this, R.color.wrongAnswerColor)

        optionButtons.forEachIndexed { i, b ->
            b.backgroundTintList = defaultTint
            b.setTextColor(defaultText)

            if (i == selected) {
                b.backgroundTintList = if (isCorrect) correctTint else wrongTint
                b.setTextColor(white)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Se tá no resultado, volta pra main
        if (binding.resultBox.visibility == View.VISIBLE) {
            goBackToMain()
        } else {
            super.onBackPressed()
        }
    }

    private fun goBackToMain() {
        autoBackRunnable?.let { binding.rootDaily.removeCallbacks(it) }
        autoBackRunnable = null

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        finish()
    }

    override fun onPause() {
        super.onPause()
        timer?.cancel()
        autoBackRunnable?.let { binding.rootDaily.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        timer = null
        autoBackRunnable = null
    }
}
