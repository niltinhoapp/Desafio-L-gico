package com.desafiolgico.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import com.bumptech.glide.Glide
import com.desafiolgico.R
import android.view.View
import com.desafiolgico.information.OnboardingActivity
import com.desafiolgico.profile.AvatarSelectionActivity
import com.desafiolgico.settings.SettingsActivity
import com.desafiolgico.utils.CoinManager
import com.desafiolgico.utils.CrashlyticsHelper
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.PremiumFrames
import com.desafiolgico.utils.UserManager
import com.desafiolgico.utils.applyEdgeToEdge
import com.google.android.material.button.MaterialButton

class BoasVindasActivity : AppCompatActivity() {

    private lateinit var userNameText: TextView
    private lateinit var userAvatarImage: ImageView
    private lateinit var btnNewGame: MaterialButton
    private lateinit var btnSettings: MaterialButton

    private val avatarLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedAvatar = result.data?.getIntExtra("SELECTED_AVATAR", -1) ?: -1
                if (selectedAvatar > 0 && CoinManager.isAvatarUnlocked(this, selectedAvatar)) {
                    val user = UserManager.carregarDadosUsuario(this)
                    UserManager.salvarDadosUsuario(
                        this,
                        user.name,
                        user.email,
                        user.photoUrl,
                        selectedAvatar
                    )

                    GameDataManager.saveUserData(
                        this,
                        user.name,
                        user.photoUrl,
                        selectedAvatar
                    )
                }
                loadWelcomeUI()
            }
        }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()
        setContentView(R.layout.activity_boas_vindas)


        // Onboarding (somente se NÃO completou)
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)
        if (!onboardingCompleted) {
            startActivity(
                Intent(this, OnboardingActivity::class.java)
                    .putExtra("FROM_SETTINGS", false)
            )
            finish()
            return
        }

        // ✅ agora sim, estamos ficando nessa tela
        Thread { CrashlyticsHelper.setGameState(applicationContext) }.start()

        // Views...
        userNameText = findViewById(R.id.userNameText)
        userAvatarImage = findViewById(R.id.userAvatarImage)
        btnNewGame = findViewById(R.id.btnNewGame)
        btnSettings = findViewById(R.id.btnSettings)

        loadWelcomeUI()

        val (_, photoUrl, avatarResId) = GameDataManager.loadUserData(this)
        val hasPhoto = !photoUrl.isNullOrBlank()
        val hasUnlockedAvatar = avatarResId != null && CoinManager.isAvatarUnlocked(this, avatarResId)
        if (!hasPhoto && !hasUnlockedAvatar) {
            avatarLauncher.launch(Intent(this, AvatarSelectionActivity::class.java))
        }

        btnNewGame.setOnClickListener {
            resetGameProgress()
            goToMain()
        }

        btnSettings.setOnClickListener { anchor ->
            showSettingsMenu(anchor)
        }
    }



    private fun showSettingsMenu(anchor: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_boas_vindas, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_continue -> { goToMain(); true }
                R.id.action_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
                R.id.action_tutorial -> {
                    startActivity(Intent(this, OnboardingActivity::class.java).putExtra("FROM_SETTINGS", true))
                    true
                }
                R.id.action_switch_account -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
                R.id.action_delete_account -> { confirmDeleteAccount(); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun confirmDeleteAccount() {
        AlertDialog.Builder(this)
            .setTitle("Excluir conta")
            .setMessage("Tem certeza? Isso remove seus dados do aparelho.")
            .setPositiveButton("Excluir") { _, _ ->
                // ✅ Seguro (local): limpa preferências do app + volta pro onboarding
                getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().clear().apply()

                // Se seus dados do jogo estiverem em SharedPreferences próprios,
                // me diga o NOME deles que eu adiciono aqui pra limpar tudo 100%.
                Toast.makeText(this, "Dados locais removidos.", Toast.LENGTH_SHORT).show()

                startActivity(
                    Intent(this, OnboardingActivity::class.java)
                        .putExtra("FROM_SETTINGS", false)
                )
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun resetGameProgress() {
        GameDataManager.clearLastQuestionIndex(this, GameDataManager.Levels.INICIANTE)
        GameDataManager.clearLastQuestionIndex(this, GameDataManager.Levels.INTERMEDIARIO)
        GameDataManager.clearLastQuestionIndex(this, GameDataManager.Levels.AVANCADO)

        GameDataManager.clearLastQuestionIndex(this, GameDataManager.SecretLevels.RELAMPAGO)
        GameDataManager.clearLastQuestionIndex(this, GameDataManager.SecretLevels.PERFEICAO)
        GameDataManager.clearLastQuestionIndex(this, GameDataManager.SecretLevels.ENIGMA)

        GameDataManager.clearUltimoNivelNormal(this)
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun loadWelcomeUI() {
        val (username, photoUrl, avatarResId) = GameDataManager.loadUserData(this)

        val displayName = if (username.isNullOrBlank()) {
            getString(R.string.default_username)
        } else username

        userNameText.text = getString(R.string.welcome_user_format, displayName)

        when {
            !photoUrl.isNullOrBlank() -> {
                Glide.with(this)
                    .load(Uri.parse(photoUrl))
                    .placeholder(R.drawable.avatar1)
                    .error(R.drawable.avatar1)
                    .circleCrop()
                    .into(userAvatarImage)
            }

            avatarResId != null && CoinManager.isAvatarUnlocked(this, avatarResId) -> {
                Glide.with(this).load(avatarResId).circleCrop().into(userAvatarImage)
            }

            else -> {
                Glide.with(this).load(R.drawable.avatar1).circleCrop().into(userAvatarImage)
            }
        }
    }
}
