package com.desafiolgico

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.desafiolgico.utils.AdMobInitializer
import com.desafiolgico.utils.CoinManager
import com.desafiolgico.utils.CrashlyticsHelper
import java.util.concurrent.atomic.AtomicBoolean

class App : Application() {

    private val didInit = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {

            override fun onActivityResumed(activity: Activity) {
                // roda só 1x por processo
                if (!didInit.compareAndSet(false, true)) return

                // ✅ depois que a UI já está na tela (fora do cold start crítico)
                activity.window.decorView.post {
                    // 1) Ads (idempotente via AtomicBoolean no initializer)
                 //   AdMobInitializer.ensureInitialized(applicationContext)

                    // 2) Multiplicador (se você decidiu persistir)
                    CoinManager.loadMultiplier(applicationContext)

                    // 3) Crashlytics (em thread separada, como você já fez)
                    Thread {
                        CrashlyticsHelper.initFast(applicationContext, enableInDebug = false)
                    }.start()
                }

                // não precisa mais ouvir callbacks
                unregisterActivityLifecycleCallbacks(this)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
