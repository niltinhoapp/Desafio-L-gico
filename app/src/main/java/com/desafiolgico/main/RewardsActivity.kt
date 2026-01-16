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
    private var rewardedAd: RewardedAd? = null

    // Estado premium
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isBusy = false
    private var loadAttempts = 0
    private var loadingTimeoutRunnable: Runnable? = null

    companion object {
        private const val TAG = "RewardsActivity"
        private const val GAME_REWARD_AMOUNT = 50

        private const val COOLDOWN_SECONDS = 3
        private const val LOAD_TIMEOUT_MS = 12_000L

        // Test oficial do Google (usar somente em DEBUG)
        private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

        // PROD (seu ID real)
        private const val PROD_REWARDED_ID = "ca-app-pub-4958622518589705/3051012274"
    }

    private fun rewardedUnitId(): String {
        return if (BuildConfig.DEBUG) TEST_REWARDED_ID else PROD_REWARDED_ID
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()
        binding = ActivityRewardsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AdMobInitializer.ensureInitialized(applicationContext)

        atualizarSaldo()

        binding.earnCoinsButton.setOnClickListener {
            if (isBusy) return@setOnClickListener
            showRewardedAd()
        }

        binding.backButton.setOnClickListener { finish() }

        setButtonStateLoading("Carregando anúncio...")
        loadRewardedAd()
    }

    // -----------------------------
    // LOAD / RETRY (premium)
    // -----------------------------
    private fun loadRewardedAd() {
        if (isFinishing) return

        isBusy = true
        setButtonStateLoading("Carregando anúncio...")

        // Timeout de segurança (evita “travado”)
        loadingTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        loadingTimeoutRunnable = Runnable {
            if (rewardedAd == null) {
                Log.w(TAG, "Timeout carregando anúncio. Tentando recarregar...")
                isBusy = false
                setButtonStateRetry("Tentar novamente")
            }
        }
        mainHandler.postDelayed(loadingTimeoutRunnable!!, LOAD_TIMEOUT_MS)

        RewardedAd.load(
            this,
            rewardedUnitId(),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    loadAttempts = 0
                    isBusy = false
                    loadingTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }

                    setupAdCallbacks()
                    setButtonStateReady("🎁 Assistir e ganhar $GAME_REWARD_AMOUNT moedas")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    loadingTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }

                    Log.w(TAG, "Falha no load: ${error.message}")
                    isBusy = false
                    scheduleRetryLoad()
                }
            }
        )
    }

    private fun scheduleRetryLoad() {
        loadAttempts++

        // backoff: 1.5s, 3s, 6s (cap)
        val delay = when (loadAttempts) {
            1 -> 1500L
            2 -> 3000L
            else -> 6000L
        }

        setButtonStateLoading("Tentando novamente em ${delay / 1000}s...")

        mainHandler.postDelayed({
            if (!isFinishing) loadRewardedAd()
        }, delay)
    }

    // -----------------------------
    // SHOW / CALLBACKS
    // -----------------------------
    private fun setupAdCallbacks() {
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                Toast.makeText(this@RewardsActivity, "Falha ao exibir anúncio.", Toast.LENGTH_SHORT).show()
                isBusy = false
                startCooldownThenLoad()
            }

            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                isBusy = false
                startCooldownThenLoad()
            }
        }
    }

    private fun showRewardedAd() {
        val ad = rewardedAd
        if (ad == null) {
            Toast.makeText(this, "Anúncio não pronto. Recarregando…", Toast.LENGTH_SHORT).show()
            loadRewardedAd()
            return
        }

        isBusy = true
        setButtonStateLoading("Abrindo anúncio...")

        ad.show(this) {
            // ✅ recompensa fixa e consistente
            CoinManager.addCoins(this, GAME_REWARD_AMOUNT, reason = "AdReward")
            atualizarSaldo()
            animateCoinUpdate()
            showCelebrationEffect()

            Toast.makeText(this, "💰 Você ganhou $GAME_REWARD_AMOUNT moedas!", Toast.LENGTH_LONG).show()
        }
    }

    // -----------------------------
    // COOLDOWN (3s) + reload
    // -----------------------------
    private fun startCooldownThenLoad() {
        var seconds = COOLDOWN_SECONDS
        setButtonStateLoading("Aguarde $seconds s...")

        val tick = object : Runnable {
            override fun run() {
                seconds--
                if (seconds <= 0) {
                    setButtonStateLoading("Carregando anúncio...")
                    loadRewardedAd()
                } else {
                    setButtonStateLoading("Aguarde $seconds s...")
                    mainHandler.postDelayed(this, 1000L)
                }
            }
        }
        mainHandler.postDelayed(tick, 1000L)
    }

    // -----------------------------
    // UI states
    // -----------------------------
    private fun setButtonStateLoading(text: String) {
        binding.earnCoinsButton.isEnabled = false
        binding.earnCoinsButton.text = text
        binding.earnCoinsButton.alpha = 0.85f
    }

    private fun setButtonStateReady(text: String) {
        binding.earnCoinsButton.isEnabled = true
        binding.earnCoinsButton.text = text
        binding.earnCoinsButton.alpha = 1f
    }

    private fun setButtonStateRetry(text: String) {
        binding.earnCoinsButton.isEnabled = true
        binding.earnCoinsButton.text = text
        binding.earnCoinsButton.alpha = 1f
        // ao clicar, tenta carregar na hora
        binding.earnCoinsButton.setOnClickListener {
            if (isBusy) return@setOnClickListener
            loadRewardedAd()
        }
    }

    // -----------------------------
    // FUN (premium)
    // -----------------------------
    private fun showCelebrationEffect() {
        try {
            val konfettiView = binding.konfettiView
            konfettiView.visibility = View.VISIBLE
            konfettiView.setBackgroundColor(Color.TRANSPARENT)

            val vibrator = getSystemService<Vibrator>()
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION") it.vibrate(300)
                }
            }

            val player = MediaPlayer.create(this, R.raw.bonus_sound)
            player.start()
            player.setOnCompletionListener { mp -> mp.release() }

            konfettiView.post {
                val party = Party(
                    emitter = Emitter(2000L, TimeUnit.MILLISECONDS).max(250),
                    speed = 10f,
                    maxSpeed = 25f,
                    damping = 0.9f,
                    spread = 360,
                    timeToLive = 3500L,
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
            }

            konfettiView.postDelayed({ konfettiView.visibility = View.GONE }, 4000L)
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao exibir celebração", e)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun atualizarSaldo() {
        val coinsAtual = CoinManager.getCoins(this)
        binding.coinsTextView.text = "💰 Moedas: $coinsAtual"
    }

    private fun animateCoinUpdate() {
        binding.coinsTextView.animate()
            .scaleX(1.2f).scaleY(1.2f)
            .setDuration(150)
            .withEndAction {
                binding.coinsTextView.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }.start()
    }

    override fun onResume() {
        super.onResume()
        atualizarSaldo()
        if (rewardedAd == null && !isBusy) loadRewardedAd()
    }

    override fun onDestroy() {
        loadingTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        super.onDestroy()
    }
}
