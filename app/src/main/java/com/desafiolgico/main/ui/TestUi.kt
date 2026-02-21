package com.desafiolgico.main.ui

import com.desafiolgico.databinding.ActivityTestBinding
import com.google.android.material.button.MaterialButton

interface TestUi {
    fun binding(): ActivityTestBinding
    fun optionButtons(): List<MaterialButton>

    fun setAnswerLocked(locked: Boolean)
    fun isAnswerLocked(): Boolean

    fun setOptionsEnabled(enabled: Boolean)

    fun showToast(msg: String)
}

