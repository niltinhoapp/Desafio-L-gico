// utils/EdgeToEdge.kt
package com.desafiolgico.utils

import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.applyEdgeToEdge(lightSystemBarIcons: Boolean) {
    // Para fundo escuro: ícones claros => lightSystemBarIcons = false
    // Para fundo claro: ícones escuros => lightSystemBarIcons = true

    val style = if (lightSystemBarIcons) {
        SystemBarStyle.light(
            scrim = android.graphics.Color.TRANSPARENT,
            darkScrim = android.graphics.Color.TRANSPARENT
        )
    } else {
        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
    }

    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
}
