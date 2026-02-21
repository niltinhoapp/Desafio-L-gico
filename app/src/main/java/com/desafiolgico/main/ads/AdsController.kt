package com.desafiolgico.main.ads

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityTestBinding
import com.desafiolgico.utils.CoinManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdsController(
    private val activity: AppCompatActivity,
    private val binding: ActivityTestBinding,
    private val dp: (Int) -> Int,
    private val onCoinsChanged: () -> Unit,
    private val adContainer: FrameLayout,
    private val bannerUnitId: String
) {

    companion object {
        const val REWARD_AD_COINS = 5
        private const val TAG = "ADS"
    }

    private var adView: AdView? = null
    private var rewardedAd: RewardedAd? = null
    private var bannerLoaded = false

    private fun rewardedUnitId(): String =
        activity.getString(R.string.admob_rewarded_ad_unit_id)

    fun onResume() {
        adView?.resume()
    }

    fun onPause() {
        adView?.pause()
    }

    fun onDestroy() {
        adView?.destroy()
        adView = null
        rewardedAd = null
    }

    /** CHAME UMA VEZ no onCreate (ou no coordinator.start()) */
    fun initAds() {
        setupBannerAdaptive()
        loadRewardedAd()
    }

    // -------------------------
    // Banner adaptive (com fallback)
    // -------------------------

    /**
     * Tenta usar getLargeAnchoredAdaptiveBannerAdSize (API mais nova).
     * Se não existir no seu SDK, retorna null e cai no fallback.
     */
    private fun getLargeAnchoredAdSizeOrNull(adWidthDp: Int): AdSize? {
        return try {
            val method = AdSize::class.java.getMethod(
                "getLargeAnchoredAdaptiveBannerAdSize",
                Context::class.java,
                Int::class.javaPrimitiveType
            )
            method.invoke(null, activity, adWidthDp) as AdSize
        } catch (_: Throwable) {
            null
        }
    }

    private fun setupBannerAdaptive() {
        if (bannerLoaded) return
        bannerLoaded = true

        adContainer.removeAllViews()

        adContainer.post {
            if (activity.isFinishing || activity.isDestroyed) return@post

            val widthPx = adContainer.width.takeIf { it > 0 }
                ?: activity.resources.displayMetrics.widthPixels
            val adWidthDp = (widthPx / activity.resources.displayMetrics.density).toInt()

            val banner = AdView(activity).apply {
                adUnitId = bannerUnitId

                val adSize = getLargeAnchoredAdSizeOrNull(adWidthDp)
                    ?: AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidthDp)

                setAdSize(adSize)

                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        visibility = View.VISIBLE
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        visibility = View.GONE
                        Log.e(TAG, "Banner failed: ${error.code} - ${error.message}")
                    }
                }

                visibility = View.GONE
            }

            adContainer.addView(
                banner,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )

            banner.loadAd(AdRequest.Builder().build())
            adView = banner
        }
    }

    // -------------------------
    // Rewarded
    // -------------------------

    private fun loadRewardedAd() {
        RewardedAd.load(
            activity,
            rewardedUnitId(),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    Log.e(TAG, "Rewarded failed: ${error.code} - ${error.message}")
                }
            }
        )
    }

    /**
     * Mostra rewarded se existir. Se não existir, só chama onClosed.
     * Use isso antes de navegar para Result.
     */
    fun showRewardedIfAvailable(
        onReward: (coins: Int) -> Unit = {},
        onClosed: () -> Unit
    ) {
        val ad = rewardedAd
        if (ad == null) {
            onClosed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewardedAd()
                onClosed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                loadRewardedAd()
                Log.e(TAG, "Rewarded show failed: ${error.code} - ${error.message}")
                onClosed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded showed")
            }
        }

        ad.show(activity) { rewardItem: RewardItem ->
            val rewardAmount = rewardItem.amount.takeIf { it > 0 } ?: REWARD_AD_COINS

            CoinManager.addCoins(activity, rewardAmount, reason = CoinManager.AD_REWARD)
            onCoinsChanged()
            onReward(rewardAmount)

            Toast.makeText(activity, "💰 +$rewardAmount moedas!", Toast.LENGTH_SHORT).show()
        }
    }
}
