package com.desafiolgico.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.desafiolgico.R
import com.desafiolgico.adapters.AvatarAdapter
import com.desafiolgico.databinding.ActivityAvatarSelectionBinding
import com.desafiolgico.utils.CoinManager
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.UserManager
import com.desafiolgico.utils.applyEdgeToEdge

class AvatarSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAvatarSelectionBinding
    private lateinit var avatarAdapter: AvatarAdapter
    private var selectedAvatarResId: Int? = null

    // 🔤 Garante que esta tela use o idioma salvo
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()
        binding = ActivityAvatarSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupConfirmButton()
        updateCoinBalance()
        loadCurrentPreview()          // ✅ Foto > Avatar
        setupUseEmailPhotoButton()    // ✅ limpa avatar e volta pra foto
    }

    /**
     * Botão: usar foto do Google (se existir).
     * Como a regra é Foto > Avatar, aqui a ideia é:
     * - limpar o avatar do perfil
     * - mostrar preview da foto
     */
    private fun setupUseEmailPhotoButton() {
        binding.btnUseEmailPhoto.setOnClickListener {
            val user = UserManager.carregarDadosUsuario(this)

            if (user.photoUrl.isNullOrBlank()) {
                Toast.makeText(this, "Sem foto do Google disponível 😕", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ limpa avatar do perfil (pra não ter “override” visual)
            UserManager.salvarDadosUsuario(
                context = this,
                nome = user.name,
                email = user.email,
                photoUrl = user.photoUrl,
                avatarId = null
            )

            // Mantém GameDataManager em sincronia (ele só salva avatar se existir)
            GameDataManager.saveUserData(this, user.name, user.photoUrl, null)
            GameDataManager.clearUserAvatar(this) // opcional, mas deixa tudo coerente

            // preview = foto
            Glide.with(this)
                .load(user.photoUrl)
                .circleCrop()
                .into(binding.previewImage)

            selectedAvatarResId = null

            Toast.makeText(this, "Foto do Google ativada ✅", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Exibe a lista de avatares disponíveis em grade.
     */
    private fun setupRecyclerView() {
        val recyclerView = binding.recyclerAvatars
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        val avatarList = listOf(
            R.drawable.avatar1,
            R.drawable.avatar2,
            R.drawable.avatar3,
            R.drawable.avatar4,
            R.drawable.avatar5
        )

        avatarAdapter = AvatarAdapter(avatarList) { selectedAvatar ->
            selectedAvatarResId = selectedAvatar

            Glide.with(this)
                .load(selectedAvatar)
                .circleCrop()
                .into(binding.previewImage)
        }

        recyclerView.adapter = avatarAdapter
    }

    /**
     * Botão "Confirmar Avatar":
     * - compra se precisar
     * - salva avatar no perfil (UserManager)
     * - sincroniza com GameDataManager
     *
     * ⚠️ OBS: Mesmo salvando avatar, a regra geral do app é:
     * Foto > Avatar. Então se o usuário tem foto, ela continuará sendo usada no app.
     */
    private fun setupConfirmButton() {
        binding.btnConfirm.setOnClickListener {
            val avatarId = selectedAvatarResId
            if (avatarId == null) {
                Toast.makeText(this, getString(R.string.select_avatar_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val cost = CoinManager.AVATAR_COST
            val coins = CoinManager.getCoins(this)
            val alreadyUnlocked = CoinManager.isAvatarUnlocked(this, avatarId)

            // Se ainda não está desbloqueado, tenta comprar
            if (!alreadyUnlocked) {
                if (coins < cost) {
                    Toast.makeText(
                        this,
                        getString(R.string.avatar_need_coins, cost, coins),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val bought = CoinManager.tryBuyAvatar(this, avatarId)
                if (!bought) {
                    Toast.makeText(
                        this,
                        getString(R.string.avatar_purchase_failed, cost, coins),
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                Toast.makeText(this, getString(R.string.avatar_unlocked_coins, cost), Toast.LENGTH_SHORT).show()
            }

            // Salva avatar no perfil
            val user = UserManager.carregarDadosUsuario(this)
            UserManager.salvarDadosUsuario(
                context = this,
                nome = user.name,
                email = user.email,
                photoUrl = user.photoUrl,
                avatarId = avatarId
            )

            // Sincronia com GameDataManager (ele salva avatarId se não for nulo)
            GameDataManager.saveUserData(this, user.name, user.photoUrl, avatarId)

            updateCoinBalance()
            animateCoinBalance()

            Toast.makeText(this, getString(R.string.avatar_atualizado), Toast.LENGTH_SHORT).show()

            setResult(RESULT_OK, Intent().apply {
                putExtra("SELECTED_AVATAR", avatarId)
            })
            finish()
        }
    }

    private fun updateCoinBalance() {
        val coins = CoinManager.getCoins(this)
        binding.coinBalanceText.text = getString(R.string.moedas_format, coins)
    }

    private fun animateCoinBalance() {
        binding.coinBalanceText.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(150)
            .withEndAction {
                binding.coinBalanceText.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start()
            }.start()
    }

    /**
     * Preview seguindo a regra do app:
     * ✅ Foto > Avatar > fallback avatar1
     */
    private fun loadCurrentPreview() {
        val user = UserManager.carregarDadosUsuario(this)

        val hasPhoto = !user.photoUrl.isNullOrBlank()
        val hasAvatar = user.avatarId > 0
        val avatarUnlocked = hasAvatar && CoinManager.isAvatarUnlocked(this, user.avatarId)

        when {
            hasPhoto -> {
                Glide.with(this).load(user.photoUrl).circleCrop().into(binding.previewImage)
                selectedAvatarResId = null
            }

            avatarUnlocked -> {
                Glide.with(this).load(user.avatarId).circleCrop().into(binding.previewImage)
                selectedAvatarResId = user.avatarId
            }

            else -> {
                Glide.with(this).load(R.drawable.avatar1).circleCrop().into(binding.previewImage)
                selectedAvatarResId = null
            }
        }
    }
}
