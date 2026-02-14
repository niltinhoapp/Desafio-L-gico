package com.desafiolgico.main

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.desafiolgico.R
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.applyEdgeToEdge
import com.desafiolgico.utils.applySystemBarsPadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SecretTransitionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SECRET_LEVEL = "SECRET_LEVEL"
        const val EXTRA_IS_SECRET_LEVEL = "IS_SECRET_LEVEL" // compat (não obrigatório)
        const val EXTRA_RETURN_TO_ACTIVE_GAME = "RETURN_TO_ACTIVE_GAME"
    }

    private var mediaPlayer: MediaPlayer? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Edge-to-edge (tela escura -> ícones claros)
        applyEdgeToEdge(lightSystemBarIcons = false)

        setContentView(R.layout.activity_secret_transition)

        // ✅ Insets: como aqui não tem binding, aplique no root do conteúdo
        findViewById<View>(android.R.id.content)
            .applySystemBarsPadding(applyTop = true, applyBottom = true)

        GameDataManager.init(this)

        val secretLevel =
            intent.getStringExtra(EXTRA_SECRET_LEVEL)
                ?: intent.getStringExtra("level") // fallback legado

        if (secretLevel.isNullOrBlank()) {
            Log.w("SecretTransition", "Secret level vazio. Finalizando.")
            finish()
            return
        }

        // ✅ Fonte única: marca modo secreto
        GameDataManager.isModoSecretoAtivo = true

        val returnToActive = intent.getBooleanExtra(EXTRA_RETURN_TO_ACTIVE_GAME, false)

        val animView = findViewById<LottieAnimationView>(R.id.lottieSecret)
        val tvTitle = findViewById<TextView>(R.id.tvSecretTitle)

        // Título
        val titleRes = when (secretLevel) {
            GameDataManager.SecretLevels.RELAMPAGO -> R.string.secret_level_relampago_title
            GameDataManager.SecretLevels.PERFEICAO -> R.string.secret_level_perfeicao_title
            GameDataManager.SecretLevels.ENIGMA -> R.string.secret_level_enigma_title
            else -> R.string.secret_level_generic_title
        }
        tvTitle.text = getString(titleRes)

        // Animação
        runCatching {
            animView.setAnimation(R.raw.ic_animationcerebro)
            animView.repeatCount = 0
            animView.visibility = View.VISIBLE
            animView.playAnimation()
        }

        // Som (opcional)
        runCatching {
            releasePlayer()
            mediaPlayer = MediaPlayer.create(this, R.raw.secret_whoosh)?.apply {
                setOnCompletionListener { releasePlayer() }
                start()
            }
        }

        // Vai pro TestActivity (modo secreto)
        lifecycleScope.launch {
            delay(3000L)
            if (isFinishing || isDestroyed) return@launch

            startActivity(
                Intent(this@SecretTransitionActivity, TestActivity::class.java).apply {
                    putExtra("level", secretLevel)
                    putExtra(EXTRA_IS_SECRET_LEVEL, true)
                    putExtra(EXTRA_RETURN_TO_ACTIVE_GAME, returnToActive)
                    // ❌ NÃO passar streak: no secreto não existe streak
                }
            )
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    private fun releasePlayer() {
        mediaPlayer?.let {
            runCatching {
                if (it.isPlaying) it.stop()
                it.release()
            }
        }
        mediaPlayer = null
    }

    override fun onStop() {
        super.onStop()
        // evita som “preso” se usuário minimizar
        releasePlayer()
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }
}
