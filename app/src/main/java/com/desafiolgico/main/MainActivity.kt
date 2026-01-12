package com.desafiolgico.main

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.desafiolgico.R
import com.desafiolgico.settings.SettingsActivity
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.LocalRecordsManager
import com.desafiolgico.utils.applyEdgeToEdge
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    // Audio / Intro
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var lottieAnimationView: LottieAnimationView

    // Managers
    private lateinit var levelManager: LevelManager

    // Launchers
    private lateinit var startForResult: ActivityResultLauncher<Intent>
    private lateinit var requestNotifLauncher: ActivityResultLauncher<String>

    // Layout
    private lateinit var menuCard: View
    private lateinit var dailyCard: View

    // Daily
    private lateinit var dailyLottie: LottieAnimationView
    private lateinit var dailyLabel: TextView
    private var hideDailyLabelRunnable: Runnable? = null

    // Top buttons
    private lateinit var btnSettingsMain: MaterialButton
    private lateinit var btnRecordsMain: MaterialButton

    // Level buttons (campos da Activity => acessíveis no onResume)
    private lateinit var beginnerButton: MaterialButton
    private lateinit var intermediateButton: MaterialButton
    private lateinit var advancedButton: MaterialButton
    private lateinit var expertButton: MaterialButton
    private lateinit var exitButton: MaterialButton
    private lateinit var mapButton: MaterialButton

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()
        setContentView(R.layout.activity_main)

        // ✅ init primeiro
        GameDataManager.init(this)

        // ✅ Launchers primeiro (pra não esquecer)
        setupActivityResultLauncher()
        setupNotificationLauncher()

        // Views
        lottieAnimationView = findViewById(R.id.lottieAnimationView)
        menuCard = findViewById(R.id.menuCard)

        dailyCard = findViewById(R.id.dailyChallengeCard)
        dailyLottie = findViewById(R.id.btnDailyChallenge)
        dailyLabel = findViewById(R.id.txtDailyLabel)

        btnRecordsMain = findViewById(R.id.btnRecordsMain)
        btnSettingsMain = findViewById(R.id.btnSettingsMain)

        beginnerButton = findViewById(R.id.beginnerButton)
        intermediateButton = findViewById(R.id.intermediateButton)
        advancedButton = findViewById(R.id.advancedButton)
        expertButton = findViewById(R.id.expertButton)
        exitButton = findViewById(R.id.exitButton)
        mapButton = findViewById(R.id.mapButton)

        levelManager = LevelManager(this)

        // Permissão notificação (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }

        // Esconde durante intro
        menuCard.visibility = View.GONE
        dailyCard.visibility = View.GONE
        btnRecordsMain.visibility = View.GONE
        btnSettingsMain.visibility = View.GONE

        // ✅ Recordes (compacto)
        btnRecordsMain.setOnClickListener {
            playClickSound()
            animateTap(btnRecordsMain)
            startActivity(Intent(this, LocalRecordsActivity::class.java))


        }

        // ✅ Settings
        btnSettingsMain.setOnClickListener {
            playClickSound()
            animateTap(btnSettingsMain)
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // ✅ clique do daily (card e lottie)
        val dailyClick = View.OnClickListener {
            playClickSound()
            animateTap(dailyCard)

            if (GameDataManager.isDailyDone(this)) {
                showDailyTempMessage(getString(R.string.daily_come_back_tomorrow))
                return@OnClickListener
            }
            startActivity(Intent(this, DailyChallengeActivity::class.java))
        }
        dailyCard.setOnClickListener(dailyClick)
        dailyLottie.setOnClickListener(dailyClick)

        // ✅ Map
        mapButton.setOnClickListener {
            playClickSound()
            animateTap(mapButton)
            startActivity(Intent(this, MapActivity::class.java))
        }

        // Setup botões de nível
        levelManager.setupButtons(
            beginnerButton,
            intermediateButton,
            advancedButton,
            expertButton,
            exitButton,
            ::handleButtonClick,
            ::onLevelLocked,
            ::showExitConfirmationDialog
        )

        // ✅ Atualiza estados já no create
        levelManager.updateButtonStates(intermediateButton, advancedButton, expertButton)

        // UI diária + recordes
        updateDailyLabelState()
        updateDailyUI()
        updateRecordsButtonText()

        // Intro + música
        setupAnimation()
        initBackgroundMusic()
    }

    override fun onResume() {
        super.onResume()

        // ✅ TEMPO REAL: voltou pra Main => recalcula desbloqueios e atualiza botões
        if (::levelManager.isInitialized
            && ::intermediateButton.isInitialized
            && ::advancedButton.isInitialized
            && ::expertButton.isInitialized
        ) {
            levelManager.updateButtonStates(intermediateButton, advancedButton, expertButton)
        }

        // ✅ Mostra aviso somente quando desbloqueou pela primeira vez
        notifyNewUnlocksIfAny()
        // ✅ Atualiza recordes / daily sempre que voltar
        updateRecordsButtonText()
        updateDailyLabelState()
        updateDailyUI()

        // Glow se estiver visível
        if (::btnSettingsMain.isInitialized && btnSettingsMain.visibility == View.VISIBLE) {
            startGlow(btnSettingsMain)
        }

        // Música
        if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) mediaPlayer.start()
    }

    override fun onPause() {
        super.onPause()
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) mediaPlayer.pause()
        hideDailyLabelRunnable?.let { dailyLabel.removeCallbacks(it) }
    }

    override fun onDestroy() {
        try {
            if (::mediaPlayer.isInitialized) {
                mediaPlayer.stop()
                mediaPlayer.release()
            }
        } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun unlockNotifPrefs() =
        getSharedPreferences("unlock_notifs", MODE_PRIVATE)

    private fun notifyNewUnlocksIfAny() {
        val p = unlockNotifPrefs()

        // 👇 Se o LevelManager usa outro comportamento visual, a forma MAIS segura
        // é basear no "isEnabled" (ou "alpha") depois do updateButtonStates()
        checkAndNotifyUnlock(
            key = "unlock_intermediate_notified",
            isUnlockedNow = intermediateButton.isEnabled,
            message = "🔥 NOVO NÍVEL DESBLOQUEADO: INTERMEDIÁRIO!",
            targetView = intermediateButton
        )

        checkAndNotifyUnlock(
            key = "unlock_advanced_notified",
            isUnlockedNow = advancedButton.isEnabled,
            message = "⭐ NOVO NÍVEL DESBLOQUEADO: AVANÇADO!",
            targetView = advancedButton
        )

        checkAndNotifyUnlock(
            key = "unlock_expert_notified",
            isUnlockedNow = expertButton.isEnabled,
            message = "👑 NOVO NÍVEL DESBLOQUEADO: EXPERIENTE!",
            targetView = expertButton
        )
    }

    private fun checkAndNotifyUnlock(
        key: String,
        isUnlockedNow: Boolean,
        message: String,
        targetView: View
    ) {
        if (!isUnlockedNow) return

        val p = unlockNotifPrefs()
        val alreadyNotified = p.getBoolean(key, false)
        if (alreadyNotified) return

        // marca que já avisou
        p.edit().putBoolean(key, true).apply()

        // ✅ aviso
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        // ✅ animação “AAA” simples e bonita no botão
        bounceUnlock(targetView)
        glowUnlock(targetView)
    }

    private fun bounceUnlock(v: View) {
        v.animate().cancel()
        v.scaleX = 1f
        v.scaleY = 1f
        v.animate()
            .scaleX(1.08f).scaleY(1.08f)
            .setDuration(160)
            .withEndAction {
                v.animate().scaleX(1f).scaleY(1f).setDuration(220).start()
            }
            .start()
    }

    private fun glowUnlock(v: View) {
        // brilho rápido (sem precisar de recurso)
        v.alpha = 1f
        v.animate().cancel()
        v.animate()
            .alpha(0.75f)
            .setDuration(120)
            .withEndAction {
                v.animate().alpha(1f).setDuration(180).start()
            }
            .start()
    }

    // =============================================================================================
    // Launchers
    // =============================================================================================

    private fun setupActivityResultLauncher() {
        startForResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                // Quando volta do jogo, o onResume já atualiza tudo.
            }
    }

    private fun setupNotificationLauncher() {
        requestNotifLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (!granted) {
                    // opcional: toast
                }
            }
    }

    private fun requestNotificationPermission() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

        if (!granted) {
            requestNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // =============================================================================================
    // Click handlers
    // =============================================================================================

    private fun handleButtonClick(btn: MaterialButton, level: String) {
        playClickSound()
        animateTap(btn)
        startForResult.launch(
            Intent(this, TestActivity::class.java).apply {
                putExtra("level", level)
            }
        )
    }

    private fun onLevelLocked(level: String) {
        // opcional: analytics/feedback
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.exit_confirm_title))
            .setMessage(getString(R.string.exit_confirm_message))
            .setPositiveButton(getString(R.string.exit_confirm_yes)) { _, _ -> finish() }
            .setNegativeButton(getString(R.string.exit_confirm_no), null)
            .show()
    }

    // =============================================================================================
    // UI updates (Recordes / Daily)
    // =============================================================================================

    private fun updateRecordsButtonText() {
        if (!::btnRecordsMain.isInitialized) return

        val bestToday = LocalRecordsManager.getBestStreakOfDay(this)
        btnRecordsMain.text = if (bestToday > 0) "Recordes (🔥$bestToday)" else "Recordes"

        // se já passou da intro, deixa visível
        if (menuCard.visibility == View.VISIBLE) {
            btnRecordsMain.visibility = View.VISIBLE
            btnSettingsMain.visibility = View.VISIBLE
        }
    }

    private fun updateDailyLabelState() {
        if (!::dailyLabel.isInitialized) return

        dailyLabel.text = if (GameDataManager.isDailyDone(this)) {
            getString(R.string.daily_done_label) // ex: "Concluído hoje ✅"
        } else {
            getString(R.string.daily_available_label) // ex: "Disponível hoje 🎯"
        }
    }

    private fun updateDailyUI() {
        if (!::dailyCard.isInitialized) return

        val done = GameDataManager.isDailyDone(this)
        dailyCard.alpha = if (done) 0.7f else 1f
    }

    private fun showDailyTempMessage(msg: String) {
        dailyLabel.text = msg
        dailyLabel.visibility = View.VISIBLE

        hideDailyLabelRunnable?.let { dailyLabel.removeCallbacks(it) }
        hideDailyLabelRunnable = Runnable {
            updateDailyLabelState()
        }.also { dailyLabel.postDelayed(it, 1700L) }
    }

    // =============================================================================================
    // Intro + animações + música
    // =============================================================================================

    private fun setupAnimation() {
        // Se você já tem uma animação pronta, pode manter a sua e só chamar showMenu()
        lottieAnimationView.visibility = View.VISIBLE
        lottieAnimationView.playAnimation()

        lottieAnimationView.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                showMenu()
            }
            override fun onAnimationCancel(animation: Animator) {
                showMenu()
            }
        })
    }

    private fun showMenu() {
        lottieAnimationView.visibility = View.GONE
        menuCard.visibility = View.VISIBLE
        dailyCard.visibility = View.VISIBLE
        btnRecordsMain.visibility = View.VISIBLE
        btnSettingsMain.visibility = View.VISIBLE

        // ✅ garante que quando a intro acabar, já esteja atualizado
        levelManager.updateButtonStates(intermediateButton, advancedButton, expertButton)
        updateRecordsButtonText()
        updateDailyLabelState()
        updateDailyUI()
    }

    private fun initBackgroundMusic() {
        // Troque o raw se seu arquivo tiver outro nome
        mediaPlayer = MediaPlayer.create(this, R.raw.background_music)
        mediaPlayer.isLooping = true
        mediaPlayer.setVolume(0.5f, 0.5f)
        mediaPlayer.start()
    }

    // =============================================================================================
    // FX helpers
    // =============================================================================================

    private fun animateTap(v: View) {
        v.animate().cancel()
        v.scaleX = 1f
        v.scaleY = 1f
        v.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(70)
            .withEndAction {
                v.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .setInterpolator(OvershootInterpolator(0.8f))
                    .start()
            }
            .start()
    }

    private fun startGlow(v: View) {
        v.animate().cancel()
        val a1 = ObjectAnimator.ofFloat(v, View.ALPHA, 1f, 0.7f).apply { duration = 450 }
        val a2 = ObjectAnimator.ofFloat(v, View.ALPHA, 0.7f, 1f).apply { duration = 450 }
        AnimatorSet().apply {
            interpolator = AccelerateDecelerateInterpolator()
            playSequentially(a1, a2)
            start()
        }
    }

    private fun playClickSound() {
        // Opcional: se você tiver R.raw.click_sound, use ele.
        // Se não tiver, pode deixar vazio.
        try {
            val mp = MediaPlayer.create(this, R.raw.click_sound)
            mp.setOnCompletionListener { it.release() }
            mp.start()
        } catch (_: Exception) { }
    }

    // util opcional (caso use em algum lugar)
    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
