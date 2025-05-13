package com.desafiolgico.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.desafiolgico.R
import com.desafiolgico.auth.LoginActivity
import com.desafiolgico.information.OnboardingActivity
import com.desafiolgico.profile.AvatarSelectionActivity
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.applyEdgeToEdge

class SettingsActivity : AppCompatActivity() {

    private lateinit var rowLanguage: View
    private lateinit var rowAvatar: View
    private lateinit var rowTutorial: View
    private lateinit var rowSwitchAccount: View
    private lateinit var rowDeleteAccount: View

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()
        setContentView(R.layout.activity_settings)

        rowLanguage = findViewById(R.id.rowLanguage)
        rowAvatar = findViewById(R.id.rowAvatar)
        rowTutorial = findViewById(R.id.rowTutorial)
        rowSwitchAccount = findViewById(R.id.rowSwitchAccount)
        rowDeleteAccount = findViewById(R.id.rowDeleteAccount)

        // Trocar idioma
        rowLanguage.setOnClickListener {
            val intent = Intent(this, LanguageSelectionActivity::class.java).apply {
                putExtra(LanguageSelectionActivity.EXTRA_FROM_SETTINGS, true)
            }
            startActivity(intent)
        }

        // Trocar avatar
        rowAvatar.setOnClickListener {
            startActivity(Intent(this, AvatarSelectionActivity::class.java))
        }

        // Ver tutorial novamente
        rowTutorial.setOnClickListener {
            startActivity(
                Intent(this, OnboardingActivity::class.java).apply {
                    putExtra("FROM_SETTINGS", true)

                }
            )
        }


        // Trocar conta
        rowSwitchAccount.setOnClickListener {
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
