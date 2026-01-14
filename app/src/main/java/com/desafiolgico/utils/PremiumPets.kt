package com.desafiolgico.utils

import android.content.Context
import android.widget.ImageView
import com.desafiolgico.R

object PremiumPets {
    fun applyPet(context: Context, petView: ImageView) {
        val petId = GameDataManager.getSelectedPet(context)
        if (petId == "pet_none") {
            petView.setImageDrawable(null)
            petView.alpha = 0f
            return
        }

        val lvl = GameDataManager.getPetLevel(context, petId)
        val res = when (petId) {
            "pet_owl" -> when (lvl) { 1 -> R.drawable.pet_owl_1; 2 -> R.drawable.pet_owl_2; else -> R.drawable.pet_owl_3 }
            "pet_bot" -> when (lvl) { 1 -> R.drawable.pet_bot_1; 2 -> R.drawable.pet_bot_2; else -> R.drawable.pet_bot_3 }
            "pet_dragon" -> when (lvl) { 1 -> R.drawable.pet_dragon_1; 2 -> R.drawable.pet_dragon_2; else -> R.drawable.pet_dragon_3 }
            else -> 0
        }

        if (res != 0) {
            petView.setImageResource(res)
            petView.alpha = 1f
        }
    }
}
