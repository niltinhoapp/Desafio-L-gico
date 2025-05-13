package com.desafiolgico.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityUserProfileBinding
import com.desafiolgico.profile.AvatarSelectionActivity
import com.desafiolgico.utils.CoinManager
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.UserManager
import com.desafiolgico.utils.applyEdgeToEdge

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        GameDataManager.init(this)

        loadUserData()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        // Sempre recarrega os dados (nome + avatar) quando volta pra essa tela
        loadUserData()
    }

    private fun loadUserData() {
        val (usernameFromGDM, photoFromGDM, avatarFromGDM) = GameDataManager.loadUserData(this)

        val userFromManager = UserManager.carregarDadosUsuario(this)
        val avatarFromUserManager = userFromManager.avatarId.takeIf { it != 0 }

        val prefs = getSharedPreferences("DesafioLogicoPrefs", MODE_PRIVATE)
        val avatarFromPrefs = prefs.getInt("avatar", 0).takeIf { it != 0 }

        val displayName = when {
            !usernameFromGDM.isNullOrBlank() -> usernameFromGDM
            userFromManager.name.isNotBlank() -> userFromManager.name
            else -> getString(R.string.default_username)
        }

        binding.welcomeUsername.text = displayName
        binding.welcomeUsername.visibility = View.VISIBLE

        // ✅ Foto tem prioridade (usa GDM, e fallback no UserManager)
        val finalPhoto = photoFromGDM.takeUnless { it.isNullOrBlank() } ?: userFromManager.photoUrl

        // ✅ Escolhe o primeiro avatar que estiver desbloqueado
        val candidateAvatars = listOfNotNull(avatarFromGDM, avatarFromUserManager, avatarFromPrefs)
        val unlockedAvatar = candidateAvatars.firstOrNull { CoinManager.isAvatarUnlocked(this, it) }

        when {
            !finalPhoto.isNullOrBlank() -> {
                Glide.with(this)
                    .load(finalPhoto)
                    .circleCrop()
                    .into(binding.logoImageView)
            }

            unlockedAvatar != null -> {
                binding.logoImageView.setImageResource(unlockedAvatar)
            }

            else -> {
                // melhor usar o grátis como padrão
                binding.logoImageView.setImageResource(R.drawable.avatar1)
            }
        }

        binding.logoImageView.visibility = View.VISIBLE
        binding.welcomeTextPrefix.text = getString(R.string.bem_vindo)
    }
    private fun setupButtons() {
        // Clicar no avatar abre a tela de seleção/compra de avatar
        binding.logoImageView.setOnClickListener {
            startActivity(Intent(this, AvatarSelectionActivity::class.java))
        }

        // Botão "Continuar" vai pro jogo (outra tela que você já usa)
        binding.continueButton.setOnClickListener {
            val intent = Intent(this, TestActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
