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

    companion object {
        const val EXTRA_SECRET_LEVEL = "SECRET_LEVEL"
        const val EXTRA_IS_SECRET_LEVEL = "IS_SECRET_LEVEL"
        const val EXTRA_RETURN_TO_ACTIVE_GAME = "RETURN_TO_ACTIVE_GAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secret_transition)

        GameDataManager.init(this)

        val secretLevel = intent.getStringExtra(EXTRA_SECRET_LEVEL)
            ?: intent.getStringExtra("level") // fallback

        if (secretLevel.isNullOrBlank()) {
            finish()
            return
        }

        // ✅ marca modo secreto ativo (fonte única pro app)
        GameDataManager.isModoSecretoAtivo = true

        val returnToActive = intent.getBooleanExtra(EXTRA_RETURN_TO_ACTIVE_GAME, false)

        val animView = findViewById<LottieAnimationView>(R.id.lottieSecret)
        val tvTitle = findViewById<TextView>(R.id.tvSecretTitle)

        val titleRes = when (secretLevel) {
            GameDataManager.SecretLevels.RELAMPAGO -> R.string.secret_level_relampago_title
            GameDataManager.SecretLevels.PERFEICAO -> R.string.secret_level_perfeicao_title
            GameDataManager.SecretLevels.ENIGMA -> R.string.secret_level_enigma_title
            else -> R.string.secret_level_generic_title
        }
        tvTitle.text = getString(titleRes)

        // ✅ animação
        animView.setAnimation(R.raw.ic_animationcerebro)
        animView.playAnimation()

        // ✅ som (opcional)
        runCatching {
            mediaPlayer = MediaPlayer.create(this, R.raw.secret_whoosh)?.apply { start() }
        }

        // ✅ vai pro TestActivity no secreto
        lifecycleScope.launch {
            delay(3000L)
            startActivity(
                Intent(this@SecretTransitionActivity, TestActivity::class.java).apply {
                    putExtra("level", secretLevel)
                    putExtra(EXTRA_IS_SECRET_LEVEL, true)
                    putExtra(EXTRA_RETURN_TO_ACTIVE_GAME, returnToActive)

                    // ❌ NÃO passar currentStreak (no secreto não existe streak)
                    // putExtra("currentStreak", ...)
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
