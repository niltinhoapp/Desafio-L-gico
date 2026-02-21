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

    fun start(intent: Intent) {
        val mode = intent.getStringExtra(TestActivity.EXTRA_MODE) ?: "NORMAL"

        if (mode.equals("WEEKLY", ignoreCase = true)) {
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
        weekly.onStart() // ✅ importante pro weekly (background timing)
    }

    fun onStop() {
        weekly.onStop()  // ✅ importante pro weekly (background timing)
    }

    fun onResume() {
        ads.onResume()
        timer.resume()
    }

    fun onPause() {
        ads.onPause()
        timer.pause()
    }

    fun onDestroy() {
        ads.onDestroy()
        timer.cancel()
        cancel()
    }
}
