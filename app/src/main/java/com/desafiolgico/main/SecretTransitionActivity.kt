package com.desafiolgico.main

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
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

        // Compatível: aceita "SECRET_LEVEL" (padrão) ou "level" (fallback)
        val secretLevel = intent.getStringExtra("SECRET_LEVEL")
            ?: intent.getStringExtra("level")

        if (secretLevel.isNullOrBlank()) {
            finish()
            return
        }

        val animView = findViewById<LottieAnimationView>(R.id.lottieSecret)

        val titleRes = when (secretLevel) {
            GameDataManager.SecretLevels.RELAMPAGO -> R.string.secret_level_relampago_title
            GameDataManager.SecretLevels.PERFEICAO -> R.string.secret_level_perfeicao_title
            GameDataManager.SecretLevels.ENIGMA -> R.string.secret_level_enigma_title
            else -> R.string.secret_level_generic_title
        }

        findViewById<android.widget.TextView>(R.id.tvSecretTitle).text = getString(titleRes)

        animView.setAnimation(R.raw.ic_animationcerebro)
        animView.playAnimation()

        mediaPlayer = MediaPlayer.create(this, R.raw.ic_animationcerebro)?.apply {
            setOnCompletionListener { mp ->
                mp.release()
                if (mediaPlayer === mp) mediaPlayer = null
            }
            start()
        }

        // Após 3s, inicia a fase secreta
        lifecycleScope.launch {
            delay(3000L)
            startActivity(
                Intent(this@SecretTransitionActivity, TestActivity::class.java).apply {
                    putExtra("level", secretLevel)
                }
            )
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.let {
            runCatching {
                if (it.isPlaying) it.stop()
                it.release()
            }
        }
        mediaPlayer = null
    }
}
