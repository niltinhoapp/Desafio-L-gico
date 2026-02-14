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
import com.desafiolgico.utils.PremiumUi
import com.desafiolgico.utils.applySystemBarsPadding

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // ✅ Edge-to-edge (ajuste o parâmetro conforme o fundo da tela)
            applyEdgeToEdge(lightSystemBarIcons = false)

            binding = ActivityUserProfileBinding.inflate(layoutInflater)

            // ✅ Insets: empurra conteúdo pra fora de status/nav bar
            // Se você tiver um container (ex.: contentContainer) no XML, prefira aplicar nele.
            binding.root.applySystemBarsPadding(applyTop = true, applyBottom = true)

            setContentView(binding.root)

            GameDataManager.init(this)

            setupButtons()
            loadUserData()
        }


    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadUserData() {
        // ✅ Fonte única do perfil: UserManager (GameDataManager já chama ele internamente, mas aqui padroniza)
        val profile = UserManager.carregarDadosUsuario(this)

        val displayName =
            profile.name.takeIf { it.isNotBlank() } ?: getString(R.string.default_username)

        binding.welcomeUsername.text = displayName
        binding.welcomeUsername.visibility = View.VISIBLE
        binding.welcomeTextPrefix.text = getString(R.string.bem_vindo)

        // ✅ Regra fixa: FOTO > AVATAR desbloqueado > fallback
        val photoUrl = profile.photoUrl
        val avatarId = profile.avatarId.takeIf { it > 0 }
        val avatarUnlocked = avatarId != null && CoinManager.isAvatarUnlocked(this, avatarId)

        when {
            !photoUrl.isNullOrBlank() -> {
                Glide.with(this)
                    .load(photoUrl)
                    .circleCrop()
                    .into(binding.logoImageView)
            }
            avatarUnlocked -> {
                binding.logoImageView.setImageResource(avatarId!!)
            }
            else -> {
                binding.logoImageView.setImageResource(R.drawable.avatar1)
            }
        }

        binding.logoImageView.visibility = View.VISIBLE

        // ✅ aplica frame/tema/título se você estiver usando PremiumUi no app
        runCatching {
            PremiumUi.applyFrameToAvatar(binding.logoImageView, this)
            PremiumUi.applyThemeToRoot(findViewById(android.R.id.content), this)
            PremiumUi.applyTitleToUsername(binding.welcomeUsername, this, displayName)
        }
    }

    private fun setupButtons() {
        // Avatar -> seleção/compra
        binding.logoImageView.setOnClickListener {
            startActivity(Intent(this, AvatarSelectionActivity::class.java))
        }

        // Continuar -> jogo (nível padrão pode ser setado no TestActivity)
        binding.continueButton.setOnClickListener {
            startActivity(Intent(this, TestActivity::class.java))
            finish()
        }
    }
}
