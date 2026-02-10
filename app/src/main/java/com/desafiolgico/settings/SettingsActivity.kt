package com.desafiolgico.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.desafiolgico.R
import com.desafiolgico.auth.LoginActivity
import com.desafiolgico.information.OnboardingActivity
import com.desafiolgico.premium.PremiumShopActivity
import com.desafiolgico.profile.AvatarSelectionActivity
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.PremiumThemes
import com.desafiolgico.utils.applyEdgeToEdge

class SettingsActivity : AppCompatActivity() {

    private lateinit var rowLanguage: View
    private lateinit var rowAvatar: View
    private lateinit var rowPremium: View
    private lateinit var rowTutorial: View
    private lateinit var rowSwitchAccount: View
    private lateinit var rowDeleteAccount: View
    private lateinit var btnBackSettings: View

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()
        setContentView(R.layout.activity_settings)

        // ✅ init primeiro
        GameDataManager.init(this)

        rowLanguage = findViewById(R.id.rowLanguage)
        rowAvatar = findViewById(R.id.rowAvatar)
        rowPremium = findViewById(R.id.rowPremium)
        rowTutorial = findViewById(R.id.rowTutorial)
        rowSwitchAccount = findViewById(R.id.rowSwitchAccount)
        rowDeleteAccount = findViewById(R.id.rowDeleteAccount)
        btnBackSettings = findViewById(R.id.btnBackSettings)

        // ✅ aplica tema premium nessa tela também
        PremiumThemes.apply(
            activity = this,
            root = findViewById(android.R.id.content),
            cardViews = listOf(rowLanguage, rowAvatar, rowPremium, rowTutorial, rowSwitchAccount, rowDeleteAccount)
        )

        btnBackSettings.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // ✅ guest: não faz sentido “Excluir conta”
        val uid = (GameDataManager.currentUserId ?: "").lowercase()
        val isGuest = uid == "guest" || uid == "guest_mode" || uid.isBlank()
        rowDeleteAccount.visibility = if (isGuest) View.GONE else View.VISIBLE

        // ⭐ Loja Premium
        rowPremium.setOnClickListener {
            startActivity(Intent(this, PremiumShopActivity::class.java))
        }

        // Idioma
        rowLanguage.setOnClickListener {
            startActivity(
                Intent(this, LanguageSelectionActivity::class.java).apply {
                    putExtra(LanguageSelectionActivity.EXTRA_FROM_SETTINGS, true)
                }
            )
        }

        // Avatar
        rowAvatar.setOnClickListener {
            startActivity(Intent(this, AvatarSelectionActivity::class.java))
        }

        // Tutorial
        rowTutorial.setOnClickListener {
            startActivity(
                Intent(this, OnboardingActivity::class.java).apply {
                    putExtra("FROM_SETTINGS", true)
                }
            )
        }

        // Trocar conta
        rowSwitchAccount.setOnClickListener {
            // OBS: se você quiser só “deslogar” sem apagar tudo, troque resetAll por um método de logout/sessão.
            GameDataManager.resetAll(this)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Excluir conta
        rowDeleteAccount.setOnClickListener {
            startActivity(Intent(this, DeleteAccountActivity::class.java))
        }
    }
}
