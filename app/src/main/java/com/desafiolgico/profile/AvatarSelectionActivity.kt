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
        loadCurrentAvatar()
        setupUseEmailPhotoButton()


    }

    private fun setupUseEmailPhotoButton() {
        binding.btnUseEmailPhoto.setOnClickListener {
            val user = UserManager.carregarDadosUsuario(this)

            if (user.photoUrl.isNullOrBlank()) {
                Toast.makeText(this, "Sem foto do Google disponível 😕", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ volta a priorizar foto do e-mail
            GameDataManager.setPreferAvatar(this, false)

            // opcional: limpa avatar salvo (deixa “limpo”)
            GameDataManager.clearUserAvatar(this)

            // atualiza preview
            Glide.with(this)
                .load(user.photoUrl)
                .circleCrop()
                .into(binding.previewImage)

            selectedAvatarResId = null

            Toast.makeText(this, "Foto do e-mail ativada ✅", Toast.LENGTH_SHORT).show()
        }
    }


    /**
     * Exibe a lista de avatares disponíveis em uma grade.
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
     * - salva o avatar escolhido
     */
    private fun setupConfirmButton() {



        binding.btnConfirm.setOnClickListener {
            val avatarId = selectedAvatarResId
            if (avatarId == null) {
                Toast.makeText(
                    this,
                    getString(R.string.select_avatar_first),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            GameDataManager.setPreferAvatar(this, true)


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

                Toast.makeText(
                    this,
                    getString(R.string.avatar_unlocked_coins, cost),
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Agora o avatar já está desbloqueado → salva como atual
            // Atualiza perfil do usuário com o novo avatar
            val user = UserManager.carregarDadosUsuario(this)
            UserManager.salvarDadosUsuario(
                context = this,
                nome = user.name,
                email = null,
                photoUrl = user.photoUrl,
                avatarId = avatarId
            )

// Salva avatar também no SharedPreferences usado pelo perfil


// Mantém em sincronia com o GameDataManager
            GameDataManager.saveUserData(this, user.name, user.photoUrl, avatarId)

            // Mantém em sincronia com o GameDataManager
            GameDataManager.saveUserData(this, user.name, user.photoUrl, avatarId)

// ✅ usuário escolheu avatar -> preferir avatar
            GameDataManager.setPreferAvatar(this, true)

// 🔹 Atualiza saldo visualmente e anima
            updateCoinBalance()

            animateCoinBalance()

            Toast.makeText(
                this,
                getString(R.string.avatar_atualizado),
                Toast.LENGTH_SHORT
            ).show()

            setResult(RESULT_OK, Intent().apply {
                putExtra("SELECTED_AVATAR", avatarId)
            })

            finish()
        }
    }

    /**
     * Atualiza a label de moedas (já usando string formatada).
     */
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
     * Carrega o avatar atual do usuário e mostra no preview.
     */
    private fun loadCurrentAvatar() {
        val currentUser = UserManager.carregarDadosUsuario(this)
        val preferAvatar = GameDataManager.isPreferAvatar(this)

        val hasPhoto = !currentUser.photoUrl.isNullOrBlank()
        val hasAvatar = (currentUser.avatarId != null && currentUser.avatarId != 0)
        val avatarUnlocked = hasAvatar && CoinManager.isAvatarUnlocked(this, currentUser.avatarId!!)

        when {
            // ✅ se preferiu avatar e tem um válido
            preferAvatar && avatarUnlocked -> {
                Glide.with(this).load(currentUser.avatarId).circleCrop().into(binding.previewImage)
                selectedAvatarResId = currentUser.avatarId
            }

            // ✅ senão, foto do Google se existir
            hasPhoto -> {
                Glide.with(this).load(currentUser.photoUrl).circleCrop().into(binding.previewImage)
                selectedAvatarResId = null
            }

            // ✅ fallback: avatar desbloqueado
            avatarUnlocked -> {
                Glide.with(this).load(currentUser.avatarId).circleCrop().into(binding.previewImage)
                selectedAvatarResId = currentUser.avatarId
            }

            else -> {
                Glide.with(this).load(R.drawable.avatar1).circleCrop().into(binding.previewImage)
                selectedAvatarResId = null
            }
        }
    }

}
