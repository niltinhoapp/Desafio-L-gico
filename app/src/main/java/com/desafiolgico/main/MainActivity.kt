package com.desafiolgico.main

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.desafiolgico.R
import com.google.android.gms.ads.MobileAds
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    companion object {
        const val GAME_PREFS = "game_prefs"
        const val UNLOCKED_LEVEL_KEY = "unlocked_levels"
        const val LEVEL_INTERMEDIATE = "Intermediário"
        const val LEVEL_ADVANCED = "Avançado"

    }

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var startForResult: ActivityResultLauncher<Intent>
    private lateinit var lottieAnimationView: LottieAnimationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar views
        lottieAnimationView = findViewById(R.id.lottieAnimationView)

        // Solicitar permissão para notificações (Android 13 ou superior)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }

        // Inicializar o AdMob
        MobileAds.initialize(this) {
            Log.d("MainActivity", "AdMob initialized")
        }

        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences(GAME_PREFS, MODE_PRIVATE)

        // Configurar a animação de inicialização
        setupAnimation()

        // Configurar botões e estados
        setupButtons()
        updateButtonStates()

        // Inicializar música de fundo
        initBackgroundMusic()

        // Configurar o ActivityResultLauncher para a atividade de teste
        setupActivityResultLauncher()


    }

    private fun setupActivityResultLauncher() {
        startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val score = data?.getIntExtra("score", 0) ?: 0
                val level = data?.getStringExtra("level") ?: ""
                // Processar os dados recebidos
                Log.d("ActivityResult", "Level: $level, Score: $score")

                // Desbloquear o próximo nível com base na pontuação
                unlockNextLevel(level, score)
            }
        }
    }

    private fun unlockNextLevel(currentLevel: String, score: Int) {
        val unlockedLevels = sharedPreferences.getStringSet(UNLOCKED_LEVEL_KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        when (currentLevel) {
            getString(R.string.level_beginner) -> {
                if (score >= 80) {
                    unlockedLevels.add(LEVEL_INTERMEDIATE)
                }
            }
            getString(R.string.level_intermediate) -> {
                if (score >= 80) {
                    unlockedLevels.add(LEVEL_ADVANCED)
                }
            }
        }
        sharedPreferences.edit().putStringSet(UNLOCKED_LEVEL_KEY, unlockedLevels).apply()
        updateButtonStates()
    }

    private fun setupAnimation() {
        val mainContent = findViewById<LinearLayout>(R.id.mainContent)

        lottieAnimationView.setAnimation(R.raw.airplane_explosion1)
        lottieAnimationView.visibility = View.VISIBLE
        mainContent.visibility = View.GONE

        lottieAnimationView.repeatCount = 1
        lottieAnimationView.invalidate()
        lottieAnimationView.requestLayout()
        lottieAnimationView.playAnimation()

        lottieAnimationView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                Log.d("Animation", "Animation started")
            }

            override fun onAnimationEnd(animation: Animator) {
                Log.d("Animation", "Animation ended")
                lottieAnimationView.visibility = View.GONE
                mainContent.visibility = View.VISIBLE
            }

            override fun onAnimationCancel(animation: Animator) {
                Log.d("Animation", "Animation canceled")
            }

            override fun onAnimationRepeat(animation: Animator) {
                Log.d("Animation", "Animation repeated")
            }
        })
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    private fun setupButtons() {
        val beginnerButton = findViewById<MaterialButton>(R.id.beginnerButton)
        val intermediateButton = findViewById<MaterialButton>(R.id.intermediateButton)
        val advancedButton = findViewById<MaterialButton>(R.id.advancedButton)
        val exitButton = findViewById<MaterialButton>(R.id.exitButton)

        beginnerButton.setOnClickListener {
            handleButtonClick(beginnerButton, getString(R.string.level_beginner))
        }

        intermediateButton.setOnClickListener {
            if (isLevelUnlocked(LEVEL_INTERMEDIATE)) {
                handleButtonClick(intermediateButton, getString(R.string.level_intermediate))
            } else {
                showLevelLockedMessage()
            }
        }

        advancedButton.setOnClickListener {
            if (isLevelUnlocked(LEVEL_ADVANCED)) {
                handleButtonClick(advancedButton, getString(R.string.level_advanced))
            } else {
                showLevelLockedMessage()
            }
        }

        exitButton.setOnClickListener {
            showExitConfirmationDialog()
        }
    }

    private fun updateButtonStates() {
        updateButtonState(R.id.intermediateButton, LEVEL_INTERMEDIATE)
        updateButtonState(R.id.advancedButton, LEVEL_ADVANCED)
    }

    private fun updateButtonState(buttonId: Int, level: String) {
        findViewById<MaterialButton>(buttonId).apply {
            isEnabled = isLevelUnlocked(level)
            setBackgroundResource(if (isEnabled) R.color.wrongAnswerColor else R.color.button_default)
            setCompoundDrawablesWithIntrinsicBounds(
                if (isEnabled) 0 else R.drawable.ic_lock,
                0,
                0,
                0
            )
        }
    }

    private fun initBackgroundMusic() {
        mediaPlayer = MediaPlayer.create(this, R.raw.background_music).apply {
            isLooping = true
            start()
        }
    }

    private fun handleButtonClick(button: MaterialButton, level: String) {
        playClickSound()
        animateButton(button)
        resetButtonColors()
        changeButtonColor(button)
        if (level != "exit") {
            navigateToTestActivity(level)
        }
    }

    private fun changeButtonColor(button: MaterialButton) {
        button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_selected)
    }

    private fun resetButtonColors() {
        val buttons = listOf(
            R.id.beginnerButton,
            R.id.intermediateButton,
            R.id.advancedButton,
            R.id.exitButton
        )
        buttons.forEach { id ->
            findViewById<MaterialButton>(id).backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.button_default)
        }
    }

    private fun playClickSound() {
        MediaPlayer.create(this, R.raw.click_sound).apply {
            setOnCompletionListener { release() }
            start()
        }
    }

    private fun animateButton(button: MaterialButton) {
        val scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 1.1f, 1f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 300
            start()
        }
    }

    private fun isLevelUnlocked(level: String): Boolean {
        val unlockedLevels =
            sharedPreferences.getStringSet(UNLOCKED_LEVEL_KEY, emptySet()) ?: emptySet()
        return unlockedLevels.contains(level)
    }

    private fun showLevelLockedMessage() {
        AlertDialog.Builder(this).apply {
            setTitle("Nível Blockhead")
            setMessage("Complete o nível anterior para desbloquear este.")
            setPositiveButton("OK", null)
            show()
        }
    }

    private fun navigateToTestActivity(level: String) {
        val intent = Intent(this, TestActivity::class.java).apply {
            intent.putExtra("level", level)
        }
        startForResult.launch(intent)
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this).apply {
            setTitle("Sair")
            setMessage("Você tem certeza que deseja sair?")
            setPositiveButton("Sim") { _, _ -> finish() }
            setNegativeButton("Não", null)
            show()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::lottieAnimationView.isInitialized) {
            lottieAnimationView.pauseAnimation()
        }
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::lottieAnimationView.isInitialized) {
            lottieAnimationView.resumeAnimation()
        }
        if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::lottieAnimationView.isInitialized) {
            lottieAnimationView.cancelAnimation()
        }
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }
}