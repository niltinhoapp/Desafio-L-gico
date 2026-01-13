package com.desafiolgico

import android.app.Activity
import android.app.Application
import android.os.Bundle
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
