package com.desafiolgico.utils

import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController

/**
 * 🧭 Utilitário para exibição moderna "Edge-to-Edge" (Android 15+)
 * Garante compatibilidade total com versões anteriores.
 */
fun AppCompatActivity.applyEdgeToEdge() {
    try {
        // Nova API do AndroidX Activity 1.9.0+ (segura e recomendada)
        enableEdgeToEdge()
    } catch (e: NoSuchMethodError) {
        // ✅ Fallback para versões antigas
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
    }

    // 🔧 Ajusta comportamento de ícones na status bar (modo claro/escuro)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val controller = window.insetsController
        controller?.setSystemBarsAppearance(
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        )
    } else {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }
}
