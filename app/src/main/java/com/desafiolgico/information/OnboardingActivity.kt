package com.desafiolgico.information

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.viewpager2.widget.ViewPager2
import com.desafiolgico.R
import com.desafiolgico.adapters.OnboardingAdapter
import com.desafiolgico.adapters.OnboardingItem
import com.desafiolgico.databinding.ActivityOnboardingBinding
import com.desafiolgico.main.BoasVindasActivity
import com.desafiolgico.utils.applyEdgeToEdge
import com.desafiolgico.utils.applySystemBarsPadding
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var onboardingAdapter: OnboardingAdapter
    private var introSound: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyEdgeToEdge(lightSystemBarIcons = false)

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Edge-to-Edge safe
        binding.onboardingRoot.applySystemBarsPadding(applyTop = true, applyBottom = true)

        // 🔹 Fundo animado
        animateGradientBackground()

        // 🔹 Música ambiente suave (segura)
        introSound = MediaPlayer.create(this, R.raw.intro_soft_music)?.apply {
            isLooping = true
            setVolume(0.5f, 0.5f)
            start()
        }

        // 🔹 Recupera nível selecionado
        val level = intent.getStringExtra("LEVEL") ?: "Iniciante"

        // 🔹 Adapter
        onboardingAdapter = OnboardingAdapter(getOnboardingItems(level))
        binding.viewPager.adapter = onboardingAdapter

        // 🔹 Tabs
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Introdução"
                1 -> "Funcionalidades"
                2 -> "Regras"
                else -> "Motivação"
            }
        }.attach()

        setupNavigationButtons()
    }

    private fun getOnboardingItems(level: String): List<OnboardingItem> {
        val levelDesc = when (level) {
            "Iniciante" -> "🌱 Ideal para começar sua jornada lógica com tranquilidade."
            "Intermediário" -> "💡 Prepare-se para pensar com mais estratégia e raciocínio."
            "Avançado" -> "🔥 Desafie seus limites e mostre sua maestria."
            "Experiente" -> "🏆 Você já domina, mas sempre pode evoluir ainda mais."
            else -> "🧠 Escolha um nível e comece sua evolução intelectual!"
        }

        return listOf(
            OnboardingItem(
                R.drawable.onboarding_image5,
                "Bem-vindo ao Desafio Lógico!",
                """
                🧠 Teste seu raciocínio com perguntas divertidas e educativas!
                $levelDesc

                🎯 Como jogar:
                - Escolha seu nível de dificuldade.
                - Responda perguntas desafiadoras.
                - Cada acerto aumenta seu streak e sua pontuação!
                """.trimIndent()
            ),
            OnboardingItem(
                R.drawable.capajogo,
                "Funcionalidades Principais",
                """
                ✅ Responda Rápido: O tempo é limitado — pense e aja!
                🎵 Efeitos e Sons: Feedback imersivo para cada acerto ou erro.
                💎 Streaks: Acertos seguidos valem multiplicadores de pontos.
                🌟 Bônus Dourado: A cada 20 acertos consecutivos, uma celebração épica!
                """.trimIndent()
            ),
            OnboardingItem(
                R.drawable.onboarding_image3,
                "Níveis e Regras",
                """
                📊 Níveis de dificuldade:
                - Iniciante → 5 erros permitidos.
                - Intermediário → 3 erros.
                - Avançado → apenas 2 erros.
                - Experiente → apenas 3 erros.

                ⏱️ Tempo por pergunta:
                - Iniciante: 30s | Intermediário: 25s | Avançado: 15s
                """.trimIndent()
            ),
            OnboardingItem(
                R.drawable.capajogo,
                "Pronto para Começar?",
                """
                🌟 Agora é com você!
                Continue aprendendo e evoluindo a cada rodada.

                🚀 Toque em Vamos lá! e comece o Desafio Lógico agora mesmo!
                """.trimIndent()
            )
        )
    }

    private fun setupNavigationButtons() {
        binding.btnNext.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem + 1 < onboardingAdapter.itemCount) {
                animateToNextPage(currentItem)
            } else {
                savePreferencesAndFinish()
            }
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val progress =
                    ((position + 1).toFloat() / onboardingAdapter.itemCount * 100).toInt()
                binding.onboardingProgress.progress = progress

                binding.btnNext.text =
                    if (position == onboardingAdapter.itemCount - 1) "Vamos lá!" else "Próximo"
            }
        })
    }

    private fun animateToNextPage(currentItem: Int) {
        binding.viewPager.animate()
            .translationX(-100f)
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.viewPager.currentItem = currentItem + 1
                binding.viewPager.translationX = 100f
                binding.viewPager.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun animateGradientBackground() {
        val bg = binding.onboardingRoot.background
        if (bg is AnimationDrawable) {
            bg.setEnterFadeDuration(1500)
            bg.setExitFadeDuration(3000)
            bg.start()
        }
    }

    private fun savePreferencesAndFinish() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        prefs.edit {
            putBoolean("onboarding_completed", true)
            putBoolean("always_show_onboarding", binding.checkBoxAlwaysShow.isChecked)
            putString("last_level_seen", intent.getStringExtra("LEVEL") ?: "Iniciante")
        }

        // ✅ encerra o som com segurança
        runCatching {
            introSound?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        }
        introSound = null

        val fromSettings = intent.getBooleanExtra("FROM_SETTINGS", false)
        if (fromSettings) {
            finish()
        } else {
            startActivity(Intent(this, BoasVindasActivity::class.java))
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        introSound?.pause()
    }

    override fun onResume() {
        super.onResume()
        introSound?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { introSound?.release() }
        introSound = null
    }
}
