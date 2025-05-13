package com.desafiolgico.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.desafiolgico.R
import com.desafiolgico.information.OnboardingActivity
import com.desafiolgico.profile.AvatarSelectionActivity
import com.desafiolgico.settings.SettingsActivity
import com.desafiolgico.utils.CoinManager
import com.desafiolgico.utils.CrashlyticsHelper
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.UserManager
import com.desafiolgico.utils.applyEdgeToEdge
import com.google.android.material.button.MaterialButton

class BoasVindasActivity : AppCompatActivity() {

    private lateinit var userNameText: TextView
    private lateinit var userAvatarImage: ImageView
    private lateinit var btnContinue: MaterialButton
    private lateinit var btnNewGame: MaterialButton
    private lateinit var btnSettings: MaterialButton


    private val avatarLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedAvatar = result.data?.getIntExtra("SELECTED_AVATAR", -1) ?: -1
                if (selectedAvatar > 0) {
                    // ✅ Só aceita/salva se estiver desbloqueado (avatar1 é grátis)
                    if (CoinManager.isAvatarUnlocked(this, selectedAvatar)) {
                        val user = UserManager.carregarDadosUsuario(this)

                        UserManager.salvarDadosUsuario(
                            this,
                            user.name,
                            user.email,          // mantém email
                            user.photoUrl,       // mantém foto
                            selectedAvatar
                        )

                        GameDataManager.saveUserData(
                            this,
                            user.name,
                            user.photoUrl,
                            selectedAvatar
                        )
                    }
                }
                // Recarrega UI respeitando as prioridades
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

        CrashlyticsHelper.setupCrashlytics(this)

        // 🔹 Verifica onboarding (somente se NÃO completou)
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


        // 🔹 Views
        userNameText = findViewById(R.id.userNameText)
        userAvatarImage = findViewById(R.id.userAvatarImage)
        btnContinue = findViewById(R.id.btnContinueGame)
        btnNewGame = findViewById(R.id.btnNewGame)
        btnSettings = findViewById(R.id.btnSettings)

        // 🔹 UI inicial
        loadWelcomeUI()

        // 🔹 Se não tiver foto e não tiver avatar válido/desbloqueado, abre seleção (opcional)
        val (_, photoUrl, avatarResId) = GameDataManager.loadUserData(this)
        val hasPhoto = !photoUrl.isNullOrBlank()
        val hasUnlockedAvatar = avatarResId != null && CoinManager.isAvatarUnlocked(this, avatarResId)

        if (!hasPhoto && !hasUnlockedAvatar) {
            avatarLauncher.launch(Intent(this, AvatarSelectionActivity::class.java))
        }



        btnContinue.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }


        btnNewGame.setOnClickListener {
            GameDataManager.clearLastQuestionIndex(this, GameDataManager.Levels.INICIANTE)
            GameDataManager.clearLastQuestionIndex(this, GameDataManager.Levels.INTERMEDIARIO)
            GameDataManager.clearLastQuestionIndex(this, GameDataManager.Levels.AVANCADO)

            GameDataManager.clearLastQuestionIndex(this, GameDataManager.SecretLevels.RELAMPAGO)
            GameDataManager.clearLastQuestionIndex(this, GameDataManager.SecretLevels.PERFEICAO)
            GameDataManager.clearLastQuestionIndex(this, GameDataManager.SecretLevels.ENIGMA)

            GameDataManager.clearUltimoNivelNormal(this)

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, com.desafiolgico.main.MainActivity::class.java))
        finish()
    }


    private fun loadWelcomeUI() {
            val (username, photoUrl, avatarResId) = GameDataManager.loadUserData(this)

        val displayName = if (username.isNullOrBlank()) {
            getString(R.string.default_username)
        } else username

        userNameText.text = getString(R.string.welcome_user_format, displayName)

        // ✅ Regras decididas:
        // 1) Foto (photoUrl) tem prioridade
        // 2) Senão, avatar só se estiver desbloqueado (avatar1 é grátis)
        // 3) Senão, mostra avatar1 como padrão
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
                Glide.with(this)
                    .load(avatarResId)
                    .circleCrop()
                    .into(userAvatarImage)
            }

            else -> {
                Glide.with(this)
                    .load(R.drawable.avatar1)
                    .circleCrop()
                    .into(userAvatarImage)
            }
        }
    }
}
