package com.desafiolgico.main

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.desafiolgico.R
import com.desafiolgico.adapters.AvatarAdapter
import com.desafiolgico.main.MainActivity.Companion.GAME_PREFS
import com.google.android.material.button.MaterialButton

class UserProfileActivity : AppCompatActivity() {

    companion object {
        const val USERNAME_KEY = "username"
        const val AVATAR_KEY = "avatar"
     //   const val USER_LEVEL_KEY = "user_level"
       // const val TOTAL_SCORE_KEY = "totalScore"
    }

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var welcomeText: TextView
    private lateinit var welcomeUsername: TextView

    private lateinit var continueButton: MaterialButton
    private lateinit var avatarImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        // Inicializa SharedPreferences
        sharedPreferences = getSharedPreferences(GAME_PREFS, MODE_PRIVATE)

        // Inicializa Views
        welcomeText = findViewById(R.id.welcomeTextPrefix)
        welcomeUsername = findViewById(R.id.welcomeUsername)
        continueButton = findViewById(R.id.continueButton)
        avatarImageView = findViewById(R.id.logoImageView)

        // Configura RecyclerView para avatares
        val avatarRecyclerView = findViewById<RecyclerView>(R.id.avatarRecyclerView)
        val avatars = listOf(
            R.drawable.avatar1,
            R.drawable.avatar2,
            R.drawable.avatar3,
            R.drawable.avatar4,
            R.drawable.avatar5
        )
        avatarRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        avatarRecyclerView.adapter = AvatarAdapter(avatars) { selectedAvatar ->
            saveAvatar(selectedAvatar, "Avatar Selecionado")
        }

        // Recupera nome ou avatar
        val usernameOrAvatar = sharedPreferences.getString(USERNAME_KEY, "Jogador") ?: "Jogador"
        val avatarId = sharedPreferences.getInt(AVATAR_KEY, R.drawable.avatar1)

        // Exibe nome ou avatar na interface
        if (usernameOrAvatar.startsWith("Avatar")) {
            avatarImageView.setImageResource(avatarId)
            avatarImageView.visibility = View.VISIBLE
            welcomeUsername.visibility = View.GONE
        } else {
            welcomeUsername.text = usernameOrAvatar
            avatarImageView.visibility = View.VISIBLE // Caso queira manter o logo padrão
        }

        // Atualiza Texto de Boas-Vindas e Dados
        updateWelcomeText()


        // Botão de continuar
        continueButton.setOnClickListener {
            // Recupera nome e avatar
            val usernameOrAvatar = sharedPreferences.getString(USERNAME_KEY, "Jogador") ?: "Jogador"
            val avatarId = sharedPreferences.getInt(AVATAR_KEY, R.drawable.ic_email_foreground)

            // Envia dados para TestActivity
            val intent = Intent(this, TestActivity::class.java)
            intent.putExtra("username", usernameOrAvatar)
            intent.putExtra("avatar", avatarId)

            // Inicia TestActivity
            startActivity(intent)
            finish() // Finaliza a UserProfileActivity
        }
    }

    private fun updateWelcomeText() {
        val usernameOrAvatar = sharedPreferences.getString(USERNAME_KEY, "Jogador") ?: "Jogador"
        welcomeText.text = "Bem-vindo,"
        welcomeUsername.text = usernameOrAvatar
        Log.d("WelcomeText", "Nome ou Avatar exibido: $usernameOrAvatar")
    }

    private fun saveAvatar(avatarResId: Int, avatarName: String) {
        sharedPreferences.edit().apply {
            putInt(AVATAR_KEY, avatarResId) // Salva o ID do avatar
            apply()
        }
        avatarImageView.setImageResource(avatarResId) // Atualiza o avatar no ImageView
        Toast.makeText(this, "Avatar atualizado: $avatarName", Toast.LENGTH_SHORT).show()
    }


}