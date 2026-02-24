package com.desafiolgico.information

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.media.MediaPlayer
import android.os.Bundle
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
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var onboardingAdapter: OnboardingAdapter
    private var introSound: MediaPlayer? = null

    private var weeklyStatusLine: String = "Carregando status do campeonato..."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyEdgeToEdge(lightSystemBarIcons = false)

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Edge-to-Edge safe
        binding.onboardingRoot.applySystemBarsPadding(applyTop = true, applyBottom = true)

        // 🔹 Fundo animado
        animateGradientBackground()

        // 🔹 Música ambiente suave
        introSound = MediaPlayer.create(this, R.raw.intro_soft_music)?.apply {
            isLooping = true
            setVolume(0.45f, 0.45f)
            start()
        }

        // ✅ Recupera nível selecionado (ANTES de usar)
        val level = intent.getStringExtra("LEVEL") ?: "Iniciante"

        // ✅ Carrega status do campeonato e só então monta as páginas
        fetchWeeklyStatusLight {
            onboardingAdapter = OnboardingAdapter(getOnboardingItemsPremium(level))
            binding.viewPager.adapter = onboardingAdapter

            setupTabsPremium()
            setupNavigationButtons()
        }
    }

    private fun setupTabsPremium() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Começo"
                1 -> "Pontuação"
                2 -> "Campeonato"
                3 -> "Regras"
                4 -> "Dicas"
                else -> "Vamos!"
            }
        }.attach()
    }

    /**
     * ✅ Firestore leve: lê só weekly_events/current
     * - Sem login obrigatório
     * - Sem travar o onboarding
     */
    private fun fetchWeeklyStatusLight(onDone: () -> Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection("weekly_events").document("current").get()
            .addOnSuccessListener { snap ->
                val weekId = snap.getString("weekId").orEmpty()
                val endAt = snap.getTimestamp("endAt")

                weeklyStatusLine =
                    if (weekId.isBlank() || endAt == null) {
                        "📌 Campeonato: indisponível no momento."
                    } else {
                        val sdf = SimpleDateFormat("dd/MM 'às' HH:mm", Locale("pt", "BR"))
                        "🏆 Campeonato ativo • Semana $weekId • Encerra em ${sdf.format(endAt.toDate())}"
                    }

                onDone()
            }
            .addOnFailureListener {
                weeklyStatusLine = "📌 Campeonato: verifique mais tarde (sem conexão)."
                onDone()
            }
    }

    private fun getOnboardingItemsPremium(level: String): List<OnboardingItem> {
        val levelKey = level.trim().replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        val levelDesc = when (levelKey) {
            "Iniciante" -> "🌱 Comece leve: foque em consistência e tempo."
            "Intermediário" -> "💡 Aqui começa a estratégia: pense e elimine opções."
            "Avançado" -> "🔥 Pressão real: precisão + velocidade."
            "Experiente" -> "🏆 Elite: controle de tempo e nervos."
            else -> "🧠 Escolha um nível e evolua a cada rodada."
        }

        return listOf(
            OnboardingItem(
                R.drawable.onboarding_image5,
                "Bem-vindo ao Desafio Lógico",
                """
                🎮 Um jogo de raciocínio com ritmo, ranking e evolução.
                $levelDesc

                ✅ Você vai:
                • Resolver perguntas rápidas
                • Subir seu streak
                • Bater recordes
                • Competir no Campeonato Semanal
                """.trimIndent()
            ),

            OnboardingItem(
                R.drawable.capajogo,
                "Pontuação & Streak",
                """
                ⚡ Quanto mais rápido e consistente, mais você ganha.

                • Acertou → pontos + streak
                • Errou → perde ritmo (e pode custar o jogo)
                • Sequência alta → bônus/efeitos especiais

                🎵 Feedback imersivo:
                • Som de acerto/erro
                • Vibração no erro
                """.trimIndent()
            ),

            OnboardingItem(
                R.drawable.onboarding_image3,
                "Campeonato Semanal 🏆",
                """
                $weeklyStatusLine

                Toda semana rola um campeonato com ranking.

                ✅ Como funciona:
                • Você tem tentativas limitadas
                • Cada tentativa tem $${15} perguntas
                • Vale acerto + tempo final

                🚫 Anti-fraude:
                • Tempo limite
                • Limite de erros
                • Controle de saídas do app (background)
                """.trimIndent()
            ),

            OnboardingItem(
                R.drawable.onboarding_image3,
                "Regras por Nível",
                """
                📊 Erros permitidos:
                • Iniciante → 5
                • Intermediário → 3
                • Avançado → 2
                • Experiente → 3

                ⏱️ Tempo por pergunta (exemplo):
                • Iniciante: 30s
                • Intermediário: 25s
                • Avançado: 15s

                (O app pode ajustar regras em eventos especiais.)
                """.trimIndent()
            ),

            OnboardingItem(
                R.drawable.capajogo,
                "Dicas pra subir no ranking",
                """
                🔥 3 dicas rápidas:

                1) Leia a pergunta inteira antes de clicar.
                2) Se travar, elimine 2 opções primeiro.
                3) Jogue no seu melhor horário (foco total).

                🎯 Meta: consistência > sorte.
                """.trimIndent()
            ),

            OnboardingItem(
                R.drawable.capajogo,
                "Pronto pra começar?",
                """
                🚀 Agora é com você.

                Toque em “Vamos lá!” e comece a evoluir
                a cada rodada. Boa sorte no ranking!
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
            .setDuration(190)
            .withEndAction {
                binding.viewPager.currentItem = currentItem + 1
                binding.viewPager.translationX = 100f
                binding.viewPager.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(280)
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
            putString("last_level_seen", intent.getStringExtra("LEVEL") ?: "Iniciante")
        }

        stopIntroSoundSafely()

        val fromSettings = intent.getBooleanExtra("FROM_SETTINGS", false)
        if (fromSettings) {
            finish()
        } else {
            startActivity(Intent(this, BoasVindasActivity::class.java))
            finish()
        }
    }

    private fun stopIntroSoundSafely() {
        runCatching {
            introSound?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        }
        introSound = null
    }

    override fun onPause() {
        super.onPause()
        runCatching { introSound?.pause() }
    }

    override fun onResume() {
        super.onResume()
        runCatching { introSound?.start() }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { introSound?.release() }
        introSound = null
    }
}
