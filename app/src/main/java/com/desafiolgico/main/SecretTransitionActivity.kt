package com.desafiolgico.main

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.desafiolgico.R
import com.desafiolgico.utils.GameDataManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SecretTransitionActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secret_transition)

        val secretLevel = intent.getStringExtra("SECRET_LEVEL")
            ?: intent.getStringExtra("level")

        if (secretLevel.isNullOrBlank()) {
            finish()
            return
        }

        // ✅ se você usa isso como “modo secreto ativo”, garante aqui também
        GameDataManager.isModoSecretoAtivo = true

        val currentStreak = intent.getIntExtra("currentStreak", 0)
        val returnToActive = intent.getBooleanExtra("RETURN_TO_ACTIVE_GAME", false)

        val animView = findViewById<LottieAnimationView>(R.id.lottieSecret)
        val tvTitle = findViewById<TextView>(R.id.tvSecretTitle)

        val titleRes = when (secretLevel) {
            GameDataManager.SecretLevels.RELAMPAGO -> R.string.secret_level_relampago_title
            GameDataManager.SecretLevels.PERFEICAO -> R.string.secret_level_perfeicao_title
            GameDataManager.SecretLevels.ENIGMA -> R.string.secret_level_enigma_title
            else -> R.string.secret_level_generic_title
        }
        tvTitle.text = getString(titleRes)

        // ✅ animação (Lottie)
        animView.setAnimation(R.raw.ic_animationcerebro)
        animView.playAnimation()

        // ✅ SOM (tem que ser mp3/ogg/wav)
        // Coloque um arquivo real em res/raw: secret_whoosh.mp3
        runCatching {
            mediaPlayer = MediaPlayer.create(this, R.raw.secret_whoosh)?.apply { start() }
        }

        // Após 3s abre a fase secreta
        lifecycleScope.launch {
            delay(3000L)
            startActivity(
                Intent(this@SecretTransitionActivity, TestActivity::class.java).apply {
                    putExtra("level", secretLevel)
                    putExtra("currentStreak", currentStreak)
                    putExtra("RETURN_TO_ACTIVE_GAME", returnToActive)
                }
            )
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
    }
}
