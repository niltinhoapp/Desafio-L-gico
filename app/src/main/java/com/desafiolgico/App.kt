package com.desafiolgico

import android.app.Application
import com.desafiolgico.utils.CrashlyticsHelper

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashlyticsHelper.setupCrashlytics(this, enableInDebug = false)


    }
}
