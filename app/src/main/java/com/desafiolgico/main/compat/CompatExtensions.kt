package com.desafiolgico.main.compat

import android.content.Intent
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.desafiolgico.main.ads.AdsController
import com.desafiolgico.main.ui.ScoreUiController
import com.desafiolgico.main.ui.TestUi
import com.desafiolgico.weekly.WeeklyController
import com.desafiolgico.model.Question

/**
 * COMPAT LAYER:
 * - evita "Unresolved reference" sem você precisar mudar suas classes reais agora.
 * - depois, quando você me mandar os métodos reais, eu substituo estes corpos.
 */

// -------------------- ScoreUiController --------------------
fun ScoreUiController.bind(binding: Any) {
    // TODO: conectar com o seu método real (ou remover se não usar)
}

fun ScoreUiController.startObserving(activity: AppCompatActivity) {
    // TODO: conectar com o seu método real (LiveData / etc)
}

// -------------------- AdsController --------------------
fun AdsController.bind(activity: AppCompatActivity, adContainer: ViewGroup) {
    // TODO: conectar com o seu método real
}

fun AdsController.prepare() {
    // TODO: conectar com o seu método real (setup banner/rewarded)
}

fun AdsController.onResume() {
    // TODO
}

fun AdsController.onPause() {
    // TODO
}

fun AdsController.onDestroy() {
    // TODO
}

/**
 * Mostra rewarded e chama onClosed sempre.
 * Assim seu fluxo compila e funciona mesmo sem Ads implementado.
 */
fun AdsController.showRewardedIfAvailable(
    onReward: (coins: Int) -> Unit = {},
    onClosed: () -> Unit
) {
    // TODO: ligar na sua rewarded real
    onClosed()
}

// -------------------- WeeklyController --------------------
fun WeeklyController.bind(activity: AppCompatActivity, ui: TestUi) {
    // TODO
}

fun WeeklyController.beginFromIntent(intent: Intent) {
    // TODO
}

/**
 * Start weekly mode compat:
 * - Você ajusta depois para chamar seu método real.
 */
fun WeeklyController.startWeeklyMode(
    onQuestionsReady: (List<Question>) -> Unit,
    onError: (String) -> Unit
) {
    // TODO: ligar no seu weekly real
    onError("WeeklyController.startWeeklyMode ainda não foi conectado (Compat).")
}

/**
 * WeekId compat:
 * - se seu weekly tiver weekId em outro lugar, conecta depois.
 */
fun WeeklyController.weekId(): String = ""

/**
 * Hud compat
 */
fun WeeklyController.updateHud(correct: Int, wrong: Int) {
    // TODO
}

fun WeeklyController.submitWeeklyResult(
    weekId: String,
    correct: Int,
    wrong: Int,
    timeMs: Long,
    backgroundCount: Int,
    backgroundTotalMs: Long
) {
    // TODO
}

fun WeeklyController.startWeeklyRanking(weekId: String) {
    // TODO
}
