package com.desafiolgico.utils

import android.content.Context
import android.util.TypedValue
import android.view.View

fun Context.dp(value: Int): Int =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        resources.displayMetrics
    ).toInt()

fun View.dp(value: Int): Int = context.dp(value)
