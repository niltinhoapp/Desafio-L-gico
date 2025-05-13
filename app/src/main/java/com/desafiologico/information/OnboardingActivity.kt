package com.desafiologico.information

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.desafiologico.R
import com.desafiologico.databinding.ActivityOnboardingBinding
import com.desafiologico.main.BoasVindasActivity
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var onboardingAdapter: OnboardingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Recuperar o nível do Intent
        val level = intent.getStringExtra("LEVEL") ?: "Iniciante"

        // Configurar os itens do onboarding com base no nível
        val onboardingItems = getOnboardingItems(level)
        onboardingAdapter = OnboardingAdapter(onboardingItems)
        binding.viewPager.adapter = onboardingAdapter

        // Configurar o TabLayout com o ViewPager
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Introdução"
                1 -> "Funcionalidades"
                2 -> "Níveis e Regras"
                else -> "Aba $position"
            }
            tab.contentDescription = tab.text // Para acessibilidade
        }.attach()

        // Configurar botões de navegação
        setupNavigationButtons()
    }

    /**
     * Retorna os itens do onboarding com base no nível selecionado.
     */
    private fun getOnboardingItems(level: String): List<OnboardingItem> {
        val descriptionLevelSpecific = when (level) {
            "Iniciante" -> "Este é o melhor lugar para começar sua jornada."
            "Intermediário" -> "Aqui está um novo desafio para aprimorar suas habilidades."
            "Avançado" -> "Prepare-se para testar seu limite com desafios intensos."
            else -> "Escolha um nível para uma experiência personalizada."
        }

        return listOf(
            OnboardingItem(
                R.drawable.onboarding_image1,
                "Bem-vindo",
                """
                🧠 Descubra as funcionalidades do nosso aplicativo!
                $descriptionLevelSpecific
                
                🎯 Como funciona?
                - Escolha o nível de dificuldade: Iniciante, Intermediário ou Avançado.
                - Responda perguntas cuidadosamente elaboradas, explorando diversos temas e assuntos.
                - Cada questão apresenta 4 alternativas, mas apenas uma é correta.
                ⏳ Lembre-se: Responda antes que o cronômetro acabe! Pense rápido e demonstre seu conhecimento.
                🎉 Jogue agora e descubra o quanto você sabe! Boa sorte! 🍀
                """.trimIndent()
            ),
            OnboardingItem(
                R.drawable.capajogo,
                "Funcionalidades Principais",
                """
                🔹 **Início do Jogo:**
                - Selecione o nível de dificuldade e comece a responder as perguntas.
                - A primeira pergunta aparece com quatro opções de resposta.

                ✅ Respondendo Perguntas:
                - Escolha sua resposta.
                - Resposta correta: botão verde e som de acerto.
                - Resposta incorreta: botão vermelho e som de erro.
                ⏰ Tempo Limite:
                - Caso o tempo acabe, a pergunta será considerada incorreta.
                🏆 Fim do Jogo:
                - Ao errar o número máximo permitido, o jogo reinicia.
                - Veja sua pontuação final e recomece!
                """.trimIndent()
            ),
            OnboardingItem(
                R.drawable.onboarding_image3,
                "Níveis e Regras",
                """
                📊 Níveis de Dificuldade:
                - Iniciante: Erre até 5 perguntas antes de perder.
                - Intermediário: Erre até 3 perguntas antes de perder.
                - Avançado: Erre até 2 perguntas antes de perder.

                ⏱️ Tempo de Resposta:
                - Iniciante e Intermediário: Cada pergunta tem até 30 segundos para ser respondida.
                - Avançado: O tempo é mais desafiador, com apenas 20 segundos por pergunta.
                💪 Pronto para começar?
                Jogue agora e desafie seus conhecimentos! 🎮
                """.trimIndent()
            )
        )
    }

    /**
     * Configura os botões de navegação do onboarding.
     */
    // Listener para animação no botão "Próximo"
    private fun setupNavigationButtons() {
        binding.btnNext.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem + 1 < onboardingAdapter.itemCount) {
                animateToNextPage(currentItem)
            } else {
                navigateToMainActivity()
            }
        }

        // Configurar listener do grupo de botões
        binding.buttonToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnSkip -> navigateToMainActivity()
                }
            }
        }

        // Registrar mudanças no ViewPager
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.btnNext.text = if (position == onboardingAdapter.itemCount - 1) {
                    "Concluir"
                } else {
                    "Próximo"
                }
            }
        })
    }

    // Animação para avançar páginas
    private fun animateToNextPage(currentItem: Int) {
        binding.viewPager.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.viewPager.currentItem = currentItem + 1
                binding.viewPager.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    // Navegar para a UserProfileActivity
    private fun navigateToMainActivity() {
        // Salvar o estado de que o onboarding foi concluído
        getSharedPreferences("AppPrefs", MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_completed", true)
            .apply()
        startActivity(Intent(this, BoasVindasActivity::class.java))
        finish()
    }

}
