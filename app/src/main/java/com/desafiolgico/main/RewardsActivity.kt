package com.desafiolgico.main

import android.annotation.SuppressLint
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityRewardsBinding
import com.desafiolgico.utils.CoinManager
import com.desafiolgico.utils.UserManager
import com.desafiolgico.utils.applyEdgeToEdge
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.initialization.AdapterStatus
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.desafiolgico.BuildConfig

import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.models.Size
import java.util.concurrent.TimeUnit

class RewardsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRewardsBinding
    private var rewardedAd: RewardedAd? = null
    private var coins = 0

    companion object {
        private const val GAME_REWARD_AMOUNT = 50


        // Test oficial do Google (usar somente em DEBUG)
        private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

        // PROD (seu ID real)
        private const val PROD_REWARDED_ID = "ca-app-pub-4958622518589705/3051012274"
    }

    /** Escolhe o ID correto conforme build type */
    private fun rewardedUnitId(): String {
        return if (BuildConfig.DEBUG) TEST_REWARDED_ID else PROD_REWARDED_ID
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()
        binding = ActivityRewardsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // (Opcional) Configure test device SOMENTE em debug
        if (BuildConfig.DEBUG) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .build()
            )
        }

        MobileAds.initialize(this) { status ->
            status.adapterStatusMap.forEach { (_, s: AdapterStatus) ->
                if (s.initializationState != AdapterStatus.State.READY) {
                    Toast.makeText(this, "Erro ao iniciar AdMob", Toast.LENGTH_SHORT).show()
                }
            }
            loadRewardedAd()
        }

        // Saldo inicial
        coins = CoinManager.getCoins(this)
        atualizarSaldo()

        binding.earnCoinsButton.apply {
            isEnabled = false
            text = "Carregando anúncio..."
            setOnClickListener {
                isEnabled = false
                showRewardedAd()
            }
        }

        binding.backButton.setOnClickListener { finish() }
    }

    /** Carrega o anúncio premiado */
    private fun loadRewardedAd() {
        binding.earnCoinsButton.isEnabled = false
        binding.earnCoinsButton.text = "Carregando anúncio..."

        RewardedAd.load(
            this,
            rewardedUnitId(),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    setupAdCallbacks()
                    binding.earnCoinsButton.isEnabled = true
                    binding.earnCoinsButton.text = "🎁 Assistir e ganhar $GAME_REWARD_AMOUNT moedas"
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    binding.earnCoinsButton.isEnabled = true
                    binding.earnCoinsButton.text = "Tentar novamente (anúncio indisponível)"
                }
            }
        )
    }

    /** Callbacks de exibição/fechamento */
    private fun setupAdCallbacks() {
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                Toast.makeText(this@RewardsActivity, "Falha ao exibir anúncio.", Toast.LENGTH_SHORT).show()
                loadRewardedAd()
            }

            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                // Recarrega automaticamente após fechar
                loadRewardedAd()
            }
        }
    }

    /** Tenta exibir o anúncio */
    private fun showRewardedAd() {
        val ad = rewardedAd
        if (ad == null) {
            Toast.makeText(this, "Anúncio não pronto. Tentando recarregar…", Toast.LENGTH_SHORT).show()
            loadRewardedAd()
            return
        }

        binding.earnCoinsButton.isEnabled = false

        ad.show(this) { rewardItem ->
            // se vier 0, ainda assim o callback só dispara quando o usuário ganhou recompensa,
            // mas você pode proteger assim:
            val earned = rewardItem.amount.takeIf { it > 0 } ?: 1

            // aqui você decide fixo 50 (ok!)
            CoinManager.addCoins(this, GAME_REWARD_AMOUNT, reason = "AdReward")

            atualizarSaldo()
            animateCoinUpdate()
            showCelebrationEffect()
            Toast.makeText(this, "💰 Você ganhou $GAME_REWARD_AMOUNT moedas!", Toast.LENGTH_LONG).show()
        }
    }

    /** Efeito visual de celebração */
    private fun showCelebrationEffect() {
        try {
            val konfettiView = binding.konfettiView
            konfettiView.visibility = View.VISIBLE
            konfettiView.setBackgroundColor(Color.TRANSPARENT)

            // vibra
            val vibrator = getSystemService<Vibrator>()
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION") it.vibrate(300)
                }
            }

            // som
            val player = MediaPlayer.create(this, R.raw.bonus_sound)
            player.start()
            player.setOnCompletionListener { mp -> mp.release() }

            // konfetti
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

            konfettiView.postDelayed({
                konfettiView.visibility = View.GONE
            }, 4000L)
        } catch (_: Exception) {
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
        if (rewardedAd == null) loadRewardedAd()
        atualizarSaldo()
    }
}
