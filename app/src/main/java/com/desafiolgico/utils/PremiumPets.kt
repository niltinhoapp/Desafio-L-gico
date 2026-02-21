package com.desafiolgico.utils

import android.content.Context
import android.view.View
import android.widget.ImageView
import com.desafiolgico.R

object PremiumPets {

    fun applyPet(context: Context, petView: ImageView) {
        val petId = GameDataManager.getSelectedPet(context)

        if (petId.isBlank() || petId == "pet_none") {
            clearPet(petView)
            return
        }

        // Se pet não estiver desbloqueado, não mostra (evita bug)
        if (!GameDataManager.isPetUnlocked(context, petId)) {
            clearPet(petView)
            return
        }

        val lvl = GameDataManager.getPetLevel(context, petId).coerceIn(1, 3)

        val resId = when (petId) {
            "pet_owl" -> when (lvl) {
                1 -> R.drawable.pet_owl_1
                2 -> R.drawable.pet_owl_2
                else -> R.drawable.pet_owl_3
            }

            "pet_bot" -> when (lvl) {
                1 -> R.drawable.pet_bot_1
                2 -> R.drawable.pet_bot_2
                else -> R.drawable.pet_bot_3
            }

            "pet_dragon" -> when (lvl) {
                1 -> R.drawable.pet_dragon_1
                2 -> R.drawable.pet_dragon_2
                else -> R.drawable.pet_dragon_3
            }

            else -> 0
        }

        if (resId == 0) {
            clearPet(petView)
            return
        }

        // ✅ aplica e garante estado consistente
        petView.setImageResource(resId)
        petView.visibility = View.VISIBLE
        petView.alpha = 1f
        petView.scaleX = 1f
        petView.scaleY = 1f
        petView.translationX = 0f
        petView.translationY = 0f
        petView.rotation = 0f
    }

    private fun clearPet(petView: ImageView) {
        // ✅ zera TUDO pra não “vazar” estado entre telas
        petView.setImageDrawable(null)
        petView.visibility = View.GONE
        petView.alpha = 0f
        petView.scaleX = 1f
        petView.scaleY = 1f
        petView.translationX = 0f
        petView.translationY = 0f
        petView.rotation = 0f
    }
}
