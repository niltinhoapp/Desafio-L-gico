package com.desafiolgico.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityNextPhaseBinding

class NextPhaseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNextPhaseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNextPhaseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val phase = intent.getIntExtra("PHASE", 1)
        val level = intent.getStringExtra("level") ?: "Iniciante"
        binding.phaseTextView.text = getString(R.string.fase_conclu_da)

        // Exibir curiosidade
        binding.curiosityTextView.text = getCuriosityForPhase(phase, level)

        binding.continueButton.setOnClickListener {
            it.alpha = 0.7f
            it.animate().alpha(1f).setDuration(300).start()
            setResult(RESULT_OK)
            finish() // Voltar para a tela anterior
        }
    }

    private fun getCuriosityForPhase(phase: Int, level: String): String {
        return when (level) {
            "Intermediário" -> getIntermediateCuriosity(phase)
            "Avançado" -> getAdvancedCuriosity(phase)
            else -> getBeginnerCuriosity(phase)
        }
    }

    private fun getBeginnerCuriosity(phase: Int): String {
        return when (phase) {
            1 -> "🌊 Sabia que o coração de um camarão está em sua cabeça?"
            2 -> "🐘 O elefante é o único animal com quatro joelhos."
            3 -> "🦋 As borboletas sentem o gosto com os pés."
            4 -> "❤️ O espaço muda a forma do coração humano!"
            5 -> "✨ Raios cósmicos causam visões de luzes nos astronautas."
            6 -> "🌌 A alma pode viajar pelo cosmos após a morte, segundo filosofias antigas."
            else ->  "🌟 Continue jogando para desbloquear mais fatos incríveis!"

        }
    }

    private fun getIntermediateCuriosity(phase: Int): String {
        return when (phase) {
            1 -> "🧠 O cérebro humano pode gerar eletricidade suficiente para acender uma lâmpada pequena!"
            2 -> "💓 O coração bate cerca de 100 mil vezes por dia, bombeando sangue para todo o corpo."
            3 -> "👀 Os olhos conseguem distinguir cerca de 10 milhões de cores diferentes."
            4 -> "🦵 O fêmur é mais forte que concreto, sendo o osso mais resistente do corpo humano."
            5 -> "👅 Cada pessoa tem uma impressão de língua única, assim como as digitais!"
            6 -> "🩸 O corpo humano contém cerca de 96 mil quilômetros de vasos sanguíneos – o suficiente para dar a volta na Terra duas vezes!"
            else -> "Curiosidade: Continue jogando para descobrir mais sobre o incrível corpo humano!"
        }
    }

    private fun getAdvancedCuriosity(phase: Int): String {
        return when (phase) {
            1 -> "⚛️ Na física quântica, as partículas podem existir em dois estados ao mesmo tempo, um fenômeno chamado de superposição."
            2 -> "💻 A primeira linguagem de programação, chamada Assembly, foi criada na década de 1940 e ainda influencia muitos sistemas modernos."
            3 -> "🔮 O princípio da incerteza de Heisenberg afirma que não podemos conhecer simultaneamente a posição e a velocidade exatas de uma partícula."
            4 -> "⏳ O algoritmo de Shor, desenvolvido na computação quântica, pode quebrar códigos de criptografia usados atualmente em segundos!"
            5 -> "⚙️ Em programação, o conceito de 'Recursão' envolve uma função chamando a si mesma para resolver problemas complexos de forma mais eficiente."
            6 -> "💡 A física quântica é tão misteriosa que até os maiores cientistas, como Einstein, não conseguiam entender totalmente suas implicações."
            else -> "Curiosidade: Continue jogando para explorar mais sobre física quântica e programação!"
        }
    }
}
