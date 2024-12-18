package com.desafiolgico.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.desafiolgico.R

class RewardsActivity : AppCompatActivity() {

    private var coins = 0  // Moedas do jogador

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewards)

        val coinsTextView: TextView = findViewById(R.id.coins_textview)
        val earnCoinsButton: Button = findViewById(R.id.earn_coins_button)
        val backButton: Button = findViewById(R.id.back_button)

        // Exibe o saldo atual de moedas
        coins = loadCoins()
        coinsTextView.text = "Moedas: $coins"

        // Ação do botão para ganhar moedas assistindo ao anúncio
        earnCoinsButton.setOnClickListener {
            showAdForRewards()
        }

        // Botão para voltar à tela anterior (pode ser a tela de nível ou a tela principal)
        backButton.setOnClickListener {
            finish()  // Volta para a tela anterior
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showAdForRewards() {
        // Simulação de exibição de anúncio
        // Aqui você pode adicionar a lógica para exibir um anúncio real
        Toast.makeText(this, "Assistir ao anúncio para ganhar moedas!", Toast.LENGTH_SHORT).show()

        // Simula a adição de moedas após o "anúncio"
        coins += 50  // Adiciona 50 moedas ao saldo

        // Atualiza o saldo de moedas na interface
        findViewById<TextView>(R.id.coins_textview).text = "Moedas: $coins"

        // Salva as novas moedas
        saveCoins(coins)
    }

    private fun loadCoins(): Int {
        val sharedPreferences = getSharedPreferences("GamePrefs", MODE_PRIVATE)
        return sharedPreferences.getInt("coins", 0)
    }

    private fun saveCoins(coins: Int) {
        val sharedPreferences = getSharedPreferences("GamePrefs", MODE_PRIVATE)
        sharedPreferences.edit().putInt("coins", coins).apply()
    }
}
