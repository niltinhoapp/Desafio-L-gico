package com.desafiolgico.main

import android.annotation.SuppressLint
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import com.desafiolgico.BuildConfig
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityRewardsBinding
import com.desafiolgico.utils.AdMobInitializer
import com.desafiolgico.utils.CoinManager
import com.desafiolgico.utils.applyEdgeToEdge
import com.desafiolgico.utils.applySystemBarsPadding
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.models.Size
import java.util.concurrent.TimeUnit
import kotlin.math.min

class RewardsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRewardsBinding

    private val mainHandler = Handler(Looper.getMainLooper())

    private var rewardedAd: RewardedAd? = null
    private var state: UiState = UiState.LOADING
    private var loadAttempts = 0

    // ✅ Runnables (limpar no onDestroy)
    private var timeoutRunnable: Runnable? = null
    private var autoRetryRunnable: Runnable? = null
    private var cooldownTickRunnable: Runnable? = null
    private var confettiHideRunnable: Runnable? = null

    private enum class UiState { LOADING, READY, RETRY, SHOWING, COOLDOWN }

    companion object {
        private const val TAG = "RewardsActivity"

        private const val GAME_REWARD_AMOUNT = 50
        private const val COOLDOWN_SECONDS = 3
        private const val LOAD_TIMEOUT_MS = 12_000L

        private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val PROD_REWARDED_ID = "ca-app-pub-4958622518589705/3051012274"
    }

    private fun rewardedUnitId(): String =
        if (BuildConfig.DEBUG) TEST_REWARDED_ID else PROD_REWARDED_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Edge-to-edge ANTES do setContentView
        applyEdgeToEdge(lightSystemBarIcons = false)

        binding = ActivityRewardsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ aplica insets SOMENTE no content (root fica “livre” pro konfetti cobrir tudo)
        binding.contentRewards.applySystemBarsPadding(applyTop = true, applyBottom = true)

        // ✅ Ads init
        AdMobInitializer.ensureInitialized(applicationContext)

        // ✅ UI
        binding.backButton.setOnClickListener { finish() }
        binding.earnCoinsButton.setOnClickListener { onEarnClick() }

        atualizarSaldo()
        renderState(UiState.LOADING, getString(R.string.rewards_loading))

        loadRewardedAd(force = true)
    }

    override fun onResume() {
        super.onResume()
        atualizarSaldo()

        // ✅ se não tem anúncio e não está carregando/mostrando, tenta carregar
        if (rewardedAd == null && state != UiState.LOADING && state != UiState.SHOWING) {
            loadRewardedAd(force = false)
        }
    }

    override fun onDestroy() {
        cancelTimeout()
        cancelAutoRetry()
        cancelCooldown()
        cancelConfettiHide()

        rewardedAd = null
        super.onDestroy()
    }

    private fun onEarnClick() {
        if (isFinishing || isDestroyed) return

        when {
            state == UiState.SHOWING || state == UiState.LOADING -> Unit
            rewardedAd != null -> showRewardedAd()
            else -> {
                Toast.makeText(this, "Anúncio não pronto. Recarregando…", Toast.LENGTH_SHORT).show()
                loadRewardedAd(force = true)
            }
        }
    }

    private fun loadRewardedAd(force: Boolean) {
        if (isFinishing || isDestroyed) return
        if (!force && (state == UiState.LOADING || state == UiState.SHOWING)) return

        cancelTimeout()
        cancelAutoRetry()
        cancelCooldown()

        rewardedAd = null

        state = UiState.LOADING
        renderState(UiState.LOADING, getString(R.string.rewards_loading))

        // ✅ timeout (SAFE: só muda pra RETRY se ainda estiver LOADING)
        timeoutRunnable = Runnable {
            if (state == UiState.LOADING && rewardedAd == null && !(isFinishing || isDestroyed)) {
                Log.w(TAG, "Timeout carregando anúncio. Liberando UI para retry.")
                state = UiState.RETRY
                renderState(UiState.RETRY, getString(R.string.rewards_retry))
            }
        }.also {
            mainHandler.postDelayed(it, LOAD_TIMEOUT_MS)
        }

        RewardedAd.load(
            this,
            rewardedUnitId(),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    if (isFinishing || isDestroyed) return

                    rewardedAd = ad
                    loadAttempts = 0
                    cancelTimeout()

                    setupAdCallbacks(ad)

                    state = UiState.READY
                    renderState(UiState.READY, "🎁 Assistir e ganhar $GAME_REWARD_AMOUNT moedas")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (isFinishing || isDestroyed) return

                    rewardedAd = null
                    cancelTimeout()

                    Log.w(TAG, "Falha no load: ${error.message}")
                    state = UiState.RETRY
                    renderState(UiState.RETRY, getString(R.string.rewards_retry))

                    scheduleAutoRetry()
                }
            }
        )
    }

    private fun scheduleAutoRetry() {
        cancelAutoRetry()

        loadAttempts++
        val delayMs = when (loadAttempts) {
            1 -> 1500L
            2 -> 3000L
            else -> 6000L
        }

        autoRetryRunnable = Runnable {
            if (!(isFinishing || isDestroyed) && rewardedAd == null && state != UiState.SHOWING) {
                loadRewardedAd(force = true)
            }
        }.also {
            mainHandler.postDelayed(it, delayMs)
        }
    }

    private fun setupAdCallbacks(ad: RewardedAd) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                if (isFinishing || isDestroyed) return

                Log.w(TAG, "Falha ao exibir anúncio: ${adError.message}")
                rewardedAd = null
                state = UiState.RETRY
                renderState(UiState.RETRY, getString(R.string.rewards_retry))

                Toast.makeText(this@RewardsActivity, "Falha ao exibir anúncio.", Toast.LENGTH_SHORT)
                    .show()

                startCooldownThenLoad()
            }

            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                if (isFinishing || isDestroyed) return
                startCooldownThenLoad()
            }
        }
    }

    private fun showRewardedAd() {
        val ad = rewardedAd ?: run {
            state = UiState.RETRY
            renderState(UiState.RETRY, getString(R.string.rewards_retry))
            loadRewardedAd(force = true)
            return
        }

        cancelTimeout()
        cancelAutoRetry()
        cancelCooldown()

        state = UiState.SHOWING
        renderState(UiState.SHOWING, getString(R.string.rewards_opening))

        ad.show(this) { rewardItem ->
            val amount = rewardItem.amount.takeIf { it > 0 } ?: GAME_REWARD_AMOUNT

            CoinManager.addCoins(this, amount, reason = "AdReward")
            atualizarSaldo()
            animateCoinUpdate()
            showCelebrationEffect(amount)

            Toast.makeText(this, "💰 Você ganhou $amount moedas!", Toast.LENGTH_LONG).show()
        }
    }

    private fun startCooldownThenLoad() {
        cancelCooldown()

        var seconds = COOLDOWN_SECONDS
        state = UiState.COOLDOWN
        renderState(UiState.COOLDOWN, "Aguarde $seconds s...")

        cooldownTickRunnable = object : Runnable {
            override fun run() {
                if (isFinishing || isDestroyed) return

                seconds--
                if (seconds <= 0) {
                    state = UiState.LOADING
                    renderState(UiState.LOADING, getString(R.string.rewards_loading))
                    loadRewardedAd(force = true)
                } else {
                    renderState(UiState.COOLDOWN, "Aguarde $seconds s...")
                    mainHandler.postDelayed(this, 1000L)
                }
            }
        }.also { mainHandler.postDelayed(it, 1000L) }
    }

    private fun renderState(newState: UiState, text: String) {
        state = newState
        binding.earnCoinsButton.text = text

        val enabled = when (newState) {
            UiState.READY, UiState.RETRY -> true
            else -> false
        }
        binding.earnCoinsButton.isEnabled = enabled
        binding.earnCoinsButton.alpha = if (enabled) 1f else 0.85f
    }

    @SuppressLint("SetTextI18n")
    private fun atualizarSaldo() {
        val coinsAtual = CoinManager.getCoins(this)
        binding.coinsTextView.text = "💰 Moedas: $coinsAtual"
    }

    private fun animateCoinUpdate() {
        binding.coinsTextView.animate().cancel()
        binding.coinsTextView.animate()
            .scaleX(1.18f).scaleY(1.18f)
            .setDuration(140)
            .withEndAction {
                binding.coinsTextView.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(160)
                    .start()
            }
            .start()
    }

    private fun showCelebrationEffect(amount: Int) {
        try {
            val konfettiView = binding.konfettiView
            konfettiView.visibility = View.VISIBLE
            konfettiView.setBackgroundColor(Color.TRANSPARENT)

            // vibração curtinha
            getSystemService<Vibrator>()?.let { vib ->
                if (vib.hasVibrator()) {
                    val ms = 260L
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION") vib.vibrate(ms)
                    }
                }
            }

            // som curtinho (sem manter referência)
            runCatching {
                val mp = MediaPlayer.create(this, R.raw.bonus_sound)
                mp.setOnCompletionListener { it.release() }
                mp.start()
            }

            // confetti
            val perSecond = min(320, 180 + (amount * 2))
            val party = Party(
                emitter = Emitter(1500L, TimeUnit.MILLISECONDS).perSecond(perSecond),
                speed = 10f,
                maxSpeed = 24f,
                damping = 0.90f,
                spread = 360,
                timeToLive = 2800L,
                colors = listOf(
                    Color.YELLOW,
                    Color.rgb(255, 223, 0),
                    Color.rgb(255, 215, 0)
                ),
                shapes = listOf(Shape.Circle, Shape.Square),
                size = listOf(Size(8, 2f)),
                position = Position.Relative(0.5, 0.0)
            )
            konfettiView.start(listOf(party))

            cancelConfettiHide()
            confettiHideRunnable = Runnable { konfettiView.visibility = View.GONE }.also {
                konfettiView.postDelayed(it, 3200L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao exibir celebração", e)
        }
    }

    private fun cancelTimeout() {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    private fun cancelAutoRetry() {
        autoRetryRunnable?.let { mainHandler.removeCallbacks(it) }
        autoRetryRunnable = null
    }

    private fun cancelCooldown() {
        cooldownTickRunnable?.let { mainHandler.removeCallbacks(it) }
        cooldownTickRunnable = null
    }

    private fun cancelConfettiHide() {
        confettiHideRunnable?.let { binding.konfettiView.removeCallbacks(it) }
        confettiHideRunnable = null
    }
}
