package com.desafiolgico.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import java.util.concurrent.atomic.AtomicBoolean

object AdMobInitializer {
    private const val TAG = "AdMobInitializer"
    private val started = AtomicBoolean(false)

    fun ensureInitialized(context: Context) {
        if (!started.compareAndSet(false, true)) return
        MobileAds.initialize(context.applicationContext) {
            if (com.desafiolgico.BuildConfig.DEBUG) {
                Log.i(TAG, "✅ AdMob inicializado (async).")
            }
        }
    }
}
