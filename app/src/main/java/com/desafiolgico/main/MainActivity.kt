package com.desafiolgico.main

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.desafiolgico.BuildConfig
import com.desafiolgico.R
import com.desafiolgico.auth.LoginActivity
import com.desafiolgico.databinding.ActivityMainBinding
import com.desafiolgico.settings.SettingsActivity
import com.desafiolgico.utils.AuthGate
import com.desafiolgico.utils.EnigmaPortalGate
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.InAppUpdateHelper
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.LocalRecordsManager
import com.desafiolgico.utils.PremiumThemes
import com.desafiolgico.utils.RewardTestRepo
import com.desafiolgico.utils.applyEdgeToEdge
import com.desafiolgico.utils.applySystemBarsPadding
import com.desafiolgico.weekly.WeeklyChampionshipActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var levelManager: LevelManager

    // ✅ In-App Updates (Opção A = FLEXIBLE + aviso + botão Reiniciar)
    private lateinit var inAppUpdate: InAppUpdateHelper
    private var updateSnack: Snackbar? = null

    private lateinit var startForResult: ActivityResultLauncher<Intent>
    private lateinit var requestNotifLauncher: ActivityResultLauncher<String>

    private var bgMusic: MediaPlayer? = null
    private var hideDailyLabelRunnable: Runnable? = null
    private var menuShown = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyEdgeToEdge(lightSystemBarIcons = false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding(applyTop = true, applyBottom = true)

        GameDataManager.init(this)

        setupActivityResultLauncher()
        setupNotificationLauncher()

        levelManager = LevelManager(this)
        applyPremiumTheme()

        // ✅ In-App Update helper
        inAppUpdate = InAppUpdateHelper(this)

        // ✅ checa e inicia update FLEXIBLE (baixa em background)
        // Quando baixar: avisa e dá botão "Reiniciar"
        inAppUpdate.checkAndStartFlexible(
            onDownloaded = {
                showUpdateReadySnack()
            }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionIfNeeded()
        }

        // começa escondido durante intro
        setMenuVisible(false)

        binding.btnRecordsMain.setOnClickListener {
            playClickSound()
            animateTap(binding.btnRecordsMain)
            startActivity(Intent(this, LocalRecordsActivity::class.java))
        }

        binding.btnSettingsMain.setOnClickListener {
            playClickSound()
            animateTap(binding.btnSettingsMain)
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val dailyClick = View.OnClickListener {
            playClickSound()
            animateTap(binding.dailyChallengeCard)

            if (GameDataManager.isDailyDone(this)) {
                showDailyTempMessage(getString(R.string.daily_come_back_tomorrow))
                return@OnClickListener
            }
            startActivity(Intent(this, DailyChallengeActivity::class.java))
        }

        binding.btnTestePremio.visibility =
            if (BuildConfig.DEBUG && AuthGate.isAdmin()) View.VISIBLE else View.GONE

        if (BuildConfig.DEBUG && AuthGate.isAdmin()) {
            binding.btnTestePremio.setOnClickListener {
                RewardTestRepo.grantTestCoins(10) { ok ->
                    if (ok) {
                        RewardTestRepo.readCoins { coins ->
                            binding.txtCoins.text = "Moedas: $coins"
                        }
                    }
                }
            }
        }

        binding.dailyChallengeCard.setOnClickListener(dailyClick)
        binding.btnDailyChallenge.setOnClickListener(dailyClick)

        binding.mapButton.setOnClickListener {
            playClickSound()
            animateTap(binding.mapButton)
            startActivity(Intent(this, MapActivity::class.java))
        }

        // ✅ setup dos botões de nível + sair (logout real)
        levelManager.setupButtons(
            binding.beginnerButton,
            binding.intermediateButton,
            binding.advancedButton,
            binding.expertButton,
            binding.exitButton,
            ::handleLevelClick,
            ::onLevelLocked,
            ::showExitConfirmationDialog
        )

        // estado inicial
        levelManager.checkAndSaveLevelUnlocks(showToast = true)
        levelManager.updateButtonStates(
            binding.intermediateButton,
            binding.advancedButton,
            binding.expertButton
        )

        updateDailyLabelState()
        updateDailyUI()
        updateRecordsButtonText()

        setupWeeklyButton()

        setupIntro()
        initBackgroundMusic()
    }

    override fun onStart() {
        super.onStart()
        RewardTestRepo.readCoins { coins ->
            binding.txtCoins.text = "Moedas: $coins"
        }
    }

    override fun onResume() {
        super.onResume()

        // ✅ se o update estava em andamento, tenta retomar
        inAppUpdate.resumeIfInProgress()

        applyPremiumTheme()

        levelManager.checkAndSaveLevelUnlocks(showToast = true)
        levelManager.updateButtonStates(
            binding.intermediateButton,
            binding.advancedButton,
            binding.expertButton
        )

        notifyNewUnlocksIfAny()

        updateDailyLabelState()
        updateDailyUI()
        updateRecordsButtonText()

        bgMusic?.let { if (!it.isPlaying) it.start() }

        val score = GameDataManager.getOverallTotalScore(this).coerceAtLeast(0)
        if (EnigmaPortalGate.consumeUnlockNotificationIfNeeded(this, score)) {
            Toast.makeText(this, "🌀 Portal desbloqueado! Vá no Mapa para entrar.", Toast.LENGTH_LONG).show()
        }

        setupWeeklyButton()
    }

    override fun onPause() {
        super.onPause()
        bgMusic?.let { if (it.isPlaying) it.pause() }
        hideDailyLabelRunnable?.let { binding.txtDailyLabel.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideDailyLabelRunnable?.let { binding.txtDailyLabel.removeCallbacks(it) }
        hideDailyLabelRunnable = null
        releaseBgMusic()

        // ✅ evita leak
        updateSnack?.dismiss()
        updateSnack = null
        inAppUpdate.unregister()
    }

    // =============================================================================================
    // In-App Update UI (Opção A)
    // =============================================================================================

    private fun showUpdateReadySnack() {
        // evita duplicar snackbar
        updateSnack?.dismiss()

        updateSnack = Snackbar.make(binding.root, "Atualização pronta ✅", Snackbar.LENGTH_INDEFINITE)
            .setAction("Reiniciar") {
                // ✅ aplica e reinicia o app
                inAppUpdate.completeFlexibleUpdate()
            }
            .also { it.show() }
    }

    // =============================================================================================
    // Theme
    // =============================================================================================

    private fun applyPremiumTheme() {
        runCatching {
            PremiumThemes.apply(
                this,
                root = findViewById(android.R.id.content),
                cardViews = listOf(binding.menuCard, binding.dailyChallengeCard)
            )
        }
    }

    // =============================================================================================
    // WEEKLY
    // =============================================================================================

    private fun setupWeeklyButton() {
        val score = GameDataManager.getOverallTotalScore(this).coerceAtLeast(0)

        val unlockScore = 500
        val unlocked = score >= unlockScore

        binding.btnWeekly.visibility = View.VISIBLE
        binding.btnWeekly.isEnabled = unlocked
        binding.btnWeekly.alpha = if (unlocked) 1f else 0.55f

        if (unlocked) {
            binding.btnWeekly.setOnClickListener {
                playClickSound()
                animateTap(it)
                startActivity(Intent(this, WeeklyChampionshipActivity::class.java))
            }
        } else {
            binding.btnWeekly.setOnClickListener {
                playClickSound()
                animateTap(it)
                Toast.makeText(
                    this,
                    "🔒 Libera com $unlockScore pontos (atual: $score)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // =============================================================================================
    // Launchers / Permissions
    // =============================================================================================

    private fun setupActivityResultLauncher() {
        startForResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                // onResume já atualiza
            }
    }

    private fun setupNotificationLauncher() {
        requestNotifLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
                // opcional
            }
    }

    private fun requestNotificationPermissionIfNeeded() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) requestNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // =============================================================================================
    // Unlock notify (1x por mudança)
    // =============================================================================================

    private fun unlockNotifPrefs() = getSharedPreferences("unlock_notifs", MODE_PRIVATE)

    private fun notifyNewUnlocksIfAny() {
        val p = unlockNotifPrefs()
        val uid = GameDataManager.currentUserId.takeIf { it.isNotBlank() } ?: "anon"
        val snapshotKey = "last_unlocked_levels_snapshot_$uid"

        val now = GameDataManager.getUnlockedLevels(this).toSet()
        val last = p.getStringSet(snapshotKey, null)?.toSet()

        if (last == null) {
            p.edit().putStringSet(snapshotKey, HashSet(now)).apply()
            return
        }

        val newLevels = now - last
        if (newLevels.isEmpty()) return

        p.edit().putStringSet(snapshotKey, HashSet(now)).apply()

        newLevels.forEach { lvl ->
            when (lvl) {
                GameDataManager.Levels.INTERMEDIARIO -> {
                    Toast.makeText(this, "🔥 NOVO NÍVEL DESBLOQUEADO: INTERMEDIÁRIO!", Toast.LENGTH_LONG).show()
                    bounceUnlock(binding.intermediateButton)
                    glowUnlock(binding.intermediateButton)
                }
                GameDataManager.Levels.AVANCADO -> {
                    Toast.makeText(this, "⚡ NOVO NÍVEL DESBLOQUEADO: AVANÇADO!", Toast.LENGTH_LONG).show()
                    bounceUnlock(binding.advancedButton)
                    glowUnlock(binding.advancedButton)
                }
                GameDataManager.Levels.EXPERIENTE -> {
                    Toast.makeText(this, "👑 NOVO NÍVEL DESBLOQUEADO: EXPERIENTE!", Toast.LENGTH_LONG).show()
                    bounceUnlock(binding.expertButton)
                    glowUnlock(binding.expertButton)
                }
                else -> Unit
            }
        }
    }

    private fun bounceUnlock(v: View) {
        v.animate().cancel()
        v.scaleX = 1f
        v.scaleY = 1f
        v.animate()
            .scaleX(1.08f).scaleY(1.08f)
            .setDuration(160)
            .withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(220).start() }
            .start()
    }

    private fun glowUnlock(v: View) {
        v.alpha = 1f
        v.animate().cancel()
        v.animate()
            .alpha(0.75f)
            .setDuration(120)
            .withEndAction { v.animate().alpha(1f).setDuration(180).start() }
            .start()
    }

    // =============================================================================================
    // Click handlers
    // =============================================================================================

    private fun handleLevelClick(btn: MaterialButton, level: String) {
        playClickSound()
        animateTap(btn)

        val target = if (level == GameDataManager.Levels.EXPERIENTE) {
            ExpertChallengeActivity::class.java
        } else {
            TestActivity::class.java
        }

        val i = Intent(this, target).apply {
            putExtra(TestNav.EXTRA_MODE, "NORMAL")
            putExtra(TestNav.EXTRA_LEVEL, level)
        }

        startForResult.launch(i)
    }

    private fun onLevelLocked(level: String) {
        Log.d("MainActivity", "Level locked: $level")
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Sair da conta")
            .setMessage("Você quer sair da sua conta agora?")
            .setPositiveButton("Sair") { _, _ ->
                playClickSound()
                doLogoutAndGoToLogin()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun doLogoutAndGoToLogin() {
        runCatching { FirebaseAuth.getInstance().signOut() }

        runCatching {
            getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
                .putBoolean("is_guest_mode", false)
                .apply()
        }

        runCatching { GameDataManager.setActiveUserId(this, null) }

        val i = Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(LoginActivity.EXTRA_FROM_LOGOUT, true)
        }
        startActivity(i)
        finish()
    }

    // =============================================================================================
    // UI updates (Recordes / Daily)
    // =============================================================================================

    private fun updateRecordsButtonText() {
        val bestToday = LocalRecordsManager.getBestStreakOfDay(this)
        binding.btnRecordsMain.text =
            if (bestToday > 0) "🏆 Recordes (🔥$bestToday)" else "🏆 Recordes"
    }

    private fun updateDailyLabelState() {
        binding.txtDailyLabel.text = if (GameDataManager.isDailyDone(this)) {
            getString(R.string.daily_done_label)
        } else {
            getString(R.string.daily_available_label)
        }
    }

    private fun updateDailyUI() {
        val done = GameDataManager.isDailyDone(this)
        binding.dailyChallengeCard.alpha = if (done) 0.7f else 1f
    }

    private fun showDailyTempMessage(msg: String) {
        binding.txtDailyLabel.text = msg
        binding.txtDailyLabel.visibility = View.VISIBLE

        hideDailyLabelRunnable?.let { binding.txtDailyLabel.removeCallbacks(it) }
        hideDailyLabelRunnable = Runnable { updateDailyLabelState() }
            .also { binding.txtDailyLabel.postDelayed(it, 1700L) }
    }

    // =============================================================================================
    // Intro + música
    // =============================================================================================

    private fun setupIntro() {
        binding.lottieAnimationView.visibility = View.VISIBLE
        binding.lottieAnimationView.playAnimation()

        binding.lottieAnimationView.removeAllAnimatorListeners()
        binding.lottieAnimationView.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) = showMenuOnce()
            override fun onAnimationCancel(animation: Animator) = showMenuOnce()
        })
    }

    private fun showMenuOnce() {
        if (menuShown) return
        menuShown = true

        binding.lottieAnimationView.visibility = View.GONE
        setMenuVisible(true)

        levelManager.updateButtonStates(binding.intermediateButton, binding.advancedButton, binding.expertButton)
        updateDailyLabelState()
        updateDailyUI()
        updateRecordsButtonText()
    }

    private fun setMenuVisible(visible: Boolean) {
        binding.menuCard.visibility = if (visible) View.VISIBLE else View.GONE
        binding.dailyChallengeCard.visibility = if (visible) View.VISIBLE else View.GONE
        binding.btnRecordsMain.visibility = if (visible) View.VISIBLE else View.GONE
        binding.btnSettingsMain.visibility = if (visible) View.VISIBLE else View.GONE
        binding.btnWeekly.visibility = if (visible) View.VISIBLE else View.GONE
        binding.mapButton.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun initBackgroundMusic() {
        releaseBgMusic()
        runCatching {
            bgMusic = MediaPlayer.create(this, R.raw.background_music)?.apply {
                isLooping = true
                setVolume(0.5f, 0.5f)
                start()
            }
        }.onFailure {
            Log.w("MainActivity", "Falha ao iniciar música de fundo", it)
            bgMusic = null
        }
    }

    private fun releaseBgMusic() {
        val mp = bgMusic ?: return
        bgMusic = null
        runCatching { if (mp.isPlaying) mp.stop() }
        runCatching { mp.release() }
    }

    // =============================================================================================
    // FX helpers
    // =============================================================================================

    private fun animateTap(v: View) {
        v.animate().cancel()
        v.scaleX = 1f
        v.scaleY = 1f
        v.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(70)
            .withEndAction {
                v.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .setInterpolator(OvershootInterpolator(0.8f))
                    .start()
            }
            .start()
    }

    private fun startGlow(v: View) {
        v.animate().cancel()
        val a1 = ObjectAnimator.ofFloat(v, View.ALPHA, 1f, 0.7f).apply { duration = 450 }
        val a2 = ObjectAnimator.ofFloat(v, View.ALPHA, 0.7f, 1f).apply { duration = 450 }
        AnimatorSet().apply {
            interpolator = AccelerateDecelerateInterpolator()
            playSequentially(a1, a2)
            start()
        }
    }

    private fun playClickSound() {
        runCatching {
            val mp = MediaPlayer.create(this, R.raw.click_sound) ?: return
            mp.setOnCompletionListener { it.release() }
            mp.start()
        }.onFailure {
            Log.w("MainActivity", "Falha ao tocar som de clique", it)
        }
    }
}
