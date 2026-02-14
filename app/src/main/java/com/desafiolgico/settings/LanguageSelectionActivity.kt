package com.desafiolgico.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.desafiolgico.R
import com.desafiolgico.main.BoasVindasActivity
import com.desafiolgico.main.MainActivity
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.applyEdgeToEdge
import androidx.core.content.edit
import com.desafiolgico.auth.LoginActivity
import com.desafiolgico.utils.applySystemBarsPadding

class LanguageSelectionActivity : AppCompatActivity() {

    private lateinit var rowPortuguese: LinearLayout
    private lateinit var rowEnglish: LinearLayout
    private lateinit var checkPortuguese: ImageView
    private lateinit var checkEnglish: ImageView
    private lateinit var languageSummary: TextView
    private lateinit var btnConfirm: Button

    private var selectedLanguageCode: String = LanguageHelper.LANGUAGE_PORTUGUESE
    private var fromSettings: Boolean = false

    companion object {
        const val EXTRA_FROM_SETTINGS = "from_settings"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // ✅ Edge-to-edge (ajuste conforme seu fundo)
            applyEdgeToEdge(lightSystemBarIcons = false)

            setContentView(R.layout.activity_language_selection)

            // ✅ Insets (top+bottom) para não colar em status/nav bar
            findViewById<android.view.View>(android.R.id.content)
                .applySystemBarsPadding(applyTop = true, applyBottom = true)

            fromSettings = intent.getBooleanExtra(EXTRA_FROM_SETTINGS, false)

            rowPortuguese = findViewById(R.id.rowPortuguese)
            rowEnglish = findViewById(R.id.rowEnglish)
            checkPortuguese = findViewById(R.id.checkPortuguese)
            checkEnglish = findViewById(R.id.checkEnglish)
            languageSummary = findViewById(R.id.languageSummary)
            btnConfirm = findViewById(R.id.btnConfirmLanguage)

            val currentLang = LanguageHelper.getLanguage(this)
            selectedLanguageCode = currentLang

            updateSelectionUI()

            rowPortuguese.setOnClickListener {
                selectedLanguageCode = LanguageHelper.LANGUAGE_PORTUGUESE
                updateSelectionUI()
            }

            rowEnglish.setOnClickListener {
                selectedLanguageCode = LanguageHelper.LANGUAGE_ENGLISH
                updateSelectionUI()
            }

            btnConfirm.setOnClickListener {
                applySelectedLanguage()
            }
        }


    private fun updateSelectionUI() {
        val isPortuguese = selectedLanguageCode.startsWith("pt", ignoreCase = true)

        checkPortuguese.visibility = if (isPortuguese) ImageView.VISIBLE else ImageView.INVISIBLE
        checkEnglish.visibility = if (!isPortuguese) ImageView.VISIBLE else ImageView.INVISIBLE

        languageSummary.text = if (isPortuguese) {
            getString(R.string.language_portuguese_brazil)
        } else {
            getString(R.string.language_english_us)
        }
    }

    @Suppress("DEPRECATION")
    private fun applySelectedLanguage() {
        // 1. Salva idioma
        LanguageHelper.setLanguage(this, selectedLanguageCode)

        // 2. Marca que já escolheu idioma
        getSharedPreferences("AppPrefs", MODE_PRIVATE)
            .edit {
                putBoolean("language_selected", true)
            }

        // 3. Aplica idioma
        LanguageHelper.applyLanguage(this)

        // 4. Decide fluxo
        if (fromSettings) {
            Toast.makeText(
                this,
                getString(R.string.language_changed_success),
                Toast.LENGTH_SHORT
            ).show()

            // Reinicia fluxo principal já com idioma aplicado
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
            }

            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        } else {
            // Primeira vez / fluxo normal
            val intent = Intent(this, LoginActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
            }
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}
