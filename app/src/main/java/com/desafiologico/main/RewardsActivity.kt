package com.desafiologico.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button // Import Button if earnCoinsButton is treated as a generic Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.desafiologico.R // Certifique-se de que R está importado se usar IDs de recursos diretamente
import com.desafiologico.databinding.ActivityRewardsBinding
import com.desafiologico.utils.GameDataManager

class RewardsActivity : AppCompatActivity() {

    private var coins = 0
    private lateinit var coinsTextView: TextView
    private lateinit var binding: ActivityRewardsBinding
    private lateinit var earnCoinsButton: Button // Declarar para poder desabilitar

    // Constante para a quantidade de moedas ganhas
    companion object {
        private const val COINS_EARNED_PER_AD = 50
        private const val EARN_BUTTON_COOLDOWN_MS = 2000L // 2 segundos de cooldown
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        coinsTextView = binding.coinsTextView
        earnCoinsButton = binding.earnCoinsButton // Atribuir aqui
        val backButton = binding.backButton

        // Carrega moedas usando GameDataManager
        coins = GameDataManager.loadCoins(this)
        atualizarSaldo()

        earnCoinsButton.setOnClickListener {
            // Desabilita o botão temporariamente para evitar cliques múltiplos
            it.isEnabled = false
            showAdForRewards()
            // Reabilita o botão após um pequeno cooldown
            Handler(Looper.getMainLooper()).postDelayed({
                it.isEnabled = true
            }, EARN_BUTTON_COOLDOWN_MS)
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun showAdForRewards() {
        // Simulação de visualização de anúncio
        Toast.makeText(this, "Parabéns! Você ganhou $COINS_EARNED_PER_AD moedas!", Toast.LENGTH_SHORT).show()

        coins += COINS_EARNED_PER_AD
        GameDataManager.saveCoins(this, coins)
        atualizarSaldo()

        // Poderia adicionar uma animação simples no TextView de moedas aqui
        // Exemplo: Animar a cor do texto ou um leve aumento de tamanho
        animateCoinUpdate()
    }

    @SuppressLint("SetTextI18n")
    private fun atualizarSaldo() {
        coinsTextView.text = getString(R.string.coins_format_string, coins) // Usando string resource para formatação
        // Exemplo de string resource em strings.xml:
        // <string name="coins_format_string">Moedas: %d</string>
    }

    private fun animateCoinUpdate() {
        // Exemplo simples de animação (piscar o texto)
        coinsTextView.animate()
            .alpha(0.5f)
            .setDuration(150)
            .withEndAction {
                coinsTextView.animate()
                    .alpha(1.0f)
                    .setDuration(150)
                    .start()
            }.start()
    }
}
