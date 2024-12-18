package com.desafiolgico.main

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.desafiolgico.R
import com.google.android.material.button.MaterialButton

class UserProfileActivity : AppCompatActivity() {

    // Constants para chaves de SharedPreferences
    companion object {
        const val POINTS_KEY = "points"
        const val USERNAME_KEY = "username"
        const val USER_LEVEL_KEY = "user_level"
        const val TOTAL_SCORE_KEY = "totalScore"
    }

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var pointsTextView: TextView
    private lateinit var welcomeText: TextView
    private lateinit var levelTextView: TextView
    private lateinit var totalScoreTextView: TextView
    private lateinit var continueButton: MaterialButton
    private lateinit var editUsernameEditText: EditText
    private lateinit var updateUsernameButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        // Inicializa SharedPreferences
        sharedPreferences = getSharedPreferences("Game_Prefs", MODE_PRIVATE)

        // Inicializa as Views
        welcomeText = findViewById(R.id.welcomeText)
        pointsTextView = findViewById(R.id.pointsTextView)
        levelTextView = findViewById(R.id.levelText)
        totalScoreTextView = findViewById(R.id.totalScoreTextView)
        continueButton = findViewById(R.id.continueButton)
        editUsernameEditText = findViewById(R.id.editUsernameEditText)
        updateUsernameButton = findViewById(R.id.updateUsernameButton)

        // Atualiza os textos da interface
        updateWelcomeText()
        loadUserProgress()

        // Ação do botão de continuar
        continueButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Ação do botão de atualizar nome de usuário
        updateUsernameButton.setOnClickListener {
            val newUsername = editUsernameEditText.text.toString()
            if (newUsername.isNotEmpty()) {
                saveUsername(newUsername)
                updateWelcomeText()
            }
        }
    }

    private fun checkLevelUp(points: Int) {
        val currentLevel = sharedPreferences.getString(USER_LEVEL_KEY, "Iniciante") ?: "Iniciante"
        var newLevel = currentLevel

        if (points >= 4000 && currentLevel == "Iniciante") {
            newLevel = "Intermediário"
        } else if (points >= 3000 && currentLevel == "Intermediário") {
            newLevel = "Avançado"
        }

        if (newLevel != currentLevel) {
            saveUserProgress(points, newLevel, sharedPreferences.getInt(TOTAL_SCORE_KEY, 0))
            loadUserProgress()  // Atualiza a interface com o novo nível
        }
    }

    // Função para atualizar os pontos do usuário
    class TestActivity : AppCompatActivity() {
        companion object {
            private const val POINTS_KEY = "points_key"
            private const val USER_LEVEL_KEY = "user_level_key"
        }

        private lateinit var sharedPreferences: SharedPreferences
        private lateinit var pointsTextView: TextView
        private lateinit var levelTextView: TextView

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_test)

            // Inicializar SharedPreferences
            sharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE)

            // Inicializar views
            pointsTextView = findViewById(R.id.pointsTextView)
            levelTextView = findViewById(R.id.levelTextView)
        }

        // Função para atualizar os pontos do usuário
        fun updateUserPoints(newPoints: Int) {
            val currentPoints = sharedPreferences.getInt(POINTS_KEY, 0)
            val updatedPoints = currentPoints + newPoints

            // Atualizar os pontos no SharedPreferences
            sharedPreferences.edit().putInt(POINTS_KEY, updatedPoints).apply()

            // Verificar se o nível deve ser atualizado
            checkLevelUp(updatedPoints)

            // Atualizar a interface com os novos pontos e nível
            pointsTextView.text = getString(R.string.pontos_format, updatedPoints)
            levelTextView.text = getString(
                R.string.nivel_format,
                sharedPreferences.getString(USER_LEVEL_KEY, "Iniciante")
            )

            Log.d(
                "UpdateUserPoints",
                "Pontos atualizados: $updatedPoints, Nível: ${sharedPreferences.getString(USER_LEVEL_KEY, "Iniciante")}"
            )
        }

        // Função para verificar e atualizar o nível do usuário
        private fun checkLevelUp(updatedPoints: Int) {
            val newLevel = when {
                updatedPoints >= 500 -> "Expert"
                updatedPoints >= 300 -> "Avançado"
                updatedPoints >= 100 -> "Intermediário"
                else -> "Iniciante"
            }

            val currentLevel = sharedPreferences.getString(USER_LEVEL_KEY, "Iniciante")

            if (newLevel != currentLevel) {
                sharedPreferences.edit().putString(USER_LEVEL_KEY, newLevel).apply()
                Log.d("CheckLevelUp", "Nível atualizado para: $newLevel")
            }
        }
    }


    // Atualiza o texto de boas-vindas
    private fun updateWelcomeText() {
        val username = sharedPreferences.getString(USERNAME_KEY, "Jogador")
        welcomeText.text = getString(R.string.bem_vindo_usuario, username)
    }

    // Carrega e exibe o progresso do usuário
    private fun loadUserProgress() {
        val points = sharedPreferences.getInt(POINTS_KEY, 0)
        val level = sharedPreferences.getString(USER_LEVEL_KEY, "Iniciante") ?: "Iniciante"
        val totalScore = sharedPreferences.getInt(TOTAL_SCORE_KEY, 0)

        Log.d("LoadUserProgress", "Dados carregados: Pontos: $points, Nível: $level, Pontuação Total: $totalScore")

        pointsTextView.text = getString(R.string.pontos_format, points)
        levelTextView.text = getString(R.string.nivel_format, level)
        totalScoreTextView.text = getString(R.string.pontuacao_total, totalScore)
    }

    // Salva o nome de usuário
    private fun saveUsername(username: String) {
        sharedPreferences.edit().apply {
            putString(USERNAME_KEY, username)
            apply()
        }
        Log.d("SaveUsername", "Nome de usuário salvo: $username")
    }

    // Salva o progresso do usuário (pontos, nível e totalScore)
    private fun saveUserProgress(points: Int, level: String, totalScore: Int) {
        Log.d("SaveUserProgress", "Salvando dados: Pontos: $points, Nível: $level, Pontuação Total: $totalScore")

        sharedPreferences.edit().apply {
            putInt(POINTS_KEY, points)
            putString(USER_LEVEL_KEY, level)
            putInt(TOTAL_SCORE_KEY, totalScore)
            apply()
        }

        Log.d("SaveUserProgress", "Dados salvos com sucesso!")
    }

    override fun onPause() {
        super.onPause()
        val points = sharedPreferences.getInt(POINTS_KEY, 0)
        val level = sharedPreferences.getString(USER_LEVEL_KEY, "Iniciante") ?: "Iniciante"
        val totalScore = sharedPreferences.getInt(TOTAL_SCORE_KEY, 0)
        saveUserProgress(points, level, totalScore)
    }

    override fun onStop() {
        super.onStop()
        val points = sharedPreferences.getInt(POINTS_KEY, 0)
        val level = sharedPreferences.getString(USER_LEVEL_KEY, "Iniciante") ?: "Iniciante"
        val totalScore = sharedPreferences.getInt(TOTAL_SCORE_KEY, 0)
        saveUserProgress(points, level, totalScore)
    }

    override fun onResume() {
        super.onResume()
        loadUserProgress()
    }
}
