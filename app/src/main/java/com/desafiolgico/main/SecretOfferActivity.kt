package com.desafiolgico.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivitySecretOfferBinding
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.SecretRightManager
import com.desafiolgico.utils.applyEdgeToEdge

class SecretOfferActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecretOfferBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()

        binding = ActivitySecretOfferBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_SECRET_UNLOCK
        val secretLevelId = intent.getStringExtra(EXTRA_SECRET_LEVEL_ID)

        if (mode == MODE_REVIVE) {
            renderRevive()
        } else {
            renderSecretUnlock(secretLevelId)
        }

        binding.btnClose.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun renderSecretUnlock(secretLevelId: String?) {
        binding.txtTitle.text = getString(R.string.secret_offer_title)
        binding.txtDesc.text = getString(R.string.secret_offer_desc)

        binding.btnPrimary.text = getString(R.string.secret_offer_play_now)
        binding.btnSecondary.text = getString(R.string.secret_offer_clear_errors)
        binding.btnTertiary.text = getString(R.string.secret_offer_save_for_later)

        binding.btnPrimary.setOnClickListener {
            // Não guarda direito: jogar agora
            setResult(RESULT_OK, Intent().putExtra(EXTRA_ACTION, ACTION_PLAY_NOW))
            // passa o level pra quem chamou
            intent.getStringExtra(EXTRA_SECRET_LEVEL_ID)?.let { lvl ->
                setResult(
                    RESULT_OK,
                    Intent()
                        .putExtra(EXTRA_ACTION, ACTION_PLAY_NOW)
                        .putExtra(EXTRA_SECRET_LEVEL_ID, lvl)
                )
            }
            finish()
        }

        binding.btnSecondary.setOnClickListener {
            // Troca por zerar erros (consome direito “virtual” na hora)
            // (se você quer consumir um direito já ganho antes, é aqui também)
            SecretRightManager.consumeToClearErrors(applicationContext)
            setResult(RESULT_OK, Intent().putExtra(EXTRA_ACTION, ACTION_CLEAR_ERRORS))
            finish()
        }

        binding.btnTertiary.setOnClickListener {
            // Guarda o direito (persistente)
            SecretRightManager.grantRight(applicationContext, secretLevelId)
            setResult(RESULT_OK, Intent().putExtra(EXTRA_ACTION, ACTION_SAVE_FOR_LATER))
            finish()
        }
    }

    private fun renderRevive() {
        binding.txtTitle.text = getString(R.string.revive_offer_title)
        binding.txtDesc.text = getString(R.string.revive_offer_desc)

        binding.btnPrimary.text = getString(R.string.revive_offer_revive_now)
        binding.btnSecondary.text = getString(R.string.revive_offer_no_thanks)
        binding.btnTertiary.text = getString(R.string.secret_offer_save_for_later)

        binding.btnPrimary.setOnClickListener {
            setResult(RESULT_OK, Intent().putExtra(EXTRA_ACTION, ACTION_REVIVE_NOW))
            finish()
        }

        binding.btnSecondary.setOnClickListener {
            setResult(RESULT_OK, Intent().putExtra(EXTRA_ACTION, ACTION_DECLINE))
            finish()
        }

        // No revive, “guardar” mantém como está (não consome)
        binding.btnTertiary.setOnClickListener {
            setResult(RESULT_OK, Intent().putExtra(EXTRA_ACTION, ACTION_SAVE_FOR_LATER))
            finish()
        }
    }

    companion object {
        const val EXTRA_MODE = "EXTRA_MODE"
        const val EXTRA_ACTION = "EXTRA_ACTION"
        const val EXTRA_SECRET_LEVEL_ID = "EXTRA_SECRET_LEVEL_ID"

        const val MODE_SECRET_UNLOCK = "MODE_SECRET_UNLOCK"
        const val MODE_REVIVE = "MODE_REVIVE"

        const val ACTION_PLAY_NOW = "ACTION_PLAY_NOW"
        const val ACTION_CLEAR_ERRORS = "ACTION_CLEAR_ERRORS"
        const val ACTION_SAVE_FOR_LATER = "ACTION_SAVE_FOR_LATER"
        const val ACTION_REVIVE_NOW = "ACTION_REVIVE_NOW"
        const val ACTION_DECLINE = "ACTION_DECLINE"

        fun intentSecret(ctx: Context, secretLevelId: String?): Intent =
            Intent(ctx, SecretOfferActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_SECRET_UNLOCK)
                putExtra(EXTRA_SECRET_LEVEL_ID, secretLevelId)
            }

        fun intentRevive(ctx: Context): Intent =
            Intent(ctx, SecretOfferActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_REVIVE)
            }
    }
}
