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
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.desafiolgico.R
import com.desafiolgico.settings.SettingsActivity
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.applyEdgeToEdge
import com.google.android.material.button.MaterialButton
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var levelManager: LevelManager
    private lateinit var startForResult: ActivityResultLauncher<Intent>
    private lateinit var btnSettingsMain: MaterialButton


    // Cards/layout novo
    private lateinit var menuCard: View
    private lateinit var dailyCard: View

    // Daily
    private lateinit var dailyLottie: LottieAnimationView
    private lateinit var dailyLabel: android.widget.TextView
    private var hideDailyLabelRunnable: Runnable? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()
        setContentView(R.layout.activity_main)

        // ✅ init primeiro
        GameDataManager.init(this)

        btnSettingsMain = findViewById(R.id.btnSettingsMain)
        btnSettingsMain.visibility = View.GONE

        btnSettingsMain.setOnClickListener {
            playClickSound()
            animateTap(btnSettingsMain)
            startActivity(Intent(this, SettingsActivity::class.java))
        }



        // Views (layout novo)
        lottieAnimationView = findViewById(R.id.lottieAnimationView)
        menuCard = findViewById(R.id.menuCard)

        dailyCard = findViewById(R.id.dailyChallengeCard)
        dailyLottie = findViewById(R.id.btnDailyChallenge)
        dailyLabel = findViewById(R.id.txtDailyLabel)

        levelManager = LevelManager(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }

        // Esconde durante intro
        menuCard.visibility = View.GONE
        dailyCard.visibility = View.GONE

        // ✅ clique do daily (card e lottie)
        val dailyClick = View.OnClickListener {
            playClickSound()

            if (GameDataManager.isDailyDone(this)) {
                showDailyTempMessage(getString(R.string.daily_come_back_tomorrow))
                return@OnClickListener
            }

            startActivity(Intent(this, DailyChallengeActivity::class.java))
        }
        dailyCard.setOnClickListener(dailyClick)
        dailyLottie.setOnClickListener(dailyClick)

        setupAnimation()
        initBackgroundMusic()
        setupActivityResultLauncher()

        // Botões de nível
        val beginnerButton = findViewById<MaterialButton>(R.id.beginnerButton)
        val intermediateButton = findViewById<MaterialButton>(R.id.intermediateButton)
        val advancedButton = findViewById<MaterialButton>(R.id.advancedButton)
        val expertButton = findViewById<MaterialButton>(R.id.expertButton)
        val exitButton = findViewById<MaterialButton>(R.id.exitButton)

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

        levelManager.updateButtonStates(intermediateButton, advancedButton, expertButton)
    }

    private fun setupActivityResultLauncher() {
        startForResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val intermediateButton = findViewById<MaterialButton>(R.id.intermediateButton)
                    val advancedButton = findViewById<MaterialButton>(R.id.advancedButton)
                    val expertButton = findViewById<MaterialButton>(R.id.expertButton)
                    levelManager.updateButtonStates(intermediateButton, advancedButton, expertButton)

                    updateDailyLabelState()
                    updateDailyUI()
                }
            }
    }

    private fun setupAnimation() {
        lottieAnimationView.setAnimation(R.raw.airplane_explosion1)
        lottieAnimationView.visibility = View.VISIBLE
        lottieAnimationView.repeatCount = 0
        lottieAnimationView.playAnimation()
        lottieAnimationView.removeAllAnimatorListeners()

        lottieAnimationView.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) = finishIntro()
            override fun onAnimationCancel(animation: Animator) = finishIntro()
        })
    }

    private fun finishIntro() {
        lottieAnimationView.visibility = View.GONE

        // Mostra card do menu com animação suave
        showWithFadeSlide(menuCard, fromDpY = 24f, delay = 0L, duration = 260L)

        // Mostra daily card com animação (subindo de leve)
        showWithFadeSlide(dailyCard, fromDpY = -12f, delay = 120L, duration = 220L)

        showWithFadeSlide(btnSettingsMain, fromDpY = -10f, delay = 80L, duration = 180L)
        startGlow(btnSettingsMain)


        updateDailyLabelState()
        updateDailyUI()
    }

    private fun showWithFadeSlide(view: View, fromDpY: Float, delay: Long, duration: Long) {
        val fromPx = dp(fromDpY)

        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.translationY = fromPx

        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delay)
            .setDuration(duration)
            .start()
    }
    private fun animateTap(view: View) {
        view.animate().cancel()
        view.scaleX = 1f
        view.scaleY = 1f
        view.animate()
            .scaleX(0.92f).scaleY(0.92f)
            .setDuration(90)
            .withEndAction {
                view.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(130)
                    .start()
            }
            .start()
    }

    private fun startGlow(view: View) {
        view.animate().cancel()
        view.alpha = 1f

        // glow bem leve (não some, só “respira”)
        view.animate()
            .alpha(0.78f)
            .setDuration(900)
            .withEndAction {
                view.animate()
                    .alpha(1f)
                    .setDuration(900)
                    .withEndAction { startGlow(view) }
                    .start()
            }
            .start()
    }

    private fun stopGlow(view: View) {
        view.animate().cancel()
        view.alpha = 1f
    }


    private fun dp(value: Float): Float {
        return (value * resources.displayMetrics.density)
    }

    private fun updateDailyLabelState() {
        val done = GameDataManager.isDailyDone(this)

        hideDailyLabelRunnable?.let { dailyLabel.removeCallbacks(it) }
        hideDailyLabelRunnable = null

        dailyLabel.visibility = View.VISIBLE

        if (done) {
            dailyLabel.text = getString(R.string.daily_done_today)
            dailyLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            dailyLabel.alpha = 0.60f
        } else {
            dailyLabel.text = getString(R.string.daily_label)
            dailyLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            dailyLabel.alpha = 0.92f
        }
    }

    private fun showDailyTempMessage(msg: String) {
        dailyLabel.visibility = View.VISIBLE
        dailyLabel.text = msg

        dailyLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        dailyLabel.alpha = 0f
        dailyLabel.animate().alpha(0.92f).setDuration(180).start()

        hideDailyLabelRunnable?.let { dailyLabel.removeCallbacks(it) }

        val r = Runnable {
            dailyLabel.animate().alpha(0f).setDuration(180).withEndAction {
                updateDailyLabelState()
            }.start()
        }
        hideDailyLabelRunnable = r
        dailyLabel.postDelayed(r, 3000L)
    }

    private fun updateDailyUI() {
        val done = GameDataManager.isDailyDone(this)
        val streak = GameDataManager.getDailyStreak(this)

        dailyLottie.alpha = if (done) 0.55f else 1f

        // chama atenção só quando estiver disponível
        dailyCard.animate().cancel()
        if (!done) {
            dailyCard.animate()
                .scaleX(1.03f).scaleY(1.03f)
                .setDuration(650)
                .withEndAction {
                    dailyCard.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(650)
                        .start()
                }
                .start()
        } else {
            dailyCard.scaleX = 1f
            dailyCard.scaleY = 1f
        }

        dailyLottie.contentDescription =
            if (done) "Desafio diário concluído. Streak $streak"
            else "Abrir desafio diário. Streak $streak"
    }


    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.notifications_enabled), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.notifications_denied), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initBackgroundMusic() {
        mediaPlayer = MediaPlayer.create(this, R.raw.background_music).apply {
            isLooping = true
            start()
        }
    }

    private fun onLevelLocked(level: String) { /* sem toast duplicado */ }

    private fun handleButtonClick(button: MaterialButton, level: String) {
        playClickSound()
        animateButton(button)

        // Só muda cor dos botões de nível (exit é outlined)
        resetLevelButtonColors()
        changeButtonColor(button)

        if (level == GameDataManager.Levels.EXPERIENTE) {
            startActivity(Intent(this, ExpertChallengeActivity::class.java))
            finish()
        } else {
            navigateToTestActivity(level)
        }
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.exit_dialog_title))
            .setMessage(getString(R.string.exit_dialog_message))
            .setPositiveButton(getString(R.string.answer_yes)) { _, _ -> finish() }
            .setNegativeButton(getString(R.string.answer_no), null)
            .show()
    }

    private fun resetLevelButtonColors() {
        val levelButtonIds = listOf(
            R.id.beginnerButton,
            R.id.intermediateButton,
            R.id.advancedButton,
            R.id.expertButton
        )
        for (id in levelButtonIds) {
            findViewById<MaterialButton>(id)?.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.button_default)
        }
    }

    private fun changeButtonColor(button: MaterialButton) {
        // Só aplica seleção se for botão de nível
        val isLevelButton = button.id in listOf(
            R.id.beginnerButton,
            R.id.intermediateButton,
            R.id.advancedButton,
            R.id.expertButton
        )
        if (isLevelButton) {
            button.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.button_selected)
        }
    }

    private fun playClickSound() {
        MediaPlayer.create(this, R.raw.click_sound).apply {
            setOnCompletionListener { mp -> mp.release() }
            start()
        }
    }

    private fun animateButton(button: MaterialButton) {
        // Animação mais “premium” (leve)
        val scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.98f, 1f)
        val scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.98f, 1f)
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 170
            start()
        }
    }

    private fun navigateToTestActivity(level: String) {
        val intent = Intent(this, TestActivity::class.java).apply {
            putExtra("level", level)
        }
        startForResult.launch(intent)
    }

    override fun onPause() {
        super.onPause()
        stopGlow(btnSettingsMain)

        hideDailyLabelRunnable?.let { dailyLabel.removeCallbacks(it) }
        hideDailyLabelRunnable = null
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) mediaPlayer.pause()
        if (::lottieAnimationView.isInitialized && lottieAnimationView.isAnimating) {
            lottieAnimationView.pauseAnimation()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::btnSettingsMain.isInitialized && btnSettingsMain.visibility == View.VISIBLE) {
            startGlow(btnSettingsMain)
        }
        updateDailyLabelState()
        updateDailyUI()
        if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) mediaPlayer.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
        if (::lottieAnimationView.isInitialized) lottieAnimationView.cancelAnimation()
    }
}
