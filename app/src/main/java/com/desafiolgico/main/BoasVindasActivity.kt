package com.desafiolgico.main

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class BoasVindasActivity : AppCompatActivity() {

    private lateinit var userNameText: TextView
    private lateinit var userAvatarImage: ImageView
    private lateinit var btnNewGame: MaterialButton
    private lateinit var btnSettings: MaterialButton

    // ✅ NOVOS (opcionais: se não existir no XML, não crasha)
    private var avatarGlow: View? = null
    private var txtTotalScore: TextView? = null
    private var txtNivelAtual: TextView? = null
    private var txtStreak: TextView? = null

    private var glowAnimator: ValueAnimator? = null

    private val levelOrder = listOf(
        GameDataManager.Levels.INICIANTE,
        GameDataManager.Levels.INTERMEDIARIO,
        GameDataManager.Levels.AVANCADO,
        GameDataManager.Levels.EXPERIENTE
    )

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
                loadStatsAsync() // ✅ atualiza stats também
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

        // ✅ novos IDs (se não tiver no XML, fica null e seguimos)
        avatarGlow = findViewById(R.id.avatarGlow)
        txtTotalScore = findViewById(R.id.txtTotalScore)
        txtNivelAtual = findViewById(R.id.txtNivelAtual)
        txtStreak = findViewById(R.id.txtStreak)

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

    override fun onResume() {
        super.onResume()
        startGlowPulse()
        loadStatsAsync()
    }

    override fun onPause() {
        stopGlowPulse()
        super.onPause()
    }

    // =============================================================================================
    // GLOW PULSANDO (premium, bem leve)
    // =============================================================================================

    private fun startGlowPulse() {
        val glow = avatarGlow ?: return

        stopGlowPulse()

        glow.visibility = View.VISIBLE
        glow.alpha = 0.55f
        glow.scaleX = 1.0f
        glow.scaleY = 1.0f

        glowAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1400L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { anim ->
                val t = anim.animatedValue as Float
                val scale = 1.00f + (0.06f * t)   // 1.00 -> 1.06
                val alpha = 0.42f + (0.28f * t)   // 0.42 -> 0.70
                glow.scaleX = scale
                glow.scaleY = scale
                glow.alpha = alpha
            }
            start()
        }
    }

    private fun stopGlowPulse() {
        glowAnimator?.cancel()
        glowAnimator = null
    }

    // =============================================================================================
    // STATS (sem travar UI)
    // =============================================================================================

    private fun loadStatsAsync() {
        // se você ainda não colocou os TextViews no XML, não faz nada
        if (txtTotalScore == null && txtNivelAtual == null && txtStreak == null) return

        lifecycleScope.launch {
            val data = withContext(Dispatchers.Default) {
                val totalScore = GameDataManager.getOverallTotalScore(this@BoasVindasActivity).coerceAtLeast(0)
                val nivel = computeCurrentLevelName()
                val streak = getStreakSafe()
                Triple(totalScore, nivel, streak)
            }

            txtTotalScore?.text = fmtNumber(data.first)
            txtNivelAtual?.text = data.second
            txtStreak?.text = data.third.toString()
        }
    }

    private fun computeCurrentLevelName(): String {
        for (lvl in levelOrder) {
            val c = GameDataManager.getCorrectForLevel(this, lvl)
            if (c < 30) return lvl
        }
        return levelOrder.last()
    }

    /**
     * ✅ “Safe” (não quebra compile):
     * tenta pegar streak do seu ScoreManager OU SecurePrefs (se existir),
     * e se não achar, retorna 0.
     */
    private fun getStreakSafe(): Int {
        // 1) tenta ScoreManager(ctx).getStreak() / getCurrentStreak()...
        runCatching {
            val cls = Class.forName("com.desafiolgico.utils.ScoreManager")
            val ctor = cls.getConstructor(Context::class.java)
            val inst = ctor.newInstance(this)
            val m = cls.methods.firstOrNull {
                it.parameterCount == 0 && it.returnType == Int::class.javaPrimitiveType &&
                    it.name.lowercase() in listOf("getstreak", "getcurrentstreak", "currentstreak", "streak")
            }
            val v = (m?.invoke(inst) as? Int) ?: 0
            return v.coerceAtLeast(0)
        }

        // 2) tenta SecurePrefs.getInt(ctx,"streak",0) (se você usa SecurePrefs)
        runCatching {
            val sp = Class.forName("com.desafiolgico.utils.SecurePrefs")
            val m = sp.getMethod("getInt", Context::class.java, String::class.java, Int::class.javaPrimitiveType)
            val v = (m.invoke(null, this, "streak", 0) as? Int) ?: 0
            return v.coerceAtLeast(0)
        }

        return 0
    }

    private fun fmtNumber(v: Int): String {
        val nf = NumberFormat.getInstance(Locale("pt", "BR"))
        return nf.format(v)
    }

    // =============================================================================================
    // SEU CÓDIGO ORIGINAL
    // =============================================================================================

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
                getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().clear().apply()
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
