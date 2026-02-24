package com.desafiolgico.main.flow

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.desafiolgico.main.TestActivity
import com.desafiolgico.main.ads.AdsController
import com.desafiolgico.main.quiz.QuizEngine
import com.desafiolgico.main.quiz.TimerController
import com.desafiolgico.main.ui.TestUi
import com.desafiolgico.weekly.WeeklyController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

class TestCoordinator(
    private val activity: AppCompatActivity,
    private val ui: TestUi,
    private val engine: QuizEngine,
    private val timer: TimerController,
    private val ads: AdsController,
    private val weekly: WeeklyController,
) : CoroutineScope by MainScope() {

    private var isWeeklyMode: Boolean = false
    private var started: Boolean = false

    fun start(intent: Intent) {
        if (started) return
        started = true

        val mode = intent.getStringExtra(TestActivity.EXTRA_MODE) ?: "NORMAL"
        isWeeklyMode = mode.equals("WEEKLY", ignoreCase = true)

        if (isWeeklyMode) {
            // valida rápido
            val weekId = intent.getStringExtra("WEEK_ID").orEmpty()
            if (weekId.isBlank()) {
                ui.showToast("Semana inválida.")
                activity.finish()
                return
            }

            weekly.beginFromIntent(intent)
            weekly.startWeeklyMode(
                onFail = { msg ->
                    ui.showToast(msg)
                    activity.finish()
                }
            )
            return
        }

        // NORMAL: a Activity controla o display inicial com:
        // binding.root.post { engine.displayQuestion(withEnterAnim = true) }
    }

    /** Clique das opções (use se você quiser centralizar aqui) */
    fun onOptionClicked(index: Int) {
        if (ui.isAnswerLocked()) return
        engine.checkAnswer(index)
    }

    // lifecycle
    fun onStart() {
        if (isWeeklyMode) weekly.onStart() // background timing só no weekly
    }

    fun onStop() {
        if (isWeeklyMode) weekly.onStop()  // background timing só no weekly
    }

    fun onResume() {
        ads.onResume()
        // ⚠️ não forçar timer.resume() aqui.
        // O engine/timer controller devem decidir quando o timer está rodando.
        // Isso evita “reviver” timer após resposta/pausa intencional.
    }

    fun onPause() {
        ads.onPause()
        // se o app sair da tela, pausa o timer por segurança
        timer.pause()
    }

    fun onDestroy() {
        ads.onDestroy()
        timer.cancel()
        cancel()
    }
}
