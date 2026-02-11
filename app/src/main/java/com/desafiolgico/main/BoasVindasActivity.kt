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
import com.desafiolgico.premium.PremiumShopActivity
import com.desafiolgico.profile.AvatarSelectionActivity
import com.desafiolgico.settings.SettingsActivity
import com.desafiolgico.utils.CoinManager
import com.desafiolgico.utils.CrashlyticsHelper
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.UserManager
import com.desafiolgico.utils.applyEdgeToEdge
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class BoasVindasActivity : AppCompatActivity() {

    private lateinit var userNameText: TextView
    private lateinit var userAvatarImage: ImageView

    private lateinit var btnContinue: MaterialButton
    private lateinit var btnNewGame: MaterialButton
    private lateinit var btnMapa: MaterialButton
    private lateinit var btnSettings: MaterialButton

    // ✅ Resumo / glow (se não existir no XML, não crasha)
    private var avatarGlow: View? = null
    private var statsCard: MaterialCardView? = null

    private var txtTotalScore: TextView? = null
    private var txtNivelAtual: TextView? = null
    private var txtStreak: TextView? = null

    private var lblTotalScore: TextView? = null
    private var lblNivelAtual: TextView? = null
    private var lblStreak: TextView? = null

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
                    GameDataManager.saveUserData(this, user.name, user.photoUrl, selectedAvatar)
                }
                loadWelcomeUI()
                loadStatsAsync()
                updateButtonsState()
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

        // ✅ Estamos ficando nessa tela
        Thread { CrashlyticsHelper.setGameState(applicationContext) }.start()

        bindViews()
        bindClicks()

        loadWelcomeUI()

        // Se não tem foto e não tem avatar desbloqueado, força escolher avatar
        val (_, photoUrl, avatarResId) = GameDataManager.loadUserData(this)
        val hasPhoto = !photoUrl.isNullOrBlank()
        val hasUnlockedAvatar = avatarResId != null && CoinManager.isAvatarUnlocked(this, avatarResId)
        if (!hasPhoto && !hasUnlockedAvatar) {
            avatarLauncher.launch(Intent(this, AvatarSelectionActivity::class.java))
        }

        loadStatsAsync()
        updateButtonsState()
    }

    override fun onResume() {
        super.onResume()
        startGlowPulse()
        loadStatsAsync()
        updateButtonsState()
    }

    override fun onPause() {
        stopGlowPulse()
        super.onPause()
    }

    private fun bindViews() {
        userNameText = findViewById(R.id.userNameText)
        userAvatarImage = findViewById(R.id.userAvatarImage)

        btnContinue = findViewById(R.id.btnContinue)
        btnNewGame = findViewById(R.id.btnNewGame)
        btnMapa = findViewById(R.id.btnMapa)
        btnSettings = findViewById(R.id.btnSettings)

        avatarGlow = findViewById(R.id.avatarGlow)
        statsCard = findViewById(R.id.statsCard)

        txtTotalScore = findViewById(R.id.txtTotalScore)
        txtNivelAtual = findViewById(R.id.txtNivelAtual)
        txtStreak = findViewById(R.id.txtStreak)

        lblTotalScore = findViewById(R.id.lblTotalScore)
        lblNivelAtual = findViewById(R.id.lblNivelAtual)
        lblStreak = findViewById(R.id.lblStreak)
    }

    private fun bindClicks() {
        userAvatarImage.setOnClickListener {
            avatarLauncher.launch(Intent(this, AvatarSelectionActivity::class.java))
        }

        btnContinue.setOnClickListener {
            openContinue()
        }

        btnMapa.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        btnNewGame.setOnClickListener {
            confirmNewGame()
        }

        btnSettings.setOnClickListener { anchor ->
            showSettingsMenu(anchor)
        }

        statsCard?.setOnClickListener {
            if (!isPremiumUser()) {
                startActivity(Intent(this, PremiumShopActivity::class.java))
            }
        }
    }

    // =============================================================================================
    // CONTINUAR / NOVO JOGO
    // =============================================================================================

    private fun updateButtonsState() {
        val totalScore = runCatching { GameDataManager.getOverallTotalScore(this) }.getOrDefault(0)

        // Se quiser, pode considerar "progresso" por acertos também:
        val hasAnyProgress = totalScore > 0 || runCatching { hasAnyCorrects() }.getOrDefault(false)

        // Continuar sempre funciona: se não tem progresso, vira “Começar”
        btnContinue.text = if (hasAnyProgress) "Continuar" else "Começar"
        btnContinue.isEnabled = true
        btnContinue.alpha = 1f

        // Novo jogo: se não tem progresso, some (opcional, fica mais coerente)
        btnNewGame.visibility = if (hasAnyProgress) View.VISIBLE else View.GONE
    }

    private fun hasAnyCorrects(): Boolean {
        for (lvl in levelOrder) {
            if (GameDataManager.getCorrectForLevel(this, lvl) > 0) return true
        }
        return false
    }

    private fun openContinue() {
        val levelToStart = computeCurrentLevelName().ifBlank { GameDataManager.Levels.INICIANTE }

        // ✅ Continue começa no nível “atual” (o TestActivity já cuida de restore/backup se existir)
        startActivity(
            Intent(this, TestActivity::class.java)
                .putExtra("level", levelToStart)
        )
        finish()
    }

    private fun confirmNewGame() {
        AlertDialog.Builder(this)
            .setTitle("Novo jogo")
            .setMessage("Isso reinicia seu progresso de fase (índices/último nível). Continuar?")
            .setPositiveButton("Sim") { _, _ ->
                resetGameProgress()
                startActivity(
                    Intent(this, TestActivity::class.java)
                        .putExtra("level", GameDataManager.Levels.INICIANTE)
                        .putExtra("NEW_GAME", true)
                )
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // =============================================================================================
    // GLOW PULSANDO (leve)
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
    // STATS (sem travar UI) + PRO coerente
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

            val premium = isPremiumUser()

            if (!premium) {
                // “PRO” travado (bonito e honesto)
                txtTotalScore?.text = "—"
                txtNivelAtual?.text = "—"
                txtStreak?.text = "—"

                lblTotalScore?.text = "Pontuação (PRO)"
                lblNivelAtual?.text = "Nível (PRO)"
                lblStreak?.text = "Streak (PRO)"

                statsCard?.alpha = 0.65f
            } else {
                txtTotalScore?.text = fmtNumber(data.first)
                txtNivelAtual?.text = data.second
                txtStreak?.text = data.third.toString()

                lblTotalScore?.text = "Pontuação"
                lblNivelAtual?.text = "Nível"
                lblStreak?.text = "Streak"

                statsCard?.alpha = 1f
            }
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
     * ✅ Safe: usa reflexão pra não depender do nome do método no ScoreManager.
     * Se não achar, retorna 0.
     */
    private fun getStreakSafe(): Int {
        runCatching {
            val cls = Class.forName("com.desafiolgico.utils.ScoreManager")
            val ctor = cls.getConstructor(Context::class.java)
            val inst = ctor.newInstance(this)
            val m = cls.methods.firstOrNull {
                it.parameterCount == 0 &&
                    it.returnType == Int::class.javaPrimitiveType &&
                    it.name.lowercase() in listOf("getstreak", "getcurrentstreak", "currentstreak", "streak")
            }
            val v = (m?.invoke(inst) as? Int) ?: 0
            return v.coerceAtLeast(0)
        }
        return 0
    }

    private fun isPremiumUser(): Boolean {
        // ✅ Ajuste aqui para o seu “flag” real de premium.
        // Ex.: return PremiumManager.isPremium(this)
        return getSharedPreferences("AppPrefs", MODE_PRIVATE).getBoolean("is_premium", false)
    }

    private fun fmtNumber(v: Int): String {
        val nf = NumberFormat.getInstance(Locale("pt", "BR"))
        return nf.format(v)
    }

    // =============================================================================================
    // MENU SETTINGS (mantive)
    // =============================================================================================

    private fun showSettingsMenu(anchor: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_boas_vindas, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_continue -> {
                    openContinue()
                    true
                }
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.action_tutorial -> {
                    startActivity(Intent(this, OnboardingActivity::class.java).putExtra("FROM_SETTINGS", true))
                    true
                }
                R.id.action_switch_account -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.action_delete_account -> {
                    confirmDeleteAccount()
                    true
                }
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

    // =============================================================================================
    // RESET (mantive)
    // =============================================================================================

    private fun resetGameProgress() {
        GameDataManager.clearLastQuestionIndex(this, GameDataManager.Levels.INICIANTE)
        GameDataManager.clearLastQuestionIndex(this, GameDataManager.Levels.INTERMEDIARIO)
        GameDataManager.clearLastQuestionIndex(this, GameDataManager.Levels.AVANCADO)

        GameDataManager.clearLastQuestionIndex(this, GameDataManager.SecretLevels.RELAMPAGO)
        GameDataManager.clearLastQuestionIndex(this, GameDataManager.SecretLevels.PERFEICAO)
        GameDataManager.clearLastQuestionIndex(this, GameDataManager.SecretLevels.ENIGMA)

        GameDataManager.clearUltimoNivelNormal(this)
    }

    // =============================================================================================
    // UI (mantive)
    // =============================================================================================

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
